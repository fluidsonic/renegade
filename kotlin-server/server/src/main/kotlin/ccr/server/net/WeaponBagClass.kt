package ccr.server.net

import ccr.net.bitstream.BitStream
import ccr.net.replication.NetworkObject
import ccr.server.defs.WeaponDefinitionClass
import ccr.server.level.DefinitionRegistry

// C++: WeaponBagClass (weaponbag.h / weaponbag.cpp)
// owner is PhysicalGameObj (not ArmedGameObj) to allow non-armed owners like PowerUpGameObj
class WeaponBagClass(val owner: PhysicalGameObj) {

    // C++: DynamicVectorClass<WeaponClass*> WeaponList
    // Index 0 is always NULL (no-weapon sentinel), matching C++: WeaponList.Add(NULL) in constructor
    private val weaponList: MutableList<WeaponClass?> = mutableListOf(null)

    // C++: int WeaponIndex
    var weaponIndex: Int = 0
        private set

    // C++: bool IsChanged
    var isChanged: Boolean = false
        private set

    // C++: bool HUDIsChanged
    var hudIsChanged: Boolean = false
        private set

    // DefinitionRegistry used to resolve weapon IDs to definitions
    var definitions: DefinitionRegistry? = null

    // C++: int Get_Count() — includes NULL at index 0
    fun getCount(): Int = weaponList.size

    // C++: WeaponClass* Peek_Weapon(int index) — returns null for index 0 (no-weapon sentinel)
    fun peekWeapon(index: Int): WeaponClass? = weaponList[index]

    // C++: WeaponClass* Get_Weapon()
    fun getWeapon(): WeaponClass? = weaponList.getOrNull(weaponIndex)

    // C++: int Get_Index()
    fun getIndex(): Int = weaponIndex

    // C++: void Select_Index(int index)
    fun selectIndex(index: Int) {
        val oldWeapon = weaponList.getOrNull(weaponIndex)
        weaponIndex = index.coerceIn(0, (weaponList.size - 1).coerceAtLeast(0))
        oldWeapon?.deselect()
        weaponList.getOrNull(weaponIndex)?.select()
        isChanged = true
        hudIsChanged = true
    }

    // C++: WeaponClass* Add_Weapon(const WeaponDefinitionClass*, int rounds, bool give_weapon)
    // Primary overload — resolves or creates weapon by definition
    fun addWeapon(def: WeaponDefinitionClass?, rounds: Int = 0, giveWeapon: Boolean = true): WeaponClass? {
        if (def == null) return null

        val found = findWeapon(def)
        if (found != null) {
            if (giveWeapon) {
                found.addRounds(rounds)
                found.weaponExists = true
            } else {
                found.addRounds(rounds)
            }
            markOwnerDirty()
            return found
        }

        val weapon = WeaponClass(def)
        weapon.init(definitions)
        weapon.owner = owner as? ArmedGameObj
        weapon.addRounds(rounds)

        // Insert sorted by keyNumber — insert before the first weapon with a higher keyNumber, or append
        val insertIndex = (1 until weaponList.size).firstOrNull { i ->
            val w = weaponList[i]
            w != null && w.getKeyNumber() > def.keyNumber
        }
        if (insertIndex != null) {
            weaponList.add(insertIndex, weapon)
            if (giveWeapon && weaponIndex >= insertIndex) {
                // C++: WeaponIndex++ when inserted before current selection
                weaponIndex++
            }
        } else {
            weaponList.add(weapon)
        }

        if (giveWeapon) isChanged = true
        markOwnerDirty()
        return weapon
    }

    // C++: WeaponClass* Add_Weapon(int id, int rounds, bool give_weapon)
    // Int overload — resolves definition from registry then delegates to primary overload
    fun addWeapon(id: Int, rounds: Int = 0, giveWeapon: Boolean = true): WeaponClass? {
        val def = definitions?.findById(id.toUInt()) as? WeaponDefinitionClass
        return addWeapon(def, rounds, giveWeapon)
    }

    // C++: WeaponClass* Find_Weapon(const WeaponDefinitionClass*) — find by definition ID
    fun findWeapon(def: WeaponDefinitionClass?): WeaponClass? =
        weaponList.firstOrNull { it != null && it.getId() == def?.id?.toInt() }

    // Find weapon by raw integer definition ID
    private fun findWeaponById(id: Int): WeaponClass? =
        weaponList.firstOrNull { it != null && it.getId() == id }

    // Mark the owner network object dirty at OCCASIONAL level
    fun markOwnerDirty() {
        (owner as? NetworkObject)?.setObjectDirtyBit(NetworkObject.BIT_OCCASIONAL, true)
    }

    // C++: void Remove_Weapon(int index) — index 0 (null sentinel) cannot be removed
    fun removeWeapon(index: Int) {
        require(index > 0) { "Cannot remove index 0 (null sentinel)" }
        weaponList.removeAt(index)
        if (weaponIndex >= weaponList.size) weaponIndex = (weaponList.size - 1).coerceAtLeast(0)
        isChanged = true
    }

    // C++: void Clear_Weapons() — removes all real weapons, re-adds NULL at index 0
    fun clearWeapons() {
        weaponList.clear()
        weaponList.add(null)  // C++: WeaponList.Add(NULL) — restore null sentinel at index 0
        weaponIndex = 0
        isChanged = true
    }

    // C++: WeaponClass* Get_Next_Weapon() — find the next existing weapon after the current selection
    fun getNextWeapon(): WeaponClass? {
        for (i in 0 until weaponList.size) {
            val index = (weaponIndex + i + 1) % weaponList.size
            val w = weaponList[index]
            if (w != null && w.doesWeaponExist()) return w
        }
        return null
    }

    // C++: bool Is_Weapon_Owned(int weapon_id)
    fun isWeaponOwned(weaponId: Int): Boolean = weaponList.any { it != null && it.getId() == weaponId }

    // C++: bool Is_Ammo_Full(int weapon_id) — true if the weapon exists and is at max ammo
    fun isAmmoFull(weaponId: Int): Boolean {
        return weaponList.firstOrNull { it != null && it.getId() == weaponId }?.isAmmoMaxed() ?: false
    }

    // C++: void Select_Weapon(int weapon_id) — select by definition ID
    fun selectWeaponId(weaponId: Int) {
        val index = weaponList.indexOfFirst { it != null && it.getId() == weaponId }
        if (index >= 0) selectIndex(index)
    }

    // C++: void Deselect() — deselect current weapon (set index to 0)
    fun deselect() {
        weaponIndex = 0
        isChanged = true
        hudIsChanged = true
    }

    // C++: bool Move_Contents(WeaponBagClass*) — move all real weapons (index 1+) from src into this bag
    fun moveContents(src: WeaponBagClass?): Boolean {
        if (src == null) return false
        var moved = false
        for (i in 1 until src.weaponList.size) {
            val w = src.weaponList[i] ?: continue
            addWeapon(w.definition, w.getTotalRounds(), true)
            moved = true
        }
        src.clearWeapons()
        return moved
    }

    // C++: WeaponBagClass* Get_Weapon_Bag() — returns self (for ArmedGameObj interface compatibility)
    fun getWeaponBag(): WeaponBagClass = this

    // C++: void Force_Changed()
    fun forceChanged() { isChanged = true }

    // C++: void Reset_Changed()
    fun resetChanged() { isChanged = false }

    // C++: void HUD_Reset_Changed()
    fun hudResetChanged() { hudIsChanged = false }

    // C++: void Export_Weapon_List(BitStreamClass& packet)
    // Writes (Count()-1) weapons (real weapons only, skipping null at index 0)
    fun exportWeaponList(packet: BitStream) {
        packet.addInt(weaponList.size - 1)  // C++: writes Count()-1
        for (i in 1 until weaponList.size) {
            val w = weaponList[i]!!  // index 1+ is always a real WeaponClass
            packet.addInt(w.getId())
            packet.addInt(w.getTotalRounds())
        }
    }

    // C++: void Import_Weapon_List(BitStreamClass& packet)
    fun importWeaponList(packet: BitStream) {
        val count = packet.getInt()
        for (i in 0 until count) {
            val defId = packet.getInt()
            val rounds = packet.getInt()
            addWeapon(defId, 0, false)  // ensure weapon exists (no-op if already owned)
            findWeaponById(defId)?.setTotalRounds(rounds)
        }
    }

    // C++: bool Save(ChunkSaveClass&) — stub
    fun save(csave: ChunkSaveClass): Boolean = TODO("stub")

    // C++: bool Load(ChunkLoadClass&) — stub
    fun load(cload: ChunkLoadClass): Boolean = TODO("stub")
}
