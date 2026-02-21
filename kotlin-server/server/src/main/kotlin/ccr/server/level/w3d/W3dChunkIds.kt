package ccr.server.level.w3d

object W3dChunkIds {
    // Top-level chunk types (w3d_file.h)
    const val MESH                  = 0x00000000u
    const val HIERARCHY             = 0x00000100u
    const val ANIMATION             = 0x00000200u
    const val COMPRESSED_ANIMATION  = 0x00000280u
    const val HLOD                  = 0x00000700u
    const val BOX                   = 0x00000740u
    const val COLLECTION            = 0x00000420u
    const val LIGHT                 = 0x00000460u
    const val EMITTER               = 0x00000500u

    // Mesh sub-chunks
    const val MESH_HEADER3          = 0x0000001Fu
    const val VERTICES              = 0x00000002u
    const val VERTEX_NORMALS        = 0x00000003u
    const val TRIANGLES             = 0x00000020u
    const val TEXTURES              = 0x00000030u
    const val TEXTURE               = 0x00000031u
    const val TEXTURE_NAME          = 0x00000032u
    const val MATERIAL_INFO         = 0x00000028u

    // Hierarchy sub-chunks
    const val HIERARCHY_HEADER      = 0x00000101u
    const val PIVOTS                = 0x00000102u
    const val PIVOT_FIXUPS          = 0x00000103u

    // Animation sub-chunks
    const val ANIMATION_HEADER      = 0x00000201u
    const val ANIMATION_CHANNEL     = 0x00000202u
    const val BIT_CHANNEL           = 0x00000203u

    // Compressed animation sub-chunks
    const val COMPRESSED_ANIMATION_HEADER = 0x00000281u
    const val COMPRESSED_ANIMATION_CHANNEL = 0x00000282u
}
