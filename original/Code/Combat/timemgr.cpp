#include "timemgr.h"
#include "always.h"
#include "ww3d.h"
#include "slist.h"
#include "input.h"
#include "combat.h"
#include "ccamera.h"
#include "chunkio.h"
#include "persistfactory.h"
#include "combatchunkid.h"

#include "win.h"
//#include "systimer.h"		// for timegettime
#include "systimer.h"

/*
**
*/
int			TimeManager::FrameTicks = 0;
int			TimeManager::RealFrameTicks = 0;
int			TimeManager::LastTicks = 0;
float			TimeManager::TimeScale = 1.0f;
float			TimeManager::TotalSeconds = 0;
float			TimeManager::AveragedFPS = 0.0f;
int			TimeManager::AveragedFPSTicks = 0;
int			TimeManager::AveragedFPSCounter = 0;
float			TimeManager::FrameSeconds = 0.0f;
float			TimeManager::RealFrameSeconds = 0.0f;

#define		SLOWEST_FPS									      5

FrameTimeHistogramClass::FrameTimeHistogramClass(unsigned slot_count, float step)
	:
	SlotCount(slot_count),
	Step(step)
{
	Counts=new unsigned[SlotCount];
	Reset();
}

FrameTimeHistogramClass::~FrameTimeHistogramClass()
{
	delete[] Counts;
}

// Report normalized counts for each frame time slot, packed to unsigned bytes.
void FrameTimeHistogramClass::Get_Packed_Report(unsigned char* bytes)
{
	unsigned total=0;
	unsigned i;
	for (i=0;i<SlotCount;++i) {
		total+=Counts[i];
	}
	if (total==0) total=1;

	for (i=0;i<SlotCount;++i) {
		unsigned char value=255*Counts[i]/total;
		if (value==0 && Counts[i]!=0) value=1;
		bytes[i]=value;
	}

}

// Report the absolute counts for each frame time slot
void FrameTimeHistogramClass::Get_Report(unsigned* counts)
{
	unsigned i;
	for (i=0;i<SlotCount;++i) {
		counts[i]=Counts[i];
	}
}

// Reset the counts.
void FrameTimeHistogramClass::Reset()
{
	for (unsigned i=0;i<SlotCount;++i) {
		Counts[i]=0;
	}
}

// Place the time of current frame to a correct slot.
void FrameTimeHistogramClass::Add(float frame_time)
{
	unsigned long slot=WWMath::Float_To_Long(frame_time*(1000.0f/Step));
	if (slot>=SlotCount) slot=SlotCount-1;
	Counts[slot]++;
}

FrameTimeHistogramClass FrameTimeHistogram(16,15.0f);

FrameTimeHistogramClass& TimeManager::Peek_Frame_Time_Histogram()
{
	return FrameTimeHistogram;
}

/*
**
*/
int	TimeManager::SystemTicks()
{
	return TIMEGETTIME();
}

void TimeManager::Reset(void)
{
	FrameTicks = 0;
	RealFrameTicks = 0;
	LastTicks = 0;
	TimeScale = 1.0f;
	TotalSeconds = 0;
	AveragedFPS = 0.0f;
	AveragedFPSTicks = 0;
	AveragedFPSCounter = 0;
	FrameSeconds = 0.0f;
	RealFrameSeconds = 0.0f;
}

/*
**
*/
void	TimeManager::Update_Frame_Time()
{
	int	ticks = SystemTicks();
	if ( LastTicks == 0) {		// sync first time
		LastTicks = ticks;
	}

	FrameTicks = ticks - LastTicks;
	RealFrameTicks = ticks - LastTicks;
	LastTicks = ticks;

	FrameTicks = MIN( FrameTicks, (TICKS_PER_SECOND / SLOWEST_FPS) );

	if ( WW3D::Get_Movie_Capture_Frame_Rate() != 0.0f ) {
		FrameTicks = TICKS_PER_SECOND / WW3D::Get_Movie_Capture_Frame_Rate();
	}



	// Single Step
	static bool single_step = false;

	if ( COMBAT_CAMERA && COMBAT_CAMERA->Is_Snap_Shot_Mode() ) {
		FrameTicks = 0;
	}

	if ( CombatManager::Is_Game_Paused() ) {
		FrameTicks = 0;
	}

	FrameTicks *= TimeScale;
	FrameSeconds=(float)FrameTicks / TICKS_PER_SECOND;
	RealFrameSeconds=(float)RealFrameTicks / TICKS_PER_SECOND;

	WW3D::Sync( WW3D::Get_Sync_Time() + FrameTicks );

/*


*/
	TotalSeconds += Get_Frame_Seconds();

	/*
	** Time averaged fps (averaged over 10 seconds)
	*/
	AveragedFPSTicks += RealFrameTicks;
	AveragedFPSCounter++;
	if ( AveragedFPSTicks >= 10 * TICKS_PER_SECOND ) {
		AveragedFPS = (float)AveragedFPSCounter / ((float)AveragedFPSTicks / (float)TICKS_PER_SECOND);
		AveragedFPSTicks = AveragedFPSCounter = 0;
	}

	FrameTimeHistogram.Add(FrameSeconds);
}

void	TimeManager::Wait_Seconds( float time )
{
	float end = Get_Seconds() + time;
	while ( Get_Seconds() < end );
}

/***********************************************************************************************
 * TimeManager::Update( void ) - updates all current timers by the frame time
 *                                                                                             *
 * INPUT:
 * 																														  *
 * OUTPUT:
 * 																														  *
 * WARNINGS:	None																									  *
 * 																														  *
 * HISTORY:                                                                                    *
 *   09/01/1997 BG  : Created.                                                                 *
 *=============================================================================================*/
void	TimeManager::Update( void )
{
	Update_Frame_Time();


}

