package ccr.server.net

import ccr.math.Matrix3D
import ccr.math.Vector3
import ccr.net.bitstream.*
import ccr.server.GameObjManager
import ccr.server.defs.PhysicalGameObjDef

// C++: PhysicalGameObj : public DamageableGameObj, public CombatPhysObserverClass
abstract class PhysicalGameObj : DamageableGameObj(), CombatPhysObserverClass {

    // C++: PhysClass* PhysObj (initialized to NULL)
    var physObj: PhysClass? = null

    // C++: AnimControlClass* AnimControl (initialized to NULL)
    // @JvmName avoids clash with explicit getAnimControl()/setAnimControl() methods below
    @get:JvmName("animControlField") @set:JvmName("setAnimControlField")
    var animControl: AnimControlClass? = null

    // C++: BYTE ServerUpdateSkips
    var serverUpdateSkips: UByte = 0u

    // C++: float HibernationTimer (initialized to 0 — starts asleep)
    var hibernationTimer: Float = 0f

    // C++: bool HibernationEnable (initialized to true)
    var hibernationEnable: Boolean = true

    // C++: GameObjReference HostGameObj
    val hostGameObj: GameObjReference = GameObjReference()

    // C++: int HostGameObjBone
    var hostGameObjBone: Int = 0

    // C++: int RadarBlipShapeType
    var radarBlipShapeType: Int = 0

    // C++: int RadarBlipColorType
    var radarBlipColorType: Int = 0

    // C++: float RadarBlipIntensity
    var radarBlipIntensity: Float = 0f

    // C++: int PendingHostObjID
    var pendingHostObjId: Int = 0

    // C++: bool HUDPokableIndicatorEnabled (initialized to false)
    var hudPokableIndicatorEnabled: Boolean = false

    // C++: bool IsInnateConversationsEnabled (initialized to true)
    var isInnateConversationsEnabled: Boolean = true

    // C++: ActiveConversationClass* ActiveConversation (protected, initialized to NULL)
    protected var activeConversation: ActiveConversationClass? = null

    init {
        // C++: PhysicalGameObj() { Reset_Server_Skips(255); }
        resetServerSkips(255u)
    }

    // C++: ~PhysicalGameObj()
    override fun destruct() {
        animControl = null
        physObj?.let {
            // C++: COMBAT_SCENE->Remove_Object(PhysObj) — COMBAT_SCENE = CombatManager::Get_Scene()
            CombatManager.getScene()?.removeObject(it)
            physObj = null
        }
        // C++: REF_PTR_RELEASE(ActiveConversation)
        activeConversation = null
        super.destruct()
    }

    // C++: void Init(const PhysicalGameObjDef & definition)
    fun init(definition: PhysicalGameObjDef) {
        super.init(definition)
        copySettings(definition)
        hideMuzzleFlashes()
        if (definition.useCreationEffect) {
            // FIXME: CombatMaterialEffectManager::Get_Spawn_Effect() + PhysObj->Add_Effect_To_Me(effect)
            // — wire when material effect system is ported
        }
    }

    // C++: void Copy_Settings(const PhysicalGameObjDef & definition)
    fun copySettings(definition: PhysicalGameObjDef) {
        physObj?.let {
            CombatManager.getScene()?.removeObject(it)
            physObj = null
        }
        check(physObj == null)
        val poDef = DefinitionMgrClass.findDefinition(definition.physDefId) as PhysDefClass
        val newPhysObj = poDef.create() as PhysClass
        check(newPhysObj != null)
        newPhysObj.setCollisionGroup(DEFAULT_COLLISION_GROUP)
        newPhysObj.setObserver(this)
        CombatManager.getScene()?.addDynamicObject(newPhysObj)
        physObj = newPhysObj
        if (definition.animation.isNotEmpty()) {
            setAnimation(definition.animation)
        }
        enableHibernation(definition.defaultHibernationEnable)
        resetRadarBlipShapeType()
    }

    // C++: void Re_Init(const PhysicalGameObjDef & definition)
    fun reInit(definition: PhysicalGameObjDef) {
        val tm = getTransform()
        super.reInit(definition)
        copySettings(definition)
        setTransform(tm)
    }

    // C++: const PhysicalGameObjDef & Get_Definition() const
    // FIXME: definition is nullable — wire proper non-null guarantee when init() is always called before use
    fun getPhysicalDefinition(): PhysicalGameObjDef = definition as PhysicalGameObjDef

    // C++: virtual int Get_Vis_ID()
    override fun getVisId(): Int = physObj?.getVisObjectId() ?: -1

    // C++: virtual bool Get_World_Position(Vector3 & pos) const { Get_Position(&pos); return true; }
    override fun getWorldPosition(): Vector3 = getPosition()

    // C++: void Set_Conversation(ActiveConversationClass * conversation)
    fun setConversation(conversation: ActiveConversationClass?) { activeConversation = conversation }

    // C++: bool Are_Innate_Conversations_Enabled()
    fun areInnateConversationsEnabled(): Boolean = getPhysicalDefinition().allowInnateConversations && isInnateConversationsEnabled

    // C++: void Enable_Innate_Conversations(bool enable)
    fun enableInnateConversations(enable: Boolean) { isInnateConversationsEnabled = enable }

    // C++: bool Is_In_Conversation() const
    fun isInConversation(): Boolean = activeConversation != null

    // C++: void Enable_HUD_Pokable_Indicator(bool enable)
    fun enableHudPokableIndicator(enable: Boolean) {
        hudPokableIndicatorEnabled = enable
        setObjectDirtyBit(BIT_RARE, true)
    }

    // C++: virtual void Object_Shattered_Something(PhysClass*, PhysClass*, int surface_type)
    override fun objectShatteredSomething(observedObj: PhysClass, shatteredObj: PhysClass, surfaceType: Int) {
        val tm = observedObj.getTransform()
        SurfaceEffectsManager.applyEffect(surfaceType, SurfaceEffectsManager.HITTER_TYPE_BULLET, tm)
    }

    // C++: virtual void Startup() {}
    open fun startup() {}

    // C++: bool Is_Attached_To_An_Object() { return (HostGameObj.Get_Ptr() != NULL); }
    fun isAttachedToAnObject(): Boolean = hostGameObj.get() != null

    // C++: BYTE Get_Server_Skips()
    fun getServerSkips(): UByte = serverUpdateSkips

    // C++: WWINLINE PhysClass* Peek_Physical_Object() const { return PhysObj; }
    fun peekPhysicalObject(): PhysClass? = physObj

    // C++: bool Is_Soft()
    fun isSoft(): Boolean = defenseObject.isSoft()

    // C++: virtual bool Takes_Explosion_Damage() { return true; }
    open fun takesExplosionDamage(): Boolean = true

    // C++: int Get_Type() const
    fun getType(): Int = getPhysicalDefinition().type

    // C++: float Get_Bullseye_Offset_Z() const { return Get_Definition().BullseyeOffsetZ; }
    fun getBullseyeOffsetZ(): Float = getPhysicalDefinition().bullseyeOffsetZ

    // C++: virtual Vector3 Get_Bullseye_Position()
    open fun getBullseyePosition(): Vector3 = getPosition()

    // C++: void Set_Collision_Group(int group)
    fun setCollisionGroup(group: Int) { physObj!!.setCollisionGroup(group) }

    // C++: virtual ExpirationReactionType Object_Expired(PhysClass * observed_obj)
    override fun objectExpired(observedObj: PhysClass): ExpirationReactionType {
        setDeletePending()
        return ExpirationReactionType.APPROVED
    }

    // C++: WWINLINE RenderObjClass* Peek_Model() { return Peek_Physical_Object()->Peek_Model(); }
    fun peekModel(): RenderObjClass? = physObj?.peekModel()

