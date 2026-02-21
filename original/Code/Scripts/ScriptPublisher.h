#ifndef _SCRIPTPUBLISHER_H_
#define _SCRIPTPUBLISHER_H_

#include "slist.h"

class ScriptClass;

class ScriptPublisher
{
	public:
		// Add a subscriber to the publisher list
		void AddSubscriber(ScriptClass* subscriber);

		// Remove a subscriber from the publisher list
		void RemoveSubscriber(ScriptClass* subscriber);

		// Send subscribers a message
		void NotifySubscribers(int what, int param);

	protected:
		// Protected to prevent direct instantiation or deletion
		ScriptPublisher();
		virtual ~ScriptPublisher();

	private:
		SList<ScriptClass> mSubscribers;
};

#endif // _SCRIPTPUBLISHER_H_
