#include "scriptfactory.h"
#include "scriptregistrar.h"
#include "scripts.h"
#include "dprint.h"
#include <string.h>

/******************************************************************************
*
* NAME
*     ScriptFactory::ScriptFactory
*
* DESCRIPTION
*     ScriptFactory constructor
*
* INPUTS
*     Name       - Script name.
*     Parameters - Parameter description string.
*
* RESULTS
*     NONE
*
******************************************************************************/

ScriptFactory::ScriptFactory(const char* name, const char* param)
	: mNext(NULL)
{
	// Save script name
	assert(name != NULL);
	ScriptName = name;

	// Save parameter description
	assert(param != NULL);
	ParamDescription = param;

	// Register this factory with the registrar
	ScriptRegistrar::RegisterScript(this);
}

/******************************************************************************
*
* NAME
*     ScriptFactory::~ScriptFactory
*
* DESCRIPTION
*     ScriptFactory destructor
*
* INPUTS
*     NONE
*
* RESULTS
*     NONE
*
******************************************************************************/

ScriptFactory::~ScriptFactory()
{
	// Remove this factory from the registrar
	ScriptRegistrar::UnregisterScript(this);

	ScriptName = NULL;
	ParamDescription = NULL;
}

/******************************************************************************
*
* NAME
*     ScriptFactory::GetNext
*
* DESCRIPTION
*     Retrieve next script factory.
*
* INPUTS
*     NONE
*
* RESULTS
*     ScriptFactory*
*
******************************************************************************/

ScriptFactory* ScriptFactory::GetNext(void) const
{
	return mNext;
}

/******************************************************************************
*
* NAME
*     ScriptFactory::SetNext
*
* DESCRIPTION
*     Set next script factory.
*
* INPUTS
*     ScriptFactory* link
*
* RESULTS
*     NONE
*
******************************************************************************/

void ScriptFactory::SetNext(ScriptFactory* link)
{
	if (mNext != NULL) {
		assert(link != NULL);
		link->SetNext(mNext);
	}

	mNext = link;
}

/******************************************************************************
*
* NAME
*     ScriptFactory::GetName
*
* DESCRIPTION
*     Retrieve the name.
*
* INPUTS
*     NONE
*
* RESULTS
*     Name - Name of script.
*
******************************************************************************/

const char* ScriptFactory::GetName(void)
{
	return ScriptName;
}

/******************************************************************************
*
* NAME
*     ScriptFactory::GetParamDescription
*
* DESCRIPTION
*     Retrieve the parameter description.
*
* INPUTS
*     NONE
*
* RESULTS
*     Parameters - Parameter description string.
*
******************************************************************************/

const char* ScriptFactory::GetParamDescription(void)
{
	return ParamDescription;
}

