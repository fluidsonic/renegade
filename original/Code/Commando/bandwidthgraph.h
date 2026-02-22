#ifndef __BANDWIDTHGRAPH_H__
#define __BANDWIDTHGRAPH_H__

#include "wwstring.h"

class Render2DTextClass;
class Font3DInstanceClass;

//-----------------------------------------------------------------------------
class	cBandwidthGraph
{
public:
	static void							Onetime_Init(void);
	static void							Onetime_Shutdown(void);

	static void							Think(void);
	static void							Render(void);

	static void							Set_Scale(int scale) {BandwidthScaler = scale;}

private:
	static void							Bandwidth_Graph(StringClass &	label, int bps,
												int target_bps, float bandwidth_multiplier, float average_priority, bool is_loading);

	static Render2DTextClass	*	PTextRenderer;
	static Font3DInstanceClass *	PFont;
	static int							BandwidthScaler;
	static float						YPosition;
	static float						BarHeight;
	static float						BarWidth;
	static float						YIncrement;
};

//-----------------------------------------------------------------------------

#endif	// __BANDWIDTHGRAPH_H__