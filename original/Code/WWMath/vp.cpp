#include "vp.h"
#include "vector2.h"
#include "vector3.h"
#include "vector4.h"
#include "matrix3d.h"
#include "matrix4.h"
#include <memory.h>

#define SHUFFLE(x, y, z, w)	(((x)&3)<< 6|((y)&3)<<4|((z)&3)<< 2|((w)&3))
#define	BROADCAST(XMM, INDEX)	__asm	shufps	XMM,XMM,(((INDEX)&3)<< 6|((INDEX)&3)<<4|((INDEX)&3)<< 2|((INDEX)&3))

#define TRANSPOSE(BX, BY, BZ, BW, TV)					\
		__asm	movaps		TV,BZ						\
		__asm	unpcklps	BZ,BW						\
		__asm	unpckhps	TV,BW						\
		__asm	movaps		BW,BX						\
		__asm	unpcklps	BX,BY						\
		__asm	unpckhps	BW,BY						\
		__asm	movaps		BY,BX						\
		__asm	shufps		BX,BZ,SHUFFLE(1, 0, 1, 0)	\
		__asm	shufps		BY,BZ,SHUFFLE(3, 2, 3, 2)	\
		__asm	movaps		BZ,BW						\
		__asm	shufps		BZ,TV,SHUFFLE(1, 0, 1, 0)	\
		__asm	shufps		BW,TV,SHUFFLE(3, 2, 3, 2)


void VectorProcessorClass::Prefetch(void* address)
{
}

static Vector4 lastrow(0.0f,0.0f,0.0f,1.0f);
void VectorProcessorClass::Transform (Vector3* dst,const Vector3 *src, const Matrix3D& mtx, const int count)
{
	if (count<=0) return;

	{
		int i;

		for (i=0; i<count; i++)
		{
			dst[i]=mtx*src[i];
		}
	}
}

void VectorProcessorClass::Transform(Vector4* dst,const Vector3 *src, const Matrix4& matrix, const int count)
{
	if (count<=0) return;

	int i;

	for (i=0; i<count; i++)
	{
		dst[i]=matrix*src[i];
	}
}

void VectorProcessorClass::Copy(Vector2 *dst, const Vector2 *src, int count)
{
	if (count<=0) return;
	memcpy(dst,src,sizeof(Vector2)*count);
}

void VectorProcessorClass::Copy(unsigned *dst, const unsigned *src, int count)
{
	if (count<=0) return;
	memcpy(dst,src,sizeof(unsigned)*count);
}

void VectorProcessorClass::Copy(Vector3 *dst, const Vector3 *src, int count)
{
	if (count<=0) return;
	memcpy(dst,src,sizeof(Vector3)*count);
}

void VectorProcessorClass::Copy(Vector4 *dst, const Vector4 *src, int count)
{
	if (count<=0) return;
	memcpy(dst,src,sizeof(Vector4)*count);
}

void VectorProcessorClass::Copy(Vector4 *dst,const Vector3 *src, const float * srca, const int count)
{
	if (count<=0) return;
	int i;

	for (i=0; i<count; i++)
	{
		dst[i].X=src[i].X;
		dst[i].Y=src[i].Y;
		dst[i].Z=src[i].Z;
		dst[i].W=srca[i];
	}
}

void VectorProcessorClass::Copy(Vector4 *dst,const Vector3 *src, const float srca, const int count)
{
	if (count<=0) return;
	int i;

	for (i=0; i<count; i++)
	{
		dst[i].X=src[i].X;
		dst[i].Y=src[i].Y;
		dst[i].Z=src[i].Z;
		dst[i].W=srca;
	}
}

void VectorProcessorClass::Copy(Vector4 *dst,const Vector3 &src, const float * srca, const int count)
{
	if (count<=0) return;
	int i;

	for (i=0; i<count; i++)
	{
		dst[i].X=src.X;
		dst[i].Y=src.Y;
		dst[i].Z=src.Z;
		dst[i].W=srca[i];
	}
}

