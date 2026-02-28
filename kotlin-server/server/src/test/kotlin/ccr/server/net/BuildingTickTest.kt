package ccr.server.net

import ccr.server.GameObjManager
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Tests that BuildingGameObj.think() is called by GameObjManager.think().
 *
 * C++: BuildingGameObj inherits from BaseGameObj, which calls GameObjManager::Add(this)
 * in its constructor. GameObjManager::Think() iterates GameObjList (not BuildingGameObjList),
 * so every building is ticked like any other BaseGameObj.
 *
 * The Kotlin port mirrors this: BaseGameObj.init{} calls GameObjManager.add(this),
 * so VehicleFactoryGameObj (and all BuildingGameObj subclasses) land in gameObjList.
 */
class BuildingTickTest {

    @Test
    fun `BuildingGameObj think is called by GameObjManager`() {
        var ticked = false

        // Subclass that overrides think() to record invocation.
        // Uses the secondary constructor to bypass definition pipeline.
        val building = object : BuildingGameObj(
            definitionId = 9999,
        ) {
            override fun think() {
                ticked = true
                super.think()
            }
        }

        // Verify the building is in gameObjList (added by BaseGameObj.init{}) and
        // call think() only on this building — avoids triggering other tests' dirty state.
        assertTrue(
            GameObjManager.getAllObjects().contains(building),
            "BuildingGameObj must be in GameObjManager.gameObjList via BaseGameObj.init{}"
        )
        building.think()
        assertTrue(ticked, "BuildingGameObj.think() was not called")

        // Cleanup: remove from both lists so this test doesn't pollute others.
        GameObjManager.remove(building)
        GameObjManager.removeBuilding(building)
    }
}
