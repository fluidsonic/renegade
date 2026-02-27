package ccr.server.net

import ccr.math.Vector3
import ccr.physics.collision.OverlapType
import ccr.server.GameObjManager
import ccr.server.defs.OBBoxClass
import ccr.server.defs.ScriptZoneGameObjDef
import ccr.server.defs.ZoneType

// C++: ScriptZoneGameObj : public ScriptableGameObj (scriptzone.h / scriptzone.cpp)
// C++ hierarchy: ScriptZoneGameObj : ScriptableGameObj
class ScriptZoneGameObj : ScriptableGameObj() {

    // C++: OBBoxClass BoundingBox
    // @JvmName avoids clash with fun getBoundingBox() / fun setBoundingBox() below
    @get:JvmName("boundingBoxField") @set:JvmName("setBoundingBoxField")
    var boundingBox: OBBoxClass = OBBoxClass()  // C++: OBBoxClass BoundingBox

    // C++: int PlayerType (initialized to PLAYERTYPE_NEUTRAL)
    var playerType: Int = PLAYERTYPE_NEUTRAL  // C++: int PlayerType

    // C++: SList<GameObjReference> InsideList
    // C++: intrusive linked list of GameObjReferences to SmartGameObjs currently inside the zone
    private val insideList: MutableList<GameObjReference> = mutableListOf()  // C++: SList<GameObjReference> InsideList

    init {
        // C++: ScriptZoneGameObj() : PlayerType(PLAYERTYPE_NEUTRAL) {}
    }

    // C++: ~ScriptZoneGameObj()
    override fun destruct() {
        // C++: while ((ref = InsideList.Remove_Head()) != NULL) { *ref = NULL; delete ref; }
        for (ref in insideList) {
            ref.set(null)
        }
        insideList.clear()
        super.destruct()
    }

    // C++: virtual void Init()
    override fun init() {
        init(getDefinition())
    }

    // C++: void Init(const ScriptZoneGameObjDef & definition)
    fun init(definition: ScriptZoneGameObjDef) {
        super.init(definition)
    }

    // C++: const ScriptZoneGameObjDef & Get_Definition() const
    fun getDefinition(): ScriptZoneGameObjDef = definition as ScriptZoneGameObjDef

    // C++: ScriptZoneGameObj * As_ScriptZoneGameObj() { return this; }
    fun asScriptZoneGameObj(): ScriptZoneGameObj = this

    // C++: virtual void Think()
    override fun think() {
        super.think()

        // C++: if (Get_Observers().Count() == 0 && Get_Definition().ZoneType != TYPE_CTF) return;
        if (observers.isEmpty() && getDefinition().zoneType != ZoneType.TYPE_CTF) {
            return
        }

        // C++: check current objects for exiting
        val toRemove = mutableListOf<GameObjReference>()
        for (ref in insideList.toList()) {
            val obj = ref.get() as? SmartGameObj

            if (obj == null) {
                // C++: Debug_Say(("Object died inside me\n"))
                toRemove.add(ref)
            } else if (!insideMe(obj)) {
                // C++: observer_list[index]->Exited(this, (PhysicalGameObj*)obj)
                for (observer in observers.toList()) {
                    observer.exited(this, obj)
                }
                toRemove.add(ref)
            }
        }
        for (ref in toRemove) {
            insideList.remove(ref)
            ref.set(null)
        }

        // C++: if (Get_Definition().CheckStarsOnly) — check stars only path
        if (getDefinition().checkStarsOnly) {
            // C++: check all stars for entering
            for (obj in GameObjManager.getStarList()) {
                if (insideMe(obj) && !inList(obj)) {
                    entered(obj)
                }
            }
        } else {
            // C++: Collect_Objects(BoundingBox, false, true, &objs_in_zone)
            val scene = CombatManager.getScene()
            val objsInZone: List<PhysClass> = scene?.collectObjects(boundingBox, false, true) ?: emptyList()
            for (physObj in objsInZone) {
                val observer = physObj.getObserver()
                val smartObj = if (observer is CombatPhysObserverClass) {
                    observer.asSmartGameObj()
                } else {
                    null
                }
                if (smartObj != null && insideMe(smartObj) && !inList(smartObj)) {
                    entered(smartObj)
                }
            }
        }
    }

    // C++: virtual void Get_Position(Vector3 * set_pos) const { *set_pos = BoundingBox.Center; }
    override fun getPosition(): Vector3 = boundingBox.center

    // C++: void Set_Bounding_Box(OBBoxClass & box) { BoundingBox = box; }
    fun setBoundingBox(box: OBBoxClass) { boundingBox = box }

    // C++: const OBBoxClass & Get_Bounding_Box() { return BoundingBox; }
    fun getBoundingBox(): OBBoxClass = boundingBox

    // C++: int Get_Player_Type() const { return PlayerType; }
    // C++: void Set_Player_Type(int type) { PlayerType = type; }
    // Covered by var playerType above.

    // C++: int Count_Team_Members_Inside(int player_type)
    fun countTeamMembersInside(playerTypeFilter: Int): Int {
        var count = 0
        for (ref in insideList) {
            val obj = ref.get() as? SmartGameObj ?: continue
            if (obj.playerType == playerTypeFilter) {
                count++
            }
        }
        return count
    }