    // C++: void Hide_Muzzle_Flashes(bool hide = true)
    fun hideMuzzleFlashes(hide: Boolean = true) {
        val model = peekModel() ?: return
        for (i in 0 until model.getNumSubObjects()) {
            val robj = model.getSubObject(i)
            if ("MUZZLEFLASH" in robj.getName() || "MZ" in robj.getName()) {
                robj.setHidden(hide)
            }
        }
    }

    // C++: AnimControlClass* Get_Anim_Control()
    fun getAnimControl(): AnimControlClass? = animControl

    // C++: void Set_Anim_Control(AnimControlClass * anim_control)
    fun setAnimControl(animControl: AnimControlClass) {
        check(this.animControl == null)
        this.animControl = animControl
        check(this.animControl != null)
    }

    // C++: virtual void Set_Animation(const char * animation_name, bool looping, float frame_offset)
    open fun setAnimation(animationName: String?, looping: Boolean = true, frameOffset: Float = 0f) {
        if (animControl == null) {
            setAnimControl(SimpleAnimControlClass())
        }
        if (!animationName.isNullOrEmpty()) {
            val animName = if ('.' in animationName) animationName
                           else createAnimationName(animationName, physObj!!.peekModel()!!.getName())
            animControl!!.setModel(physObj!!.peekModel()!!)
            animControl!!.setAnimation(animName, 0, frameOffset)
            animControl!!.setMode(if (looping) ANIM_MODE_LOOP else ANIM_MODE_ONCE)
            animControl!!.update(0f)
            setObjectDirtyBit(BIT_RARE, true)
        }
    }

    // C++: virtual void Set_Animation_Frame(const char * animation_name, int frame)
    open fun setAnimationFrame(animationName: String?, frame: Int) {
        if (animControl == null) {
            setAnimControl(SimpleAnimControlClass())
        }
        if (!animationName.isNullOrEmpty()) {
            val animName = if ('.' in animationName) animationName
                           else createAnimationName(animationName, physObj!!.peekModel()!!.getName())
            animControl!!.setModel(physObj!!.peekModel()!!)
            animControl!!.setAnimation(animName, 0)
            animControl!!.setMode(ANIM_MODE_STOP, frame)
            setObjectDirtyBit(BIT_RARE, true)
        }
    }

    // C++: void Attach_To_Object_Bone(PhysicalGameObj * host, const char * bone_name)
    fun attachToObjectBone(host: PhysicalGameObj?, boneName: String) {
        teleportToHostBone()
        if (hostGameObj.get() !== host && host == null) {
            // C++: zero velocity on detach via RigidBodyClass
            // FIXME: wire velocity zeroing when physics pipeline is ported
        }
        hostGameObj.set(host)
        if (host != null) {
            hostGameObjBone = host.physObj!!.peekModel()!!.getBoneIndex(boneName)
            teleportToHostBone()
        }
        setObjectDirtyBit(BIT_RARE, true)
    }

    // C++: void Teleport_To_Host_Bone()
    fun teleportToHostBone() {
        val host = hostGameObj.get() as? PhysicalGameObj ?: return
        val model = host.physObj?.peekModel() ?: return
        val newTransform = model.getBoneTransform(hostGameObjBone)
        physObj?.let { phys ->
            phys.asMoveablePhysClass()?.let { moveable ->
                moveable.setVelocity(Vector3())
                moveable.cinematicMoveTo(newTransform)
            } ?: phys.setTransform(newTransform)
        }
        resetHibernating()
    }

    // C++: virtual void Apply_Damage(const OffenseObjectClass & damager, float scale, int alternate_skin)
    override open fun applyDamage(damager: OffenseObjectClass, scale: Float, alternateSkin: Int) {
        if (!CombatManager.canDamage(damager.getOwner(), this)) return
        super.applyDamage(damager, scale, alternateSkin)
    }

    // C++: virtual void Apply_Damage_Extended(...)
    open fun applyDamageExtended(damager: OffenseObjectClass, scale: Float = 1f, direction: Vector3 = Vector3(), collisionBoxName: String? = null) {
        applyDamage(damager, scale)
    }