void VectorProcessorClass::CopyIndexed (unsigned *dst,const unsigned *src, const unsigned int *index, int count)
{
	if (count<=0) return;
	int i;

	for (i=0; i<count; i++)
	{
		dst[i]=src[index[i]];
	}
}

void VectorProcessorClass::CopyIndexed (Vector2 *dst,const Vector2 *src, const unsigned int *index, int count)
{
	if (count<=0) return;
	int i;

	for (i=0; i<count; i++)
	{
		dst[i]=src[index[i]];
	}
}

void VectorProcessorClass::CopyIndexed (Vector3 *dst,const Vector3 *src, const unsigned int *index, int count)
{
	if (count<=0) return;
	int i;

	for (i=0; i<count; i++)
	{
		dst[i]=src[index[i]];
	}
}

void VectorProcessorClass::CopyIndexed (Vector4 *dst,const Vector4 *src, const unsigned int *index, int count)
{
	if (count<=0) return;
	int i;

	for (i=0; i<count; i++)
	{
		dst[i]=src[index[i]];
	}
}

void VectorProcessorClass::CopyIndexed(unsigned char* dst, const unsigned char* src, const unsigned int *index, int count)
{
	if (count<=0) return;
	int i;

	for (i=0; i<count; i++)
	{
		dst[i]=src[index[i]];
	}
}

void VectorProcessorClass::CopyIndexed(float* dst, float* src, const unsigned int *index, int count)
{
	if (count<=0) return;
	int i;

	for (i=0; i<count; i++)
	{
		dst[i]=src[index[i]];
	}
}

void VectorProcessorClass::Clamp(Vector4 *dst,const Vector4 *src, const float min, const float max, const int count)
{
	if (count<=0) return;
	int i;

	for (i=0; i<count; i++)
	{
		dst[i].X=(src[i].X<min)?min:src[i].X;
		dst[i].X=(src[i].X>max)?max:src[i].X;

		dst[i].Y=(src[i].Y<min)?min:src[i].Y;
		dst[i].Y=(src[i].Y>max)?max:src[i].Y;

		dst[i].Z=(src[i].Z<min)?min:src[i].Z;
		dst[i].Z=(src[i].Z>max)?max:src[i].Z;

		dst[i].W=(src[i].W<min)?min:src[i].W;
		dst[i].W=(src[i].W>max)?max:src[i].W;
	}
}

void VectorProcessorClass::Clear(Vector3*dst, const int count)
{
	if (count<=0) return;
	memset(dst,0,sizeof(Vector3)*count);
}


void VectorProcessorClass::Normalize(Vector3 *dst, const int count)
{
	if (count<=0) return;
	int i;

	for (i=0; i<count; i++)
		dst[i].Normalize();
}

void VectorProcessorClass::MinMax(Vector3 *src, Vector3 &min, Vector3 &max, const int count)
{
	if (count<=0) return;
	min=*src;
	max=*src;

	int i;

	for (i=1; i<count; i++)
	{
		min.X=MIN(min.X,src[i].X);
		min.Y=MIN(min.Y,src[i].Y);
		min.Z=MIN(min.Z,src[i].Z);

		max.X=MAX(max.X,src[i].X);
		max.Y=MAX(max.Y,src[i].Y);
		max.Z=MAX(max.Z,src[i].Z);
	}
}

void VectorProcessorClass::MulAdd(float * dest,float multiplier,float add,int count)
{
	for (int i=0; i<count; i++) {
		dest[i] = dest[i] * multiplier + add;
	}
}

void VectorProcessorClass::DotProduct(float *dst, const Vector3 &a, const Vector3 *b,const int count)
{
	for (int i=0; i<count; i++)
		dst[i]=Vector3::Dot_Product(a,b[i]);
}

void VectorProcessorClass::ClampMin(float *dst, float *src, const float min, const int count)
{
	for (int i=0; i<count; i++)
		dst[i]=(src[i]>min?src[i]:min);
}

void VectorProcessorClass::Power(float *dst, float *src, const float pow, const int count)
{
	for (int i=0; i<count; i++)
		dst[i]=powf(src[i],pow);
}