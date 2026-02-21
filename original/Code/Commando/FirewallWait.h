#ifndef __FIREWALLWAIT_H__
#define __FIREWALLWAIT_H__

#include <WWOnline/WaitCondition.h>
#include <windows.h>

namespace WWOnline
{
class Session;
}

/*
** Wait code for firewall/NAT detection.
**
**
**
*/

class FirewallDetectWait :
		public SingleWait
	{
	public:
		static RefPtr<FirewallDetectWait> Create(void);

		void WaitBeginning(void);
		WaitResult GetResult(void);

	protected:
		FirewallDetectWait();
		virtual ~FirewallDetectWait();

		FirewallDetectWait(const FirewallDetectWait&);
		const FirewallDetectWait& operator=(const FirewallDetectWait&);

		RefPtr<WWOnline::Session> mWOLSession;
		unsigned int mPingsRemaining;

		HANDLE mEvent;
	};




/*
** Wait class for clients when trying to open up a firewall for a server connection.
**
*/
class FirewallConnectWait :
		public SingleWait
	{
	public:
		static RefPtr<FirewallConnectWait> Create(void);

		void WaitBeginning(void);
		WaitResult GetResult(void);
		virtual void EndWait(WaitResult, const wchar_t* endText);

	protected:
		FirewallConnectWait();
		virtual ~FirewallConnectWait();

		FirewallConnectWait(const FirewallConnectWait&);
		const FirewallConnectWait& operator=(const FirewallConnectWait&);

		RefPtr<WWOnline::Session> mWOLSession;
		unsigned int mPingsRemaining;

		HANDLE mEvent;
		HANDLE mCancelEvent;

		/*
		** Did the port negotiation succeed?
		*/
		int mSuccessFlag;

		/*
		** How many players in the queue ahead of us?
		*/
		unsigned int mQueueCount;
		unsigned int mLastQueueCount;
		unsigned long mStartTime;
	};

#endif // __FIREWALLWAIT_H__
