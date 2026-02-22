#pragma once

#include "global.h"

class ScriptFactory;
class ScriptImpClass;

class ScriptRegistrar
	{
	public:
		// Add ScriptFactory with the registrar
		static void RegisterScript(ScriptFactory* factory);

		// Remove ScriptFactory from the registrar
		static void UnregisterScript(ScriptFactory* factory);
		
		// Create an instance of a script
		static ScriptImpClass* CreateScript(const char* name);

		// Get ScriptFactory with the specified name
		static ScriptFactory* GetScriptFactory(const char* name);

		// Get the ScriptFactory at the specified index
		static ScriptFactory* GetScriptFactory(int index);

		// Retrieve the number of registered ScriptFactories
		static int Count(void);

	private:
		static ScriptFactory* mScriptFactories;
	};
