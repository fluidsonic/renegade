#pragma once

#include "global.h"

#include "scriptfactory.h"

// Script factory registrant
template<class T>
class	ScriptRegistrant : public ScriptFactory
	{
	public:
		ScriptRegistrant(const char* name, const char* param)
			: ScriptFactory(name, param)
			{}

		virtual ScriptImpClass* Create(void)
			{
			T* script = new T;
			assert(script != NULL);
			script->SetFactory(this);
			script->Register_Auto_Save_Variables();
			return script;
			}
	};

// Register script factory
#define REGISTER_SCRIPT(x, d) \
	class x; \
	ScriptRegistrant<x> _## x ##Registrant(#x, d);
