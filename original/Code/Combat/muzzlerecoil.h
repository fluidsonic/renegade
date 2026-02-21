#if defined(_MSC_VER)
#pragma once
#endif

#ifndef MUZZLERECOIL_H
#define MUZZLERECOIL_H

#include "always.h"

class RenderObjClass;

/**
** MuzzleRecoilClass
** This class tracks the recoil state of a muzzle for an ArmedGameObj.  To set up the object,
** call Init with the bone index that you want it to control.  To trigger a recoil, call 
** Start_Recoil.  To make everything work, call Update once per frame with the model pointer
** and the amount of time that has elapsed.
*/
class MuzzleRecoilClass
{
public:
	MuzzleRecoilClass(void);

	void		Init(int bone_index);
	void		Start_Recoil(float recoil_scale,float recoil_time);
	void		Update(RenderObjClass * model);

protected:

	int		BoneIndex;			// bone to move
	float		RecoilScale;		// size of the initial translation
	float		RecoilTimer;		// counts down to 0.0s
	float		OORecoilTime;		// one-over the total time for the recoil effect

};



#endif //MUZZLERECOIL_H

