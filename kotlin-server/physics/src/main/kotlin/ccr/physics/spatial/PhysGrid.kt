package ccr.physics.spatial

import ccr.math.AABox
import ccr.math.Vector3
import ccr.physics.PhysClass
import ccr.physics.collision.AABoxCollisionTest
import ccr.physics.collision.RayCollisionTest
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class PhysGrid {
    private var worldMin = Vector3(-100f, -100f, -100f)
    private var worldMax = Vector3(100f, 100f, 100f)
    private var cellSize = 10f
    private var cellsX = 0
    private var cellsY = 0
    private var cellsZ = 0
    private val cells = HashMap<Int, MutableList<PhysClass>>()
    private val objectCells = HashMap<PhysClass, List<Int>>()

    fun rePartition(worldMin: Vector3, worldMax: Vector3, cellSize: Float) {
        this.worldMin = worldMin
        this.worldMax = worldMax
        this.cellSize = cellSize
        cellsX = max(1, ((worldMax.x - worldMin.x) / cellSize).toInt() + 1)
        cellsY = max(1, ((worldMax.y - worldMin.y) / cellSize).toInt() + 1)
        cellsZ = max(1, ((worldMax.z - worldMin.z) / cellSize).toInt() + 1)
        cells.clear()
        objectCells.clear()
    }

    private fun cellIndex(cx: Int, cy: Int, cz: Int) = cx + cy * cellsX + cz * cellsX * cellsY

    private fun getCellsForBounds(mn: Vector3, mx: Vector3): List<Int> {
        val x0 = max(0, floor((mn.x - worldMin.x) / cellSize).toInt())
        val x1 = min(cellsX - 1, floor((mx.x - worldMin.x) / cellSize).toInt())
        val y0 = max(0, floor((mn.y - worldMin.y) / cellSize).toInt())
        val y1 = min(cellsY - 1, floor((mx.y - worldMin.y) / cellSize).toInt())
        val z0 = max(0, floor((mn.z - worldMin.z) / cellSize).toInt())
        val z1 = min(cellsZ - 1, floor((mx.z - worldMin.z) / cellSize).toInt())
        val result = mutableListOf<Int>()
        for (z in z0..z1) for (y in y0..y1) for (x in x0..x1) result.add(cellIndex(x, y, z))
        return result
    }

    private fun getBoundsForObj(obj: PhysClass): Pair<Vector3, Vector3> {
        val t = obj.transform.translation
        val pad = Vector3(1f, 1f, 1f)
        return Pair(t - pad, t + pad)
    }

    fun addObject(obj: PhysClass) {
        val (mn, mx) = getBoundsForObj(obj)
        val indices = getCellsForBounds(mn, mx)
        objectCells[obj] = indices
        for (idx in indices) cells.getOrPut(idx) { mutableListOf() }.add(obj)
    }

    fun removeObject(obj: PhysClass) {
        objectCells[obj]?.forEach { idx -> cells[idx]?.remove(obj) }
        objectCells.remove(obj)
    }

    fun updateObject(obj: PhysClass) {
        removeObject(obj)
        addObject(obj)
    }

    fun castRay(test: RayCollisionTest): Boolean {
        var hit = false
        val mn = test.ray.p0
        val mx = test.ray.p1
        val searchMin = Vector3(min(mn.x, mx.x), min(mn.y, mx.y), min(mn.z, mx.z))
        val searchMax = Vector3(max(mn.x, mx.x), max(mn.y, mx.y), max(mn.z, mx.z))
        val checked = HashSet<PhysClass>()
        for (idx in getCellsForBounds(searchMin, searchMax)) {
            cells[idx]?.forEach { obj ->
                if (checked.add(obj) && obj.castRay(test)) hit = true
            }
        }
        return hit
    }

    fun castAABox(test: AABoxCollisionTest): Boolean {
        var hit = false
        val checked = HashSet<PhysClass>()
        for (idx in getCellsForBounds(test.sweepMin, test.sweepMax)) {
            cells[idx]?.forEach { obj ->
                if (checked.add(obj) && obj.castAABox(test)) hit = true
            }
        }
        return hit
    }

    fun collectObjects(box: AABox): List<PhysClass> {
        val result = HashSet<PhysClass>()
        for (idx in getCellsForBounds(box.min, box.max)) {
            cells[idx]?.forEach { result.add(it) }
        }
        return result.toList()
    }
}
