#pragma once

#include "global.h"

#include "refcount.h"
#include "vector.h"
#include "vector3.h"
#include "w3derr.h"

class ChunkLoadClass;

class SnapPointsClass : public DynamicVectorClass<Vector3>, public RefCountClass
{
public:

	WW3DErrorType Load_W3D(ChunkLoadClass & cload);

protected:

	~SnapPointsClass(void) {}

};