    // C++: virtual void Completely_Damaged(const OffenseObjectClass & damager)
    override fun completelyDamaged(damager: OffenseObjectClass) {
        if (getPhysicalDefinition().killedExplosion != 0) {
            val pos = getPosition()
            val zRot = getTransform().getZRotation()
            val tm = Matrix3D(m03 = pos.x, m13 = pos.y, m23 = pos.z).also { it.rotateZ(zRot) }
            ExplosionManager.createExplosionAt(getPhysicalDefinition().killedExplosion, tm, damager.getOwner())
            // C++: if (damager.Get_Owner() == COMBAT_STAR) EncyclopediaMgrClass::Reveal_Object(this)
            // FIXME: wire EncyclopediaMgrClass::Reveal_Object when ported
        }
        setDeletePending()
    }

    // C++: virtual void Set_Player_Type(int type)
    override var playerType: Int
        get() = super.playerType
        set(value) {
            super.playerType = value
            resetRadarBlipColorType()
        }

    // C++: void Reset_Radar_Blip_Shape_Type()
    fun resetRadarBlipShapeType() { radarBlipShapeType = getPhysicalDefinition().radarBlipType }

    // C++: void Reset_Radar_Blip_Color_Type()
    fun resetRadarBlipColorType() {
        radarBlipColorType = when (playerType) {
            PLAYERTYPE_NOD      -> RadarManager.BLIP_COLOR_TYPE_NOD
            PLAYERTYPE_GDI      -> RadarManager.BLIP_COLOR_TYPE_GDI
            PLAYERTYPE_MUTANT   -> RadarManager.BLIP_COLOR_TYPE_MUTANT
            PLAYERTYPE_RENEGADE -> RadarManager.BLIP_COLOR_TYPE_RENEGADE
            else                -> RadarManager.BLIP_COLOR_TYPE_NEUTRAL
        }
    }

    // C++: void Reset_Server_Skips(BYTE value)
    fun resetServerSkips(value: UByte) { serverUpdateSkips = value }

    // C++: void Increment_Server_Skips()
    fun incrementServerSkips() { if (serverUpdateSkips < 254u) serverUpdateSkips++ }

    // C++: virtual bool Is_Hibernating() { return HibernationTimer <= 0; }
    override fun isHibernating(): Boolean = hibernationTimer <= 0f

    // C++: void Enable_Hibernation(bool enable)
    fun enableHibernation(enable: Boolean) {
        hibernationEnable = enable
        if (isHibernating()) hibernationTimer = 1f
    }

    // C++: void Reset_Hibernating()
    fun resetHibernating() {
        if (isHibernating()) endHibernation()
        hibernationTimer = minOf(HIBERNATION_DELAY, hibernationTimer + TimeManager.getFrameSeconds() * 2f)
    }

    // C++: virtual void Begin_Hibernation() — debug log only
    open fun beginHibernation() {}

    // C++: virtual void End_Hibernation() — debug log only
    open fun endHibernation() {}

    // C++: virtual void Post_Think()
    override fun postThink() {
        // Handle pending host bone attachment
        if (pendingHostObjId != 0) {
            resetHibernating()
            hostGameObj.set(GameObjManager.findPhysicalGameObj(pendingHostObjId))
            setObjectDirtyBit(BIT_RARE, true)
            if (hostGameObj.get() != null) {
                pendingHostObjId = 0
            }
        }

        if (hostGameObj.get() != null) {
            teleportToHostBone()
        }

        super.postThink()

        // Hibernation countdown
        if (hibernationEnable && hibernationTimer > 0f) {
            hibernationTimer -= TimeManager.getFrameSeconds()
            if (hibernationTimer <= 0f) {
                beginHibernation()
            }
        }

        // Animation update
        animControl?.let { anim ->
            val wasComplete = anim.isComplete()
            anim.update(TimeManager.getFrameSeconds())
            if (!wasComplete && anim.isComplete()) {
                if (this !is SmartGameObj || !action.isAnimating()) {
                    for (observer in getObservers()) {
                        observer.animationComplete(this, anim.getAnimationName())
                    }
                }
            }
        }

        // "Going to hell" check — object fell below level bounds
        val scene = CombatManager.getScene()
        if (scene != null) {
            val pos = getPosition()
            val min = scene.getLevelMin()
            if (pos.z < min.z - 20f) {
                setDeletePending()
            }
        }
    }

