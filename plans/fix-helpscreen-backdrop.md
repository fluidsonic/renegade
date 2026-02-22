# Fix: Help Screen Backdrop Only Renders Right Portion

## Context

The Help Screen's 3D backdrop model ("IF_HELPLOAD") only renders on the right ~40-60% of the screen in the macOS port, with the left portion being completely black. Other backdrops (e.g. main menu) render correctly. The 2D UI elements (text, buttons) render fine across the full screen — only the 3D model is affected.

## Root Cause

The W3D rendering pipeline has two levels of frustum culling:

1. **Scene-level** (`scene.cpp`): Tests the top-level HLod's bounding sphere. This PASSES (otherwise nothing would render at all). Honors `Is_Force_Visible()`.
2. **Per-mesh level** (`mesh.cpp`): Each sub-mesh of the HLod is individually tested against the camera frustum using its axis-aligned bounding box (AABox). Sub-meshes covering the left portion of the screen were being incorrectly culled here because `Is_Force_Visible()` was NOT checked.

## Fix Applied

### `original/Code/ww3d2/mesh.cpp` — `MeshClass::Render()`

Added `Is_Force_Visible()` as the first condition in the per-mesh frustum test:

```cpp
const bool _in_frustum = Is_Force_Visible() ||
        Model->Get_Flag(MeshGeometryClass::SKIN) ||
        CollisionMath::Overlap_Test(frustum,Get_Bounding_Box())!=CollisionMath::OUTSIDE;
```

This ensures force-visible meshes skip the frustum cull, matching the behavior at the scene level.

### `original/Code/wwui/menubackdrop.cpp` — `MenuBackDropClass::Set_Model()`

After adding the model to the scene, set `Force_Visible(true)` on the model AND on all its sub-objects (since Force_Visible is a per-instance flag and doesn't auto-propagate through the HLod hierarchy):

```cpp
Scene->Add_Render_Object (Model);
Model->Set_Force_Visible(true);
for (int32_t i = 0; i < Model->Get_Num_Sub_Objects(); i++) {
    RenderObjClass* sub = Model->Get_Sub_Object(i);
    if (sub != nullptr) {
        sub->Set_Force_Visible(true);
        sub->Release_Ref();
    }
}
```

Note: `Get_Sub_Object()` calls `Add_Ref()` on the returned pointer, so `Release_Ref()` is required.

Also removed all temporary diagnostic logging that had accumulated in `mesh.cpp`, `scene.cpp`, `hlod.cpp`, and `menubackdrop.cpp`.

## Verification

1. Build: `cmake --build original/build --target Commando -j8`
2. Run: `MallocNanoZone=0 original/build/Code/Commando/Commando.app/Contents/MacOS/Commando`
3. Navigate to Help Screen — full green tech background should render across the entire screen (no black left portion)
4. Other backdrops (main menu) should still render correctly
