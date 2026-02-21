#ifndef _SCRIPTFACTORY_H_
#define _SCRIPTFACTORY_H_

class ScriptImpClass;
class ScriptRegistrar;

class ScriptFactory
	{
	public:
		virtual ~ScriptFactory();

		// Retrieve the name of this ScriptFactory
		const char* GetName(void);

		// Retrieve the parameter description for this ScriptFactory
		const char* GetParamDescription(void);

		// Create and instance of this Script
		virtual ScriptImpClass* Create(void) = 0;

	protected:
		friend class ScriptRegistrar;

		ScriptFactory(const char* name, const char* param);

		// Retrieve next node
		ScriptFactory* GetNext(void) const;

		// Set next node
		void SetNext(ScriptFactory* link);

	private:
		ScriptFactory* mNext;
		const char * ScriptName;
		const char * ParamDescription;
	};

#endif // _SCRIPTFACTORY_H_
