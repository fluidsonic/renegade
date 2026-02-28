package ccr.server.net

import ccr.server.level.w3d.W3dAnimation
import ccr.server.level.w3d.W3dFileParser
import ccr.server.level.w3d.W3dHierarchy
import ccr.server.core.MixReader

// ============================================================
// HAnimClass
// C++: Code/ww3d2/hanim.h — abstract base, concrete types are HRawAnimClass / HCompressedAnimClass
// Server-side: backed directly by a parsed W3dAnimation; ref-counting omitted (GC handles it).
// ============================================================

// C++: HAnimClass (hanim.h ~line 32)
class HAnimClass(private val anim: W3dAnimation) {

    // C++: virtual const char* Get_Name() const = 0  [hanim.h ~line 38]
    val name: String get() = anim.name

    // C++: virtual const char* Get_HName() const = 0  [hanim.h ~line 39]
    val hName: String get() = anim.hierarchyName

    // C++: virtual int Get_Num_Frames() = 0  [hanim.h ~line 43]
    @get:JvmName("numFramesField")
    val numFrames: Int get() = anim.numFrames
    fun getNumFrames(): Int = numFrames

    // C++: virtual float Get_Frame_Rate() = 0  [hanim.h ~line 44]
    val frameRate: Int get() = anim.frameRate

    // C++: virtual float Get_Total_Time() = 0  [hanim.h ~line 45]
    // Total time = numFrames / frameRate (matching HRawAnimClass::Get_Total_Time)
    fun getTotalTime(): Float =
        if (frameRate > 0) anim.numFrames.toFloat() / anim.frameRate.toFloat() else 0f

    // C++: virtual const char* Get_Key()  [hanim.h ~line 41 — HashableClass key; same as Get_Name()]
    fun getKey(): String = anim.name

    // C++: virtual bool Has_Embedded_Sounds() const { return HasEmbeddedSounds; }  [hanim.h ~line 68]
    // Server-side: W3dAnimation does not parse sound channels; always false.
    fun hasEmbeddedSounds(): Boolean = false
}

// ============================================================
// HTreeClass
// C++: Code/ww3d2/htree.h — hierarchy (skeleton) with named pivots / bones
// Server-side: backed by a parsed W3dHierarchy; no transform computation needed server-side.
// ============================================================

// C++: HTreeClass (htree.h ~line 40)
class HTreeClass(private val hierarchy: W3dHierarchy) {

    // C++: const char* Get_Name() const  [htree.h ~line 48]
    val name: String get() = hierarchy.name

    // C++: int Num_Pivots() const  [htree.h ~line 49]
    val numPivots: Int get() = hierarchy.pivots.size

    // C++: int Get_Bone_Index(const char* name) const  [htree.h ~line 50]
    // Returns -1 if not found (matching C++ behaviour)
    fun getBoneIndex(boneName: String): Int {
        val lower = boneName.lowercase()
        return hierarchy.pivots.indexOfFirst { it.name.lowercase() == lower }
    }

    // C++: const char* Get_Bone_Name(int boneid) const  [htree.h ~line 51]
    fun getBoneName(boneId: Int): String? =
        hierarchy.pivots.getOrNull(boneId)?.name

    // C++: int Num_Pivots() via Get_Num_Bones() — same count; convenience alias matching usage in callers
    fun getNumBones(): Int = hierarchy.pivots.size
}

// ============================================================
// HAnimManagerClass
// C++: Code/ww3d2/hanimmgr.h / hanimmgr.cpp
// Manages the set of loaded animations; provides load/get/peek/add/free/missing support.
// ============================================================

// C++: HAnimManagerClass (hanimmgr.h ~line 33)
class HAnimManagerClass {

    // C++: HashTableClass* AnimPtrTable  [hanimmgr.h ~line 55]
    // Key is lowercase name (matching strlwr usage in HTreeManagerClass::Load_Tree)
    private val animTable: HashMap<String, HAnimClass> = HashMap()

    // C++: HashTableClass* MissingAnimTable  [hanimmgr.h ~line 56]
    private val missingAnims: HashSet<String> = HashSet()

    // C++: int Load_Anim(ChunkLoadClass&)  [hanimmgr.cpp ~line 65]
    // OMITTED: HAnimManagerClass::Load_Anim(ChunkLoadClass&) [hanimmgr.cpp ~line 65]
    // The server uses W3dFileParser instead of ChunkLoadClass-based streaming;
    // use loadAnim(W3dAnimation) to feed parsed animations into the manager.

    // Server-specific: feed a parsed W3dAnimation into the manager (mirrors C++ Add_Anim path)
    fun loadAnim(anim: W3dAnimation): Boolean {
        val key = anim.name.lowercase()
        if (animTable.containsKey(key)) return false   // duplicate — reject (matches C++ Load_Raw_Anim logic)
        val hanim = HAnimClass(anim)
        animTable[key] = hanim
        return true
    }

    // C++: HAnimClass* Get_Anim(const char* name)  [hanimmgr.cpp ~line 241]
    // Returns the anim (C++ does Add_Ref; server omits ref-counting — GC handles it)
    fun getAnim(name: String): HAnimClass? = animTable[name.lowercase()]

    // C++: HAnimClass* Peek_Anim(const char* name)  [hanimmgr.cpp ~line 224]
    // Peek does NOT Add_Ref in C++; same result in Kotlin (no ref-counting)
    fun peekAnim(name: String): HAnimClass? = animTable[name.lowercase()]

    // C++: bool Add_Anim(HAnimClass* new_anim)  [hanimmgr.cpp ~line 288]
    fun addAnim(anim: HAnimClass): Boolean {
        val key = anim.name.lowercase()
        if (animTable.containsKey(key)) return false
        animTable[key] = anim
        return true
    }

    // C++: void Free_All_Anims()  [hanimmgr.cpp ~line 262]
    fun freeAllAnims() {
        animTable.clear()
    }

    // C++: void Register_Missing(const char* name)  [hanimmgr.cpp ~line 312]
    fun registerMissing(name: String) {
        missingAnims.add(name.lowercase())
    }

    // C++: bool Is_Missing(const char* name)  [hanimmgr.cpp ~line 317]
    fun isMissing(name: String): Boolean = missingAnims.contains(name.lowercase())

    // C++: void Reset_Missing()  [hanimmgr.cpp ~line 322]
    fun resetMissing() {
        missingAnims.clear()
    }
}

// ============================================================
// HTreeManagerClass
// C++: Code/ww3d2/htreemgr.h / htreemgr.cpp
// Manages the set of loaded hierarchy trees.
// ============================================================

// C++: HTreeManagerClass (htreemgr.h ~line 20)
class HTreeManagerClass {

    // C++: HTreeClass* TreePtr[MAX_TREES]  [htreemgr.h ~line 47]
    // Using a list instead of a fixed 4096-element array; IDs are insertion indices.
    private val trees: MutableList<HTreeClass> = mutableListOf()

    // C++: HashTemplateClass<StringClass,HTreeClass*> TreeHash  [htreemgr.h ~line 48]
    // Key is lowercase name (matching strlwr in C++ Load_Tree)
    private val treeHash: HashMap<String, HTreeClass> = HashMap()

    // C++: int Load_Tree(ChunkLoadClass&)  [htreemgr.cpp ~line 98]
    // OMITTED: HTreeManagerClass::Load_Tree(ChunkLoadClass&) [htreemgr.cpp ~line 98]
    // The server uses W3dFileParser; use loadTree(W3dHierarchy) instead.

    // Server-specific: feed a parsed W3dHierarchy into the manager
    fun loadTree(hierarchy: W3dHierarchy): Boolean {
        val key = hierarchy.name.lowercase()
        if (treeHash.containsKey(key)) return false   // duplicate — reject (matches C++ Load_Tree logic)
        val htree = HTreeClass(hierarchy)
        trees.add(htree)
        treeHash[key] = htree
        return true
    }

    // C++: HTreeClass* Get_Tree(const char* name)  [htreemgr.cpp ~line 195]
    fun getTree(name: String): HTreeClass? = treeHash[name.lowercase()]

    // C++: HTreeClass* Get_Tree(int id)  [htreemgr.cpp ~line 222]
    fun getTree(id: Int): HTreeClass? = trees.getOrNull(id)

    // C++: int Get_Tree_ID(const char* name)  [htreemgr.cpp ~line 150]
    // Returns -1 if not found
    fun getTreeId(name: String): Int {
        val lower = name.lowercase()
        return trees.indexOfFirst { it.name.lowercase() == lower }
    }

    // C++: int Num_Trees()  [htreemgr.h ~line 29]
    fun numTrees(): Int = trees.size

    // C++: char* Get_Tree_Name(const int id)  [htreemgr.cpp ~line 172]
    fun getTreeName(id: Int): String? = trees.getOrNull(id)?.name

    // C++: void Free_All_Trees()  [htreemgr.cpp ~line 72]
    fun freeAllTrees() {
        trees.clear()
        treeHash.clear()
    }
}

// ============================================================
// WW3DAssetManager
// C++: Code/ww3d2/assetmgr.h / assetmgr.cpp
//
// SCOPE: Server-only — only functionality needed by a dedicated server is implemented.
//
// OMITTED groups:
//
// 1. Prototype / render-object system [assetmgr.cpp ~line 49–600]
//    OMITTED: WW3DAssetManager::Create_Render_Obj(), Render_Obj_Exists(),
//             Create_Render_Obj_Iterator(), Release_Render_Obj_Iterator(),
//             Add_Prototype(), Remove_Prototype(), Find_Prototype(),
//             Register_Prototype_Loader(), Find_Prototype_Loader(), Load_Prototype()
//    Dedicated server has no renderer — RenderObjClass is a no-op stub with no factory
//    infrastructure.  PrototypeClass / PrototypeLoaderClass also omitted.
//
// 2. Texture methods [assetmgr.cpp ~line 700–800]
//    OMITTED: Get_Texture(), Release_All_Textures(), Release_Unused_Textures(),
//             Release_Texture(), Load_Procedural_Textures(), Texture_Hash(),
//             Log_All_Textures(), Log_Texture_Statistics()
//    No TextureClass / renderer on the server.
//
// 3. Font methods [assetmgr.cpp ~line 850–950]
//    OMITTED: Get_Font3DInstance(), Get_FontChars(), Create_Font3DData_Iterator(),
//             Add_Font3DData(), Remove_Font3DData(), Get_Font3DData(),
//             Release_All_Font3DDatas(), Release_Unused_Font3DDatas(),
//             Release_All_FontChars()
//    No Font3DInstance / FontChars / renderer on the server.
//
// 4. Metal map manager [assetmgr.h ~line 224]
//    OMITTED: Peek_Metal_Map_Manager(), MetalMapManagerClass
//    No renderer on the server.
//
// 5. Load-on-demand / fog flags [assetmgr.h ~line 262–269]
//    OMITTED: Get_WW3D_Load_On_Demand(), Set_WW3D_Load_On_Demand(),
//             Get_Activate_Fog_On_Load(), Set_Activate_Fog_On_Load()
//    Server uses eager loading through load3DAssets(); fog is irrelevant.
//
// 6. Release_Unused_Assets() [assetmgr.cpp ~line 650]
//    OMITTED: Release_Unused_Assets() — no ref-counting on the server; GC handles lifetime.
// ============================================================

// C++: WW3DAssetManager (assetmgr.h ~line 139)
class WW3DAssetManager(private val mixReaders: List<MixReader>) {

    // C++: HTreeManagerClass HTreeManager  [assetmgr.h ~line 335]
    private val hTreeManager = HTreeManagerClass()

    // C++: HAnimManagerClass HAnimManager  [assetmgr.h ~line 336]
    private val hAnimManager = HAnimManagerClass()

    // --------------------------------------------------------
    // Load_3D_Assets
    // --------------------------------------------------------

    // C++: virtual bool Load_3D_Assets(const char* filename)  [assetmgr.cpp ~line 100]
    // Searches all mixReaders for the file, then delegates to the ByteArray overload.
    fun load3DAssets(filename: String): Boolean {
        val data = mixReaders.firstNotNullOfOrNull { it.readFile(filename) } ?: return false
        return load3DAssets(data)
    }

    // C++: virtual bool Load_3D_Assets(FileClass& assetfile)  [assetmgr.cpp ~line 120]
    // Server variant: accepts a ByteArray directly (no FileClass on server).
    // Parses with W3dFileParser and feeds hierarchies / animations into their managers.
    // Note: mesh / prototype loading is omitted (no renderer).
    fun load3DAssets(data: ByteArray): Boolean {
        val w3dFile = W3dFileParser.parse(data)

        // C++: W3D_CHUNK_HIERARCHY → HTreeManager.Load_Tree()  [assetmgr.cpp ~line 170]
        for (hierarchy in w3dFile.hierarchies) {
            hTreeManager.loadTree(hierarchy)
        }

        // C++: W3D_CHUNK_ANIMATION / W3D_CHUNK_COMPRESSED_ANIMATION → HAnimManager.Load_Anim()
        // [assetmgr.cpp ~line 175–180]
        for (anim in w3dFile.animations) {
            hAnimManager.loadAnim(anim)
        }

        // C++: W3D_CHUNK_MESH / W3D_CHUNK_HMODEL → PrototypeLoaders  [assetmgr.cpp ~line 155–165]
        // OMITTED: mesh prototype loading — no renderer on the server.

        return true
    }

