#pragma once

#include "global.h"

class StringClass;
class MeshClass;

// Note! For the debugger to be usable, the application must call DX8RendererDebugger::Update() once
// each frame.

class DX8RendererDebugger
{
	static bool Enabled;
public:
	static void Enable(bool enable);
	WWINLINE static bool Is_Enabled() { return Enabled; }
	static void Get_String(StringClass& s);
	static void Update();
	static void Add_Mesh(MeshClass* mesh) {}

	static void Disable_Mesh(unsigned id);
	static void Enable_Mesh(unsigned id);
	static void Disable_All();
	static void Enable_All();
};
