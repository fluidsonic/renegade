#if defined(_MSC_VER)
#pragma once
#endif

#ifndef HUMANRECOIL_H
#define HUMANRECOIL_H

#include "always.h"

class RenderObjClass;
class Matrix3D;

/**
** HumanRecoilClass
** This class encapsulates all of the code to perform a programatic 'recoil' animation on
** a human skeleton in Renegade.  
**
** WARNING: this code assumes that you are using the same bone structure for every model
** that you apply recoils to.  The first time it is called, it caches bone indexes for
** the named bones that it modifies.  If you call it with two totally different skeletons
** the wrong bones will be moved.  If this is needed in the future we can extend this class
** and store an instance of it per game object or something...
*/
class HumanRecoilClass
{
public:

	HumanRecoilClass(void);

	void		Capture_Bones(RenderObjClass * model);
	void		Apply_Recoil(const Matrix3D & recoil_tm,RenderObjClass * model,float scale);
	void		Release_Bones(RenderObjClass * model);

protected:

	void		Initialize(RenderObjClass * model);

	static bool IsInitted;
};



#endif //HUMANRECOIL_H