    // --------------------------------------------------------
    // Get_HAnim
    // --------------------------------------------------------

    // C++: virtual HAnimClass* Get_HAnim(const char* name)  [assetmgr.cpp ~line 330 approx]
    // Checks missing-anim table first, then looks up in HAnimManager.
    // C++ does Add_Ref on the result; server omits ref-counting (GC handles lifetime).
    fun getHAnim(name: String): HAnimClass? {
        if (hAnimManager.isMissing(name)) return null
        val anim = hAnimManager.getAnim(name)
        if (anim == null) {
            hAnimManager.registerMissing(name)
        }
        return anim
    }

    // --------------------------------------------------------
    // Get_HTree
    // --------------------------------------------------------

    // C++: virtual HTreeClass* Get_HTree(const char* name)  [assetmgr.cpp ~line 350 approx]
    fun getHTree(name: String): HTreeClass? = hTreeManager.getTree(name)

    // --------------------------------------------------------
    // Add_Anim (delegate to HAnimManager)
    // --------------------------------------------------------

    // C++: virtual bool Add_Anim(HAnimClass* new_anim)  [assetmgr.h ~line 203]
    fun addAnim(anim: HAnimClass): Boolean = hAnimManager.addAnim(anim)

    // --------------------------------------------------------
    // Free_Assets / Free
    // --------------------------------------------------------

    // C++: virtual void Free_Assets()  [assetmgr.cpp ~line 620 approx]
    fun freeAssets() {
        hTreeManager.freeAllTrees()
        hAnimManager.freeAllAnims()
        hAnimManager.resetMissing()
        // OMITTED: prototype/texture/font cleanup — those systems are not present on server.
    }

    // C++: void Free()  [assetmgr.cpp ~line 640 approx, protected]
    fun free() {
        freeAssets()
    }

    // --------------------------------------------------------
    // Singleton
    // --------------------------------------------------------

    companion object {
        // C++: static WW3DAssetManager* TheInstance  [assetmgr.cpp ~line 38]
        private var theInstance: WW3DAssetManager? = null

        // C++: static WW3DAssetManager* Get_Instance()  [assetmgr.h ~line 160]
        fun getInstance(): WW3DAssetManager? = theInstance

        // C++: (constructor sets TheInstance)  [assetmgr.cpp ~line 60 approx]
        fun init(mixReaders: List<MixReader>): WW3DAssetManager {
            check(theInstance == null) { "WW3DAssetManager already initialized" }
            val instance = WW3DAssetManager(mixReaders)
            theInstance = instance
            return instance
        }

        // C++: static void Delete_This()  [assetmgr.h ~line 161]
        fun deleteThis() {
            theInstance = null
        }
    }
}

// ============================================================
// Top-level utility functions
// C++: Code/Combat/assets.cpp
// ============================================================

// C++: void Strip_Path_From_Filename(StringClass& new_name, const char* filename)  [assets.cpp ~line 42]
fun stripPathFromFilename(filename: String): String {
    val fwdIdx  = filename.lastIndexOf('/')
    val backIdx = filename.lastIndexOf('\\')
    val lastIdx = maxOf(fwdIdx, backIdx)
    return if (lastIdx >= 0) filename.substring(lastIdx + 1) else filename
}

// C++: void Get_Render_Obj_Name_From_Filename(StringClass& new_name, const char* filename)
// [assets.cpp ~line 53]
// Strips path and removes the last 4 characters (the ".w3d" extension).
fun getRenderObjNameFromFilename(filename: String): String {
    val stripped = stripPathFromFilename(filename)
    return if (stripped.length > 4) stripped.dropLast(4) else stripped
}

// C++: RenderObjClass* Create_Render_Obj_From_Filename(const char* filename)  [assets.cpp ~line 61]
// Server-side: the asset manager has no renderer — returns null (RenderObjClass is a stub).
// Calls WW3DAssetManager::Get_Instance()->Create_Render_Obj() which is omitted on server.
fun createRenderObjFromFilename(@Suppress("UNUSED_PARAMETER") filename: String): RenderObjClass? = null
