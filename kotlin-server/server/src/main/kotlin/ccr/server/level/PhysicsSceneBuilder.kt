package ccr.server.level

import ccr.math.Matrix3D as MathMatrix3D
import ccr.math.Triangle
import ccr.math.Vector3 as MathVector3
import ccr.physics.scene.PhysicsScene
import ccr.physics.static.StaticPhysClass
import ccr.server.level.lsd.StaticPhysObject
import ccr.server.level.w3d.W3dChunkIds
import ccr.server.level.w3d.W3dFileParser
import ccr.server.mix.MixReader

object PhysicsSceneBuilder {

    fun build(
        staticObjects: List<StaticPhysObject>,
        mapMix: MixReader,
        alwaysMix: MixReader?,
    ): PhysicsScene {
        val scene = PhysicsScene()

        // Collect unique model names and parse each W3D file once
        val modelNames = staticObjects.map { it.modelName }.toSet()
        val w3dCache = mutableMapOf<String, ccr.server.level.w3d.W3dFile>()
        for (name in modelNames) {
            val data = mapMix.readFile("$name.w3d") ?: alwaysMix?.readFile("$name.w3d") ?: continue
            w3dCache[name] = W3dFileParser.parse(data)
        }

        var objectCount = 0
        var triangleCount = 0

        for (obj in staticObjects) {
            val w3d = w3dCache[obj.modelName] ?: continue
            val worldTransform = toMathMatrix3D(obj.transform)

            val collisionMeshes = w3d.meshes.filter { mesh ->
                (mesh.attributes and W3dChunkIds.W3D_MESH_FLAG_COLLISION_TYPE_PHYSICAL) != 0u
            }
            if (collisionMeshes.isEmpty()) continue

            val allTriangles = mutableListOf<Triangle>()
            for (mesh in collisionMeshes) {
                for (tri in mesh.triangles) {
                    if (tri.i0 >= mesh.vertices.size || tri.i1 >= mesh.vertices.size || tri.i2 >= mesh.vertices.size) continue
                    val v0 = worldTransform.transformPoint(toMathVec3(mesh.vertices[tri.i0]))
                    val v1 = worldTransform.transformPoint(toMathVec3(mesh.vertices[tri.i1]))
                    val v2 = worldTransform.transformPoint(toMathVec3(mesh.vertices[tri.i2]))
                    allTriangles.add(Triangle(v0, v1, v2))
                }
            }
            if (allTriangles.isEmpty()) continue

            val physObj = StaticPhysClass()
            physObj.triangles = allTriangles
            physObj.transform = worldTransform
            scene.addStaticObject(physObj)

            objectCount++
            triangleCount += allTriangles.size
        }

        println("[LEVEL] Loaded $objectCount static collision objects, $triangleCount triangles")
        return scene
    }

    private fun toMathMatrix3D(m: Matrix3D): MathMatrix3D {
        val e = m.elements
        return MathMatrix3D(
            m00 = e[0], m01 = e[1], m02 = e[2], m03 = e[3],
            m10 = e[4], m11 = e[5], m12 = e[6], m13 = e[7],
            m20 = e[8], m21 = e[9], m22 = e[10], m23 = e[11],
        )
    }

    private fun toMathVec3(v: Vector3): MathVector3 = MathVector3(v.x, v.y, v.z)
}
