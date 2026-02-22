package ccr.server.level.w3d

import ccr.server.level.Vector3
import ccr.server.mix.ChunkReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class W3dParserTest {

    @Test
    fun `W3dChunkIds are correct`() {
        assertEquals(0x00000000u, W3dChunkIds.MESH)
        assertEquals(0x00000100u, W3dChunkIds.HIERARCHY)
        assertEquals(0x00000200u, W3dChunkIds.ANIMATION)
        assertEquals(0x00000280u, W3dChunkIds.COMPRESSED_ANIMATION)
        assertEquals(0x0000001Fu, W3dChunkIds.MESH_HEADER3)
        assertEquals(0x00000101u, W3dChunkIds.HIERARCHY_HEADER)
        assertEquals(0x00000201u, W3dChunkIds.ANIMATION_HEADER)
    }

    @Test
    fun `parse empty W3D file returns empty collections`() {
        val result = W3dFileParser.parse(ByteArray(0))
        assertTrue(result.meshes.isEmpty())
        assertTrue(result.hierarchies.isEmpty())
        assertTrue(result.animations.isEmpty())
    }

    @Test
    fun `W3dMesh data class works`() {
        val mesh = W3dMesh("test", "container", 0u, 3, 1, emptyList(), emptyList(), emptyList(), emptyList())
        assertEquals("test", mesh.name)
        assertEquals(3, mesh.vertexCount)
        assertEquals(1, mesh.faceCount)
    }

    @Test
    fun `W3dHierarchy data class works`() {
        val pivot = W3dPivot("root", -1, Vector3.ZERO)
        val hierarchy = W3dHierarchy("TestHierarchy", Vector3.ZERO, listOf(pivot))
        assertEquals("TestHierarchy", hierarchy.name)
        assertEquals(1, hierarchy.pivots.size)
        assertEquals("root", hierarchy.pivots[0].name)
        assertEquals(1f, hierarchy.pivots[0].rotW)
    }

    @Test
    fun `W3dAnimation data class works`() {
        val channel = W3dAnimationChannel(0, 10, 1, 0, 0, FloatArray(11))
        val anim = W3dAnimation("TestAnim", "TestHierarchy", 11, 30, listOf(channel))
        assertEquals("TestAnim", anim.name)
        assertEquals(30, anim.frameRate)
        assertEquals(1, anim.channels.size)
    }

    @Test
    fun `W3dAnimationChannel equals and hashCode`() {
        val data1 = floatArrayOf(1f, 2f, 3f)
        val data2 = floatArrayOf(1f, 2f, 3f)
        val ch1 = W3dAnimationChannel(0, 2, 1, 0, 5, data1)
        val ch2 = W3dAnimationChannel(0, 2, 1, 0, 5, data2)
        assertEquals(ch1, ch2)
        assertEquals(ch1.hashCode(), ch2.hashCode())
    }

    @Test
    fun `parse mesh with header and vertices`() {
        // Build a minimal W3D file with a MESH chunk containing MESH_HEADER3 and VERTICES
        val header3 = buildMeshHeader3("TestMesh", "Container", numTris = 0, numVertices = 2)
        val vertexData = buildVertexData(listOf(Vector3(1f, 2f, 3f), Vector3(4f, 5f, 6f)))

        val meshData = buildChunk(W3dChunkIds.MESH_HEADER3, header3) +
            buildChunk(W3dChunkIds.VERTICES, vertexData)
        val w3dData = buildChunk(W3dChunkIds.MESH, meshData, isContainer = true)

        val result = W3dFileParser.parse(w3dData)
        assertEquals(1, result.meshes.size)
        val mesh = result.meshes[0]
        assertEquals("TestMesh", mesh.name)
        assertEquals("Container", mesh.containerName)
        assertEquals(2, mesh.vertexCount)
        assertEquals(2, mesh.vertices.size)
        assertEquals(1f, mesh.vertices[0].x)
        assertEquals(5f, mesh.vertices[1].y)
    }

    @Test
    fun `parse hierarchy with header and pivots`() {
        val headerData = buildHierarchyHeader("TestHier", numPivots = 1, cx = 10f, cy = 20f, cz = 30f)
        val pivotData = buildPivotData("RootBone", parentIdx = -1, tx = 1f, ty = 2f, tz = 3f)

        val hierData = buildChunk(W3dChunkIds.HIERARCHY_HEADER, headerData) +
            buildChunk(W3dChunkIds.PIVOTS, pivotData)
        val w3dData = buildChunk(W3dChunkIds.HIERARCHY, hierData, isContainer = true)

        val result = W3dFileParser.parse(w3dData)
        assertEquals(1, result.hierarchies.size)
        val hier = result.hierarchies[0]
        assertEquals("TestHier", hier.name)
        assertEquals(10f, hier.centerPos.x)
        assertEquals(1, hier.pivots.size)
        assertEquals("RootBone", hier.pivots[0].name)
        assertEquals(1f, hier.pivots[0].translation.x)
    }

    @Test
    fun `parse animation with header and channel`() {
        val headerData = buildAnimHeader("TestAnim", "TestHier", numFrames = 3, frameRate = 15)
        val channelData = buildAnimChannel(firstFrame = 0, lastFrame = 2, vectorLen = 1, pivotIdx = 0, floats = floatArrayOf(1f, 2f, 3f))

        val animData = buildChunk(W3dChunkIds.ANIMATION_HEADER, headerData) +
            buildChunk(W3dChunkIds.ANIMATION_CHANNEL, channelData)
        val w3dData = buildChunk(W3dChunkIds.ANIMATION, animData, isContainer = true)

        val result = W3dFileParser.parse(w3dData)
        assertEquals(1, result.animations.size)
        val anim = result.animations[0]
        assertEquals("TestAnim", anim.name)
        assertEquals("TestHier", anim.hierarchyName)
        assertEquals(3, anim.numFrames)
        assertEquals(15, anim.frameRate)
        assertEquals(1, anim.channels.size)
        assertEquals(3, anim.channels[0].data.size)
        assertEquals(2f, anim.channels[0].data[1])
    }

    // --- Helper functions to build binary test data ---

    private fun buildChunk(id: UInt, data: ByteArray, isContainer: Boolean = false): ByteArray {
        val buf = ByteBuffer.allocate(8 + data.size).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(id.toInt())
        val sizeField = data.size or (if (isContainer) 0x80000000.toInt() else 0)
        buf.putInt(sizeField)
        buf.put(data)
        return buf.array()
    }

    private fun buildMeshHeader3(meshName: String, containerName: String, numTris: Int, numVertices: Int): ByteArray {
        // 120 bytes total for W3dMeshHeader3Struct
        val buf = ByteBuffer.allocate(120).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(0) // version
        buf.putInt(0) // attributes
        writeFixedString(buf, meshName, 16) // offset 8
        writeFixedString(buf, containerName, 16) // offset 24
        buf.putInt(numTris) // offset 40
        buf.putInt(numVertices) // offset 44
        // Remaining fields are zero-filled
        return buf.array()
    }

    private fun buildVertexData(vertices: List<Vector3>): ByteArray {
        val buf = ByteBuffer.allocate(vertices.size * 12).order(ByteOrder.LITTLE_ENDIAN)
        for (v in vertices) {
            buf.putFloat(v.x)
            buf.putFloat(v.y)
            buf.putFloat(v.z)
        }
        return buf.array()
    }

    private fun buildHierarchyHeader(name: String, numPivots: Int, cx: Float = 0f, cy: Float = 0f, cz: Float = 0f): ByteArray {
        // 36 bytes: version(4) + name(16) + numPivots(4) + center(12)
        val buf = ByteBuffer.allocate(36).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(0) // version
        writeFixedString(buf, name, 16)
        buf.putInt(numPivots)
        buf.putFloat(cx)
        buf.putFloat(cy)
        buf.putFloat(cz)
        return buf.array()
    }

    private fun buildPivotData(name: String, parentIdx: Int, tx: Float = 0f, ty: Float = 0f, tz: Float = 0f): ByteArray {
        // 60 bytes per pivot: name(16) + parentIdx(4) + translation(12) + eulerAngles(12) + rotation(16)
        val buf = ByteBuffer.allocate(60).order(ByteOrder.LITTLE_ENDIAN)
        writeFixedString(buf, name, 16)
        buf.putInt(parentIdx)
        buf.putFloat(tx); buf.putFloat(ty); buf.putFloat(tz) // translation
        buf.putFloat(0f); buf.putFloat(0f); buf.putFloat(0f) // euler angles
        buf.putFloat(0f); buf.putFloat(0f); buf.putFloat(0f); buf.putFloat(1f) // quaternion (identity)
        return buf.array()
    }

    private fun buildAnimHeader(name: String, hierName: String, numFrames: Int, frameRate: Int): ByteArray {
        // 44 bytes: version(4) + name(16) + hierarchyName(16) + numFrames(4) + frameRate(4)
        val buf = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(0) // version
        writeFixedString(buf, name, 16)
        writeFixedString(buf, hierName, 16)
        buf.putInt(numFrames)
        buf.putInt(frameRate)
        return buf.array()
    }

    private fun buildAnimChannel(firstFrame: Int, lastFrame: Int, vectorLen: Int, pivotIdx: Int, floats: FloatArray): ByteArray {
        // 12 byte header + N*4 bytes data
        val buf = ByteBuffer.allocate(12 + floats.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(firstFrame.toShort())
        buf.putShort(lastFrame.toShort())
        buf.putShort(vectorLen.toShort())
        buf.putShort(0) // flags
        buf.putShort(pivotIdx.toShort())
        buf.putShort(0) // pad
        for (f in floats) buf.putFloat(f)
        return buf.array()
    }

    private fun writeFixedString(buf: ByteBuffer, str: String, len: Int) {
        val bytes = str.toByteArray(Charsets.ISO_8859_1)
        buf.put(bytes, 0, minOf(bytes.size, len))
        repeat(len - minOf(bytes.size, len)) { buf.put(0) }
    }
}