    // C++: virtual void On_Post_Load()
    override fun onPostLoad() {
        check(physObj != null)
        physObj!!.setObserver(this)
        hideMuzzleFlashes()
        super.onPostLoad()
    }

    // C++: virtual bool Save(ChunkSaveClass & csave)
    override fun save(csave: ChunkSaveClass): Boolean {
        csave.beginChunk(CHUNKID_PARENT)
        super.save(csave)
        csave.endChunk()

        csave.beginChunk(CHUNKID_PHYSICAL_VARIABLES)
        // FIXME: MICROCHUNKID_PHYS_OBSERVER_PTR — pointer remap not ported
        // FIXME: MICROCHUNKID_PHYSICAL_OBJECT — pointer remap not ported
        csave.writeMicroChunk(MICROCHUNKID_HIBERNATION_TIMER, hibernationTimer)
        csave.writeMicroChunk(MICROCHUNKID_HIBERNATION_ENABLE, hibernationEnable)
        csave.writeMicroChunk(MICROCHUNKID_HOST_GAME_OBJ_BONE, hostGameObjBone)
        csave.writeMicroChunk(MICROCHUNKID_RADAR_BLIP_SHAPE_TYPE, radarBlipShapeType)
        csave.writeMicroChunk(MICROCHUNKID_RADAR_BLIP_COLOR_TYPE, radarBlipColorType)
        csave.writeMicroChunk(MICROCHUNKID_RADAR_BLIP_INTENSITY, radarBlipIntensity)
        // FIXME: MICROCHUNKID_ACTIVE_CONVERSATION — pointer remap not ported
        csave.writeMicroChunk(MICROCHUNKID_HUD_POKABLE_INDICATOR, hudPokableIndicatorEnabled)
        csave.writeMicroChunk(MICROCHUNKID_IS_INNATE_CONVERSATIONS_ENABLED, isInnateConversationsEnabled)
        csave.endChunk()

        animControl?.let {
            csave.beginChunk(CHUNKID_ANIM_CONTROL)
            it.save(csave)
            csave.endChunk()
        }

        if (hostGameObj.get() != null) {
            csave.beginChunk(CHUNKID_HOST_GAME_OBJ)
            hostGameObj.save(csave)
            csave.endChunk()
        }

        return true
    }

    // C++: virtual bool Load(ChunkLoadClass & cload)
    override fun load(cload: ChunkLoadClass): Boolean {
        check(physObj == null)
        while (cload.openChunk()) {
            when (cload.curChunkId) {
                CHUNKID_PARENT          -> super.load(cload)
                LEGACY_CHUNKID_PARENT_OLD -> super.load(cload)  // C++: ScriptableGameObj::Load
                LEGACY_CHUNKID_DEFENSE  -> defenseObject.load(cload)
                CHUNKID_PHYSICAL_VARIABLES -> {
                    while (cload.openMicroChunk()) {
                        when (cload.curMicroChunkId) {
                            // FIXME: MICROCHUNKID_PHYS_OBSERVER_PTR — pointer remap not ported
                            // FIXME: MICROCHUNKID_PHYSICAL_OBJECT — pointer remap not ported
                            LEGACY_MICROCHUNKID_PLAYER_TYPE              -> { playerType = cload.readInt() }
                            MICROCHUNKID_HIBERNATION_TIMER                -> { hibernationTimer = cload.readFloat() }
                            MICROCHUNKID_HIBERNATION_ENABLE               -> { hibernationEnable = cload.readBool() }
                            MICROCHUNKID_HOST_GAME_OBJ_BONE               -> { hostGameObjBone = cload.readInt() }
                            MICROCHUNKID_RADAR_BLIP_SHAPE_TYPE            -> { radarBlipShapeType = cload.readInt() }
                            MICROCHUNKID_RADAR_BLIP_COLOR_TYPE            -> { radarBlipColorType = cload.readInt() }
                            MICROCHUNKID_RADAR_BLIP_INTENSITY             -> { radarBlipIntensity = cload.readFloat() }
                            // FIXME: MICROCHUNKID_ACTIVE_CONVERSATION — pointer remap not ported
                            MICROCHUNKID_HUD_POKABLE_INDICATOR            -> { hudPokableIndicatorEnabled = cload.readBool() }
                            MICROCHUNKID_IS_INNATE_CONVERSATIONS_ENABLED  -> { isInnateConversationsEnabled = cload.readBool() }
                            else -> error("Unrecognized PhysicalGameObj variable chunk ID: ${cload.curMicroChunkId}")
                        }
                        cload.closeMicroChunk()
                    }
                }
                CHUNKID_ANIM_CONTROL -> {
                    setAnimation(null)
                    animControl!!.load(cload)
                }
                CHUNKID_HOST_GAME_OBJ -> hostGameObj.load(cload)
                else -> error("Unrecognized PhysicalGameObj chunk ID: ${cload.curChunkId}")
            }
            cload.closeChunk()
        }
        // FIXME: REQUEST_REF_COUNTED_POINTER_REMAP for PhysObj and ActiveConversation
        // FIXME: SaveLoadSystemClass::Register_Pointer(phys_observer_ptr, this)
        // FIXME: SaveLoadSystemClass::Register_Post_Load_Callback(this)
        return true
    }

    // Server-port direct fields — derived from PhysObj in C++
    // @JvmName avoids clash with override fun getPosition() / fun getFacing() below
    @get:JvmName("positionField") @set:JvmName("setPositionField")
    var position: Vector3 = Vector3()
    @get:JvmName("facingField") @set:JvmName("setFacingField")
    var facing: Float = 0f
    var modelName: String = ""
    var animName: String = ""

    companion object {
        // C++: #define HIBERNATION_DELAY 30
        const val HIBERNATION_DELAY = 30f

        // C++: physicalgameobj.cpp enum (starting at 910991145)
        internal const val CHUNKID_PHYSICAL_VARIABLES                    = 910991146
        private const val LEGACY_CHUNKID_DEFENSE                        = 910991148
        private const val LEGACY_CHUNKID_PARENT_OLD                     = 910991152
        private const val CHUNKID_ANIM_CONTROL                          = 910991153
        private const val CHUNKID_HOST_GAME_OBJ                         = 910991154
        private const val CHUNKID_PARENT                                 = 910991155

        private const val LEGACY_MICROCHUNKID_PLAYER_TYPE               = 9
        private const val MICROCHUNKID_PHYS_OBSERVER_PTR                = 3
        private const val MICROCHUNKID_PHYSICAL_OBJECT                  = 10
        private const val MICROCHUNKID_HIBERNATION_TIMER                = 11
        private const val MICROCHUNKID_HIBERNATION_ENABLE               = 12
        private const val MICROCHUNKID_HOST_GAME_OBJ_BONE               = 13
        private const val MICROCHUNKID_RADAR_BLIP_SHAPE_TYPE            = 14
        private const val MICROCHUNKID_RADAR_BLIP_COLOR_TYPE            = 15
        private const val MICROCHUNKID_RADAR_BLIP_INTENSITY             = 16
        private const val MICROCHUNKID_ACTIVE_CONVERSATION              = 17
        private const val MICROCHUNKID_HUD_POKABLE_INDICATOR            = 18
        private const val MICROCHUNKID_IS_INNATE_CONVERSATIONS_ENABLED  = 19
    }

    // C++: virtual void Get_Position(Vector3 * set_pos) const
    override fun getPosition(): Vector3 = physObj!!.getPosition()

    // C++: void Set_Position(const Vector3 & pos)
    fun setPosition(pos: Vector3) { physObj!!.setPosition(pos) }

    // C++: float Get_Facing() const
    fun getFacing(): Float = physObj!!.getFacing()