    // C++: bool Is_Environment_Zone() { return Get_Definition().IsEnvironmentZone; }
    fun isEnvironmentZone(): Boolean = getDefinition().isEnvironmentZone

    // C++: virtual bool Save(ChunkSaveClass & csave)
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_VARIABLES)
        csave.writeMicroChunk(MICROCHUNKID_BOUNDING_BOX, boundingBox)
        csave.writeMicroChunk(MICROCHUNKID_PLAYER_TYPE,  playerType)
        csave.endChunk()

        csave.beginChunk(CHUNKID_INSIDE_LIST)
        for (ref in insideList) {
            if (ref.get() != null) {
                csave.beginChunk(CHUNKID_INSIDE_LIST_ENTRY)
                // C++: ref->Save(csave) — ReferencerClass::Save writes old pointer for remap
                // FIXME: GameObjReference.save() not yet ported — pointer remap system not ported
                csave.endChunk()
            }
        }
        csave.endChunk()

        return true
    }

    // C++: virtual bool Load(ChunkLoadClass & cload)
    override fun load(cload: ChunkLoadClass): Boolean {
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT -> super.load(cload)

                CHUNKID_VARIABLES -> {
                    while (cload.openMicroChunk()) {
                        when (cload.curMicroChunkId) {
                            MICROCHUNKID_BOUNDING_BOX -> boundingBox = cload.readOBBox()
                            MICROCHUNKID_PLAYER_TYPE  -> playerType  = cload.readInt()
                            else -> error("Unrecognized ScriptZoneGameObj variable chunk ID: ${cload.curMicroChunkId}")
                        }
                        cload.closeMicroChunk()
                    }
                }

                CHUNKID_INSIDE_LIST -> {
                    while (cload.openChunk()) {
                        val ref = GameObjReference()
                        // C++: ref->Load(cload) — ReferencerClass::Load reads old pointer for remap
                        // FIXME: GameObjReference.load() not yet ported — pointer remap system not ported
                        insideList.add(0, ref)  // C++: InsideList.Add_Head(ref)
                        cload.closeChunk()
                    }
                }

                else -> error("Unrecognized ScriptZoneGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }

        // C++: Legacy Fix — remap old player type values
        playerType = when (playerType) {
            2 -> PLAYERTYPE_NEUTRAL  // Remap TEAM2
            3 -> PLAYERTYPE_NEUTRAL  // Remap TEAM3
            4 -> PLAYERTYPE_NOD      // Remap NOD
            5 -> PLAYERTYPE_GDI      // Remap GDI
            else -> playerType
        }

        // FIXME: SaveLoadSystemClass::Register_Post_Load_Callback(this)
        return true
    }

    // C++: void Entered(SmartGameObj * obj)
    private fun entered(obj: SmartGameObj) {
        // C++: observer_list[index]->Entered(this, obj)
        for (observer in observers.toList()) {
            observer.entered(this, obj)
        }

        // C++: Create a new reference and add it to InsideList
        val ref = GameObjReference(obj)
        insideList.add(0, ref)  // C++: InsideList.Add_Head(ref)
    }

    // C++: bool In_List(SmartGameObj * obj)
    private fun inList(obj: SmartGameObj): Boolean {
        for (ref in insideList) {
            if (obj === ref.get()) return true
        }
        return false
    }

    // C++: bool Inside_Me(const SmartGameObj * obj)
    private fun insideMe(obj: SmartGameObj): Boolean {
        // C++: if (obj && obj->Peek_Physical_Object()) — hack to hide zones
        if (obj.peekPhysicalObject() != null) {
            val pos = obj.getPosition()
            return CollisionMath.overlapTest(boundingBox, pos) == OverlapType.INSIDE
        }
        return false
    }

    companion object {
        // C++: CHUNKID_GAME_OBJECT_SCRIPT_ZONE = 0x00040122 (combatchunkid.h)
        const val CHUNK_ID: UInt = 0x00040122u

        // Chunk IDs from scriptzone.cpp local enum (starting at CHUNKID_PARENT_OLD = 922991806)
        private const val CHUNKID_PARENT_OLD       = 922991806  // legacy, not written in Save
        private const val CHUNKID_VARIABLES        = 922991807
        private const val CHUNKID_INSIDE_LIST      = 922991808
        private const val CHUNKID_INSIDE_LIST_ENTRY= 922991809
        private const val CHUNKID_PARENT           = 922991810

        // Micro-chunk IDs inside CHUNKID_VARIABLES
        private const val MICROCHUNKID_BOUNDING_BOX = 1
        private const val MICROCHUNKID_PLAYER_TYPE  = 2

        // C++: static ScriptZoneGameObj * Find_Closest_Zone(const Vector3 & pos, ZoneConstants::ZoneType type)
        fun findClosestZone(pos: Vector3, type: Int): ScriptZoneGameObj? {
            var closestDist2 = 999999.0f
            var closestZone: ScriptZoneGameObj? = null

            for (gameObj in GameObjManager.getAllObjects()) {
                if (gameObj is ScriptZoneGameObj) {
                    if (gameObj.getDefinition().getType() == type) {
                        val diff = pos - gameObj.getBoundingBox().center
                        val dist2 = diff.x * diff.x + diff.y * diff.y + diff.z * diff.z
                        if (dist2 < closestDist2) {
                            closestDist2 = dist2
                            closestZone = gameObj
                        }
                    }
                }
            }

            return closestZone
        }
    }
}
