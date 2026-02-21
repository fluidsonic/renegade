#include "always.h"
#include "scriptregistrar.h"
#include "scriptfactory.h"
#include "dprint.h"
#include <string.h>
#include <assert.h>

// ScriptFactory list
ScriptFactory* ScriptRegistrar::mScriptFactories = NULL;

/******************************************************************************
*
* NAME
*     ScriptRegistrar::RegisterScript
*
* DESCRIPTION
*     Register a Script factory
*
* INPUTS
*     ScriptFactory* factory
*
* RESULTS
*     NONE
*
******************************************************************************/

void ScriptRegistrar::RegisterScript(ScriptFactory* factory)
{
	if (factory != NULL) {
//		DebugPrint("Registering Script '%s'\n", factory->GetName());

		factory->SetNext(mScriptFactories);
		mScriptFactories = factory;
	}
}

/******************************************************************************
*
* NAME
*     ScriptRegistrar::UnregisterScript
*
* DESCRIPTION
*     Remove a Script factory from the registery
*
* INPUTS
*     Factory - ScriptFactory to remove.
*
* RESULTS
*     NONE
*
******************************************************************************/

void ScriptRegistrar::UnregisterScript(ScriptFactory* factory)
{
	ScriptFactory* previous = NULL;
	ScriptFactory* current = mScriptFactories;

	while (current != NULL) {
		ScriptFactory* next = current->GetNext();

		if (current == factory) {
			// Handle head of list condition
			if (previous == NULL) {
				mScriptFactories = next;
			} else {
				previous->SetNext(next);
			}

//			DebugPrint("Unregistered script '%s'\n", factory->GetName());
		}

		// Advance to next node
		previous = current;
		current = next;
	}
}

/******************************************************************************
*
* NAME
*     ScriptRegistrar::CreateScript
*
* DESCRIPTION
*     Create an instance of the specified script.
*
* INPUTS
*     ScriptName - Name of script to create.
*
* RESULTS
*     ScriptClass - New script instance.
*
******************************************************************************/

ScriptImpClass* ScriptRegistrar::CreateScript(const char* scriptName)
{
	assert(scriptName != NULL);

	if (scriptName != NULL) {
		ScriptFactory* factory = mScriptFactories;

		while (factory != NULL) {
			if (stricmp(factory->GetName(), scriptName) == 0) {
//				DebugPrint("Creating Script '%s'\n", factory->GetName());
				return factory->Create();
			}

			factory = factory->GetNext();
		}
	}

	DebugPrint("Failed to find script '%s'\n", scriptName);
	return NULL;
}

/******************************************************************************
*
* NAME
*     ScriptRegistrar::GetScriptFactory
*
* DESCRIPTION
*     Get script factory by name.
*
* INPUTS
*     Name - Name of script
*
* RESULTS
*     ScriptFactory - Factory for specified script.
*
******************************************************************************/

ScriptFactory* ScriptRegistrar::GetScriptFactory(const char* name)
{
	assert(name != NULL);

	if (name != NULL) {
		ScriptFactory* factory = mScriptFactories;

		while (factory != NULL) {
			if (stricmp(factory->GetName(), name) == 0) {
				return factory;
			}

			factory = factory->GetNext();
		}
	}

	return NULL;
}

/******************************************************************************
*
* NAME
*     ScriptRegistrar::GetScriptFactory
*
* DESCRIPTION
*     Get script factory at specified location
*
* INPUTS
*     Index - Index of script factory.
*
* RESULTS
*     ScriptFactory - Factory for specified script.
*
******************************************************************************/

ScriptFactory* ScriptRegistrar::GetScriptFactory(int index)
{
	int count = 0;
	ScriptFactory* factory = mScriptFactories;

	while (factory != NULL) {
		if (count == index) {
			return factory;
		}

		count++;
		factory = factory->GetNext();
	}

	return NULL;
}

/******************************************************************************
*
* NAME
*     ScriptRegistrar::Count
*
* DESCRIPTION
*     Retrieve the number of registered scripts.
*
* INPUTS
*     NONE
*
* RESULTS
*     Count - Number of registered scripts
*
******************************************************************************/

int ScriptRegistrar::Count(void)
{
	int count = 0;
	ScriptFactory* factory = mScriptFactories;

	while (factory != NULL) {
		count++;
		factory = factory->GetNext();
	}

	return count;
}