    // C++: const Matrix3D & Get_Transform() const
    fun getTransform(): Matrix3D = physObj!!.getTransform()

    // C++: void Set_Transform(const Matrix3D & tm)
    fun setTransform(tm: Matrix3D) { physObj!!.setTransform(tm) }

    // C++: PhysicalGameObj::Export_Creation — position + facing.
    // definitionId is written by NetworkGameObjectFactory.prepPacket() before Export_Creation is called.
    override fun exportCreation(packet: BitStream) {
        super.exportCreation(packet)
        // C++: definitionId written by factory.Prep_Packet() — not here
        packet.addFloat(position.x, BITPACK_WORLD_POSITION_X)
        packet.addFloat(position.y, BITPACK_WORLD_POSITION_Y)
        packet.addFloat(position.z, BITPACK_WORLD_POSITION_Z)
        packet.addFloat(facing)
    }

    override fun importCreation(packet: BitStream) {
        super.importCreation(packet)
        val pos = Vector3()
        pos.x = packet.getFloat(BITPACK_WORLD_POSITION_X)
        pos.y = packet.getFloat(BITPACK_WORLD_POSITION_Y)
        pos.z = packet.getFloat(BITPACK_WORLD_POSITION_Z)
        val facing = packet.getFloat()
        val tm = Matrix3D.identity().also { it.translate(pos); it.rotateZ(facing) }
        setTransform(tm)
    }

    // C++: PhysicalGameObj::Export_Rare
    override fun exportRare(packet: BitStream) {
        super.exportRare(packet)
        packet.addTerminatedString(physObj?.peekModel()?.getName() ?: modelName, permitEmpty = true)
        packet.addTerminatedString(animName, permitEmpty = true)
        packet.addInt(0)  // currFrame — no real animation system on server
        packet.addInt(0)  // targetFrame
        packet.addInt(0)  // animMode
        packet.addInt(hostGameObj.get()?.networkId ?: 0)
        packet.addInt(hostGameObjBone)
        packet.addInt(playerType)
        packet.addBool(hudPokableIndicatorEnabled)
        if (this is VehicleGameObj) {
            packet.addBool(peekModel()?.isHidden() ?: false)
        }
    }

    // C++: PhysicalGameObj::Import_Rare
    override fun importRare(packet: BitStream) {
        super.importRare(packet)
        val modelName = packet.getTerminatedString(true)
        if (!physObj!!.peekModel()!!.getName().equals(modelName, ignoreCase = true)) {
            physObj!!.setModelByName(modelName)
        }
        val animationName = packet.getTerminatedString(true)
        val currFrame  = packet.getInt()
        val targetFrame = packet.getInt()
        val animMode   = packet.getInt()
        animControl?.let {
            it.setAnimation(animationName, 0, currFrame.toFloat())
            it.setTargetFrame(targetFrame)
            it.setMode(animMode)
        }
        val hostModelId = packet.getInt()
        hostGameObjBone = packet.getInt()
        pendingHostObjId = 0
        if (hostModelId != 0) {
            hostGameObj.set(GameObjManager.findPhysicalGameObj(hostModelId))
            if (hostGameObj.get() == null) {
                pendingHostObjId = hostModelId
                resetHibernating()
            }
        } else {
            hostGameObj.set(null)
        }
        playerType = packet.getInt()
        hudPokableIndicatorEnabled = packet.getBool()
        if (this is VehicleGameObj) {
            val hidden = packet.getBool()
            peekModel()?.setHidden(hidden)
        }
    }

    // C++: PhysicalGameObj::Export_Frequent
    override fun exportFrequent(packet: BitStream) {
        val onHostBone = (hostGameObj.get() as? PhysicalGameObj)?.peekModel() != null
        packet.addBool(onHostBone)
    }

    // C++: PhysicalGameObj::Import_Frequent
    override fun importFrequent(packet: BitStream) {
        val onHostBone = packet.getBool()
        if (onHostBone) {
            physObj?.asMoveablePhysClass()?.setVelocity(Vector3())
        }
    }
}
