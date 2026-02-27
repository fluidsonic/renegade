package ccr.server.net

import ccr.net.bitstream.BitStream

// C++: WeaponBagClass (weaponbag.h / weaponbag.cpp)
// owner is PhysicalGameObj (not ArmedGameObj) to allow non-armed owners like PowerUpGameObj
class WeaponBagClass(val owner: PhysicalGameObj) {

    // C++: DynamicVectorClass<WeaponClass*> WeaponList
    private val weaponList: MutableList<WeaponClass> = mutableListOf()

    // C++: int WeaponIndex
    var weaponIndex: Int = 0
        private set

    // C++: bool IsChanged
    var isChanged: Boolean = false
        private set

    // C++: bool HUDIsChanged
    var hudIsChanged: Boolean = false
        private set

    // C++: int Get_Count()
    fun getCount(): Int = weaponList.size

    // C++: WeaponClass* Peek_Weapon(int index)
    fun peekWeapon(index: Int): WeaponClass = weaponList[index]

    // C++: WeaponClass* Get_Weapon()
    fun getWeapon(): WeaponClass? = weaponList.getOrNull(weaponIndex)

    // C++: int Get_Index()
    fun getIndex(): Int = weaponIndex

    // C++: void Select_Index(int index)
    fun selectIndex(index: Int) {
        weaponIndex = index.coerceIn(0, (weaponList.size - 1).coerceAtLeast(0))
        isChanged = true
        hudIsChanged = true
    }

    // C++: WeaponClass* Add_Weapon(int id, int rounds, bool give_weapon)
    fun addWeapon(id: Int, rounds: Int = 0, giveWeapon: Boolean = true): WeaponClass {
        val weapon = WeaponClass(definitionId = id, totalRounds = rounds)
        weapon.owner = owner
        weaponList.add(weapon)
        if (giveWeapon) isChanged = true
        return weapon
    }

    // C++: WeaponClass* Add_Weapon(const WeaponDefinitionClass*, int rounds, bool give_weapon)
    fun addWeapon(weapon: WeaponClass, giveWeapon: Boolean = true): WeaponClass {
        weapon.owner = owner
        weaponList.add(weapon)
        if (giveWeapon) isChanged = true
        return weapon
    }

    // C++: void Remove_Weapon(int index)
    fun removeWeapon(index: Int) {
        weaponList.removeAt(index)
        if (weaponIndex >= weaponList.size) weaponIndex = (weaponList.size - 1).coerceAtLeast(0)
        isChanged = true
    }

    // C++: void Clear_Weapons()
    fun clearWeapons() {
        weaponList.clear()
        weaponIndex = 0
        isChanged = true
    }

    // C++: bool Is_Weapon_Owned(int weapon_id)
    fun isWeaponOwned(weaponId: Int): Boolean = weaponList.any { it.definitionId == weaponId }

    // C++: bool Is_Ammo_Full(int weapon_id) — true if the weapon exists and is at max ammo
    // FIXME: full check requires WeaponDefinitionClass for max-rounds cap; stub returns false (never full)
    fun isAmmoFull(weaponId: Int): Boolean = false

    // C++: void Select_Weapon(int weapon_id) — select by definition ID
    fun selectWeaponId(weaponId: Int) {
        val index = weaponList.indexOfFirst { it.definitionId == weaponId }
        if (index >= 0) selectIndex(index)
    }

    // C++: void Deselect() — deselect current weapon (set index to 0)
    fun deselect() {
        weaponIndex = 0
        isChanged = true
        hudIsChanged = true
    }

    // C++: bool Move_Contents(WeaponBagClass*) — move all weapons from src into this bag
    fun moveContents(src: WeaponBagClass?): Boolean {
        if (src == null) return false
        var moved = false
        for (w in src.weaponList.toList()) {
            addWeapon(w, true)
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
    fun exportWeaponList(packet: BitStream) {
        // C++: writes (Count()-1) then for each weapon from slot 1: id(32) + totalRounds(32)
        packet.addInt(weaponList.size - 1)
        for (i in 1 until weaponList.size) {
            packet.addInt(weaponList[i].definitionId)
            packet.addInt(weaponList[i].totalRounds)
        }
    }

    // C++: void Import_Weapon_List(BitStreamClass& packet)
    fun importWeaponList(packet: BitStream) {
        val count = packet.getInt()
        for (i in 0 until count) {
            val defId = packet.getInt()
            val rounds = packet.getInt()
            val existing = weaponList.find { it.definitionId == defId }
            if (existing != null) existing.totalRounds = rounds
        }
    }

    // C++: bool Save(ChunkSaveClass&) — stub
    fun save(csave: ChunkSaveClass): Boolean = TODO("stub")

    // C++: bool Load(ChunkLoadClass&) — stub
    fun load(cload: ChunkLoadClass): Boolean = TODO("stub")
}
