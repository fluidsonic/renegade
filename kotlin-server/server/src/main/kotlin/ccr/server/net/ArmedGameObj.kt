package ccr.server.net

import ccr.math.Vector3
import ccr.net.bitstream.*
import ccr.server.defs.ArmedGameObjDef
import ccr.server.defs.PhysicalGameObjDef

// C++: ArmedGameObj (armedgameobj.cpp)
// C++ hierarchy: PhysicalGameObj → ArmedGameObj
abstract class ArmedGameObj : PhysicalGameObj() {

    // C++: WeaponBagClass* WeaponBag — new WeaponBagClass(this) in C++ constructor
    val weaponBag: WeaponBagClass = WeaponBagClass(this)

    // C++: Vector3 TargetingPos
    var targeting: Vector3 = Vector3(0f, 0f, 0f)

    // C++: int MuzzleA0Bone, MuzzleA1Bone, MuzzleB0Bone, MuzzleB1Bone
    var muzzleA0Bone: Int = 0
    var muzzleA1Bone: Int = 0
    var muzzleB0Bone: Int = 0
    var muzzleB1Bone: Int = 0

    // C++: MuzzleRecoilClass MuzzleRecoilController[MAX_MUZZLES] — FIXME: not yet ported

    // C++: void Init(const ArmedGameObjDef&)
    fun init(definition: ArmedGameObjDef) {
        val physDef: ccr.server.defs.PhysicalGameObjDef = definition
        super<PhysicalGameObj>.init(physDef)
        copySettings(definition)
    }

    // C++: void Copy_Settings(const ArmedGameObjDef&)
    fun copySettings(definition: ArmedGameObjDef) {
        weaponBag.clearWeapons()
        weaponBag.addWeapon(definition.weaponDefId, definition.weaponRounds)
        if (definition.secondaryWeaponDefId != 0) {
            weaponBag.addWeapon(definition.secondaryWeaponDefId, definition.weaponRounds)
        }
    }

    // C++: void Re_Init(const ArmedGameObjDef&)
    fun reInit(definition: ArmedGameObjDef) {
        val physDef: ccr.server.defs.PhysicalGameObjDef = definition
        super<PhysicalGameObj>.reInit(physDef)
        copySettings(definition)
    }

    // C++: const ArmedGameObjDef& Get_Definition() const
    fun getArmedDefinition(): ArmedGameObjDef = definition as ArmedGameObjDef

    // C++: ~ArmedGameObj() — delete WeaponBag; GC handles this in Kotlin
    override fun destruct() {
        super.destruct()
    }

    // C++: WeaponClass* Get_Weapon() — delegates to weaponBag
    fun getWeapon(): WeaponClass? = weaponBag.getWeapon()

    // C++: bool Muzzle_Exists(int index)
    fun muzzleExists(index: Int): Boolean = when (index) {
        0 -> muzzleA0Bone != 0
        1 -> muzzleA1Bone != 0
        2 -> muzzleB0Bone != 0
        3 -> muzzleB1Bone != 0
        else -> false
    }

    // C++: virtual const Matrix3D& Get_Muzzle(int index) — FIXME: requires model/bone system
    open fun getMuzzle(index: Int = 0): ccr.math.Matrix3D = getTransform()

    // C++: void Start_Recoil(int muzzle_index, float recoil_scale, float recoil_time)
    // WWASSERT(muzzle_index >= 0 && muzzle_index < MAX_MUZZLES)
    fun startRecoil(muzzleIndex: Int, recoilScale: Float, recoilTime: Float) {
        require(muzzleIndex in 0 until MAX_MUZZLES)
        // FIXME: MuzzleRecoilController[muzzleIndex].Start_Recoil() — not yet ported
    }

    // C++: float Get_Weapon_Error() { return Get_Definition().WeaponError; }
    fun getWeaponError(): Float = getArmedDefinition().weaponError

    // C++: WeaponBagClass* Get_Weapon_Bag() — covered by val weaponBag

    // C++: Vector3 Get_Targeting_Pos() — covered by var targeting
    // C++: virtual bool Set_Targeting(const Vector3& pos, bool do_tilt = true)
    open fun setTargeting(pos: Vector3, doTilt: Boolean = true): Boolean {
        targeting = pos
        return true
    }

    // C++: virtual bool Save(ChunkSaveClass&)
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_ARMED_VARIABLES)
        csave.writeMicroChunk(MICROCHUNKID_TARGETING_POS, targeting)
        csave.endChunk()

        csave.beginChunk(CHUNKID_WEAPONBAG)
        weaponBag.save(csave)
        csave.endChunk()

        return true
    }

    // C++: virtual bool Load(ChunkLoadClass&)
    override fun load(cload: ChunkLoadClass): Boolean {
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT    -> super.load(cload)
                CHUNKID_ARMED_VARIABLES -> {
                    while (cload.openMicroChunk()) {
                        when (cload.curMicroChunkId) {
                            MICROCHUNKID_TARGETING_POS -> targeting = cload.readVector3()
                            else -> error("Unrecognized ArmedGameObj variable chunk ID: ${cload.curMicroChunkId}")
                        }
                        cload.closeMicroChunk()
                    }
                }
                CHUNKID_WEAPONBAG -> weaponBag.load(cload)
                else -> error("Unrecognized ArmedGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }
        // FIXME: C++ calls SaveLoadSystemClass::Register_Post_Load_Callback(this) here — not yet ported
        return true
    }

    companion object {
        private const val MAX_MUZZLES           = 4
        private const val CHUNKID_PARENT        = 418001841
        internal const val CHUNKID_ARMED_VARIABLES = 418001842  // C++: CHUNKID_VARIABLES in ArmedGameObj
        private const val CHUNKID_WEAPONBAG     = 418001843

        private const val MICROCHUNKID_TARGETING_POS = 1
    }

    // C++: void On_Post_Load()
    override fun onPostLoad() {
        super.onPostLoad()
        // FIXME: C++ calls Init_Muzzle_Bones() here — requires model/bone system, not yet ported
    }

    // C++: ArmedGameObj::Post_Think
    override fun postThink() {
        super.postThink()
        if (isDeletePending) return
        weaponBag.getWeapon()?.update()
        // FIXME: MuzzleRecoilController[i].Update() — requires model system, not yet ported
    }

    // C++: ArmedGameObj::Export_State_Cs — sends relative targeting (targeting - position)
    open fun exportStateCs(packet: BitStream) {
        val myPos = getPosition()
        val rel = targeting - myPos
        packet.addFloat(rel.x, BITPACK_WORLD_POSITION_X)
        packet.addFloat(rel.y, BITPACK_WORLD_POSITION_Y)
        packet.addFloat(rel.z, BITPACK_WORLD_POSITION_Z)
    }

    // C++: ArmedGameObj::Import_State_Cs — receives relative targeting, adds position
    open fun importStateCs(packet: BitStream) {
        val rel = ccr.math.Vector3(
            packet.getFloat(BITPACK_WORLD_POSITION_X),
            packet.getFloat(BITPACK_WORLD_POSITION_Y),
            packet.getFloat(BITPACK_WORLD_POSITION_Z),
        )
        val myPos = getPosition()
        setTargeting(rel + myPos)
    }

    // C++: ArmedGameObj::Export_Frequent — calls PhysicalGameObj::Export_Frequent then writes targeting.
    override fun exportFrequent(packet: BitStream) {
        super.exportFrequent(packet)
        packet.addFloat(targeting.x, BITPACK_WORLD_POSITION_X)
        packet.addFloat(targeting.y, BITPACK_WORLD_POSITION_Y)
        packet.addFloat(targeting.z, BITPACK_WORLD_POSITION_Z)
    }

    // C++: ArmedGameObj::Import_Frequent — calls PhysicalGameObj::Import_Frequent then reads targeting.
    override fun importFrequent(packet: BitStream) {
        super.importFrequent(packet)
        targeting = Vector3(
            packet.getFloat(BITPACK_WORLD_POSITION_X),
            packet.getFloat(BITPACK_WORLD_POSITION_Y),
            packet.getFloat(BITPACK_WORLD_POSITION_Z),
        )
    }
}
