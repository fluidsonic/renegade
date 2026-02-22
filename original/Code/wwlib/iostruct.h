

#ifndef IOSTRUCT_H
#define IOSTRUCT_H


/*
** Some useful structures for writing/writing (safe from changes).
** The chunk IO classes contain code for reading and writing these.
*/
struct IOVector2Struct
{
	float		X;
	float		Y;
};

struct IOVector3Struct
{
	float		X;							// X,Y,Z coordinates
	float		Y;
	float		Z;
};

struct IOVector4Struct
{
	float		X;
	float		Y;
	float		Z;
	float		W;
};

struct IOQuaternionStruct
{
	float		Q[4];
};

#endif

