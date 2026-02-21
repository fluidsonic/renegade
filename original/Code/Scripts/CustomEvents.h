#ifndef _CUSTOMEVENTS_H_
#define _CUSTOMEVENTS_H_

// Custom script events
typedef enum {
	// Global events 0-999
	SCMD_GLOBAL = 0,

	// Sent by subscribers to publishers to request subscription to published events.
	// Note: Not all scripts support subscription, if a script subscribes to an object
	//       that does not publish then it will never receive messages from it.
	//
	// Param = Subscriber
	SCMD_SUBSCRIBE,

	// Sent by subscribers to publishers to request removal from subscriber list.
	//
	// Param = Subscriber
	SCMD_UNSUBSCRIBE,

	// Sent by the mission controller when an objective has failed to be satisfied.
	// (An example of a failed objective would be when an object that was to be
	// protected was destroyed or when a timed objective expired.)
	//
	// Param = Objective Number
	SCMD_OBJECTIVE_FAILED,

	// Sent by mission controller when an objective has been completed.
	//
	// Param: Objective Number
	SCMD_OBJECTIVE_COMPLETE,

	// Sent by mission controller when the mission is lost.
	SCMD_MISSION_FAILED,
	
	// Send by mission controller when the mission is accomplished.
	SCMD_MISSION_ACCOMPLISHED,

	// Refer to Group definition for description of group events.
	SCMD_GROUP_EVENT,

	// Mission 1 events 1000-1999	
	SCMD_MISSION1 = 1000,

	// Mission 2 events 2000-2999	
	SCMD_MISSION2 = 2000,

	// Mission 3 events 3000-3999	
	SCMD_MISSION3 = 3000,

	// Mission 4 events 4000-4999	
	SCMD_MISSION4 = 4000,

	// Mission 5 events 5000-5999	
	SCMD_MISSION5 = 5000,

	// Mission 6 events 6000-6999	
	SCMD_MISSION6 = 6000,

	// Mission 7 events 7000-7999	
	SCMD_MISSION7 = 7000,

		// Mission 8 events 8000-8999	
	SCMD_MISSION8 = 8000,

	// Mission 9 events 9000-9999	
	SCMD_MISSION9 = 9000,

	// Mission 10 events 10000-10999	
	SCMD_MISSION10 = 10000,

	// Mission 11 events 11000-11999	
	SCMD_MISSION11 = 11000,

	// Mission 12 events 12000-12999	
	SCMD_MISSION12 = 12000,

	// PR Demo events 99000-99999	
	SCMD_PRDEMO = 99000,

	// Programmer generated events begin at 1000000000

} SCRIPT_CUSTOMEVENT;


// Script timer IDs
typedef enum {
	// Global timers 0-99
	STIMER_GLOBAL = 0,

	STIMER_BEAT,
	STIMER_SURVIVE,

	// Mission 1 timers 100-199	
	STIMER_MISSION1 = 100,

	// Mission 2 timers 200-299	
	STIMER_MISSION2 = 200,

	// Mission 3 timers 300-399	
	STIMER_MISSION3 = 300,

	// Mission 4 timers 400-499	
	STIMER_MISSION4 = 400,

	// Mission 5 timers 500-599	
	STIMER_MISSION5 = 500,

	// Mission 6 timers 600-699	
	STIMER_MISSION6 = 600,

	// Mission 7 timers 700-799	
	STIMER_MISSION7 = 700,

	// Mission 8 timers 800-899	
	STIMER_MISSION8 = 800,

	// Mission 9 timers 900-999	
	STIMER_MISSION9 = 900,

	// Mission 10 timers 1000-1099	
	STIMER_MISSION10 = 1000,

	// Mission 11 timers 1100-1199	
	STIMER_MISSION11 = 1100,

	// Mission 12 timers 1200-1299	
	STIMER_MISSION12 = 1200,

	// PR Demo timers 1300-1399	
	STIMER_PRDEMO = 1300,

	// Toolkit timers 9900 - 9999
	STIMER_TOOLKIT = 9900,

} SCRIPT_TIMER;

#endif // _CUSTOMEVENTS_H_
