#ifndef	TIMEMGR_H
#define	TIMEMGR_H

#ifndef	ALWAYS_H
	#include "always.h"
#endif

#ifndef	PERSIST_H
	#include "persist.h"
#endif

class	ChunkSaveClass;
class	ChunkLoadClass;

// Framerate histogram utility class
class FrameTimeHistogramClass
{
	unsigned* Counts;
	unsigned SlotCount;
	float Step;
public:
	FrameTimeHistogramClass(unsigned slot_count, float step);	// Number of millisecond-slots, step in milliseconds
	~FrameTimeHistogramClass();

	void Reset();
	void Add(float frame_time);

	unsigned Get_Slot_Count() const { return SlotCount; }
	float Get_Step() const { return Step; }

	void Get_Packed_Report(unsigned char* bytes);	// Normalized counts, as bytes
	void Get_Report(unsigned* counts);					// Absolute counts

};

/*
**
*/
// frame rate of timeGetTime()
#define	TICKS_PER_SECOND		1000

/*
**
*/
class		TimeManager	{

public:
	// Get the elapsed time this frame (technicaly last frame) in either Ticks or Seconds
	static	float			Get_Frame_Seconds( void )			{ return FrameSeconds; }
	static	float			Get_Frame_Real_Seconds( void )	{ return RealFrameSeconds; }
	static	int			Get_Frame_Ticks( void )				{ return FrameTicks; }
	static	float			Get_Total_Seconds( void )			{ return TotalSeconds; }

	// Get the absolute time in either Ticks or Seconds
	static	float			Get_Seconds( void )					{ return (float)SystemTicks() / TICKS_PER_SECOND; }

	// update frame and total time
	static	void			Update_Frame_Time( void );
	static	float			Get_Average_Frame_Rate( void )	{ return AveragedFPS; }
	// delay
	static		void		Wait_Seconds( float	seconds );

	// Update timers
	static	void	Update( void );

	static	void	Set_Time_Scale( float scale )							{ TimeScale = scale; }
	static	void	Reset(void);

	static	FrameTimeHistogramClass& Peek_Frame_Time_Histogram();
private:
	static	int			SystemTicks();
	static	int			FrameTicks;		   // ticks this frame
	static	int			RealFrameTicks;	// ticks this frame
	static	int			LastTicks;		   // Last game ticks
	static	float			TimeScale;			// Allow animation smoothing
	static	float			TotalSeconds;
	static	float			FrameSeconds;
	static	float			RealFrameSeconds;

	static	float			AveragedFPS;
	static	int			AveragedFPSTicks;
	static	int			AveragedFPSCounter;
};


#endif	//	TIMEMGR_H
