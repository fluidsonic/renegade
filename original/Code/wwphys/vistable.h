#pragma once

#include "global.h"

#include "vector.h"
#include "refcount.h"
#include "multilist.h"

class ChunkLoadClass;
class ChunkSaveClass;
class CompressedVisTableClass;

/*
** VisTableClass
** This is a bit vector which contains a bit for each static node in the world indicating
** whether that node can be seen from the current "vis-sector"
*/
class VisTableClass : public RefCountClass, public MultiListObjectClass
{
public:
	VisTableClass(unsigned bitcount,int id);
	VisTableClass(CompressedVisTableClass * ctable,int bitcount,int id);
	VisTableClass(const VisTableClass & that);
	~VisTableClass(void);

	VisTableClass & operator = (const VisTableClass & that);

	int			Get_Bit_Count(void) const								{ return BitCount; }

	void			Reset_All(void);
	void			Set_All(void);
	int			Get_Bit(int i) const;
	void			Set_Bit(int i,bool onoff);
	void			Delete_Bit(int i);
	
	void			Merge(const VisTableClass & that);
	void			Invert(void);
	bool			Is_Equal_To(const VisTableClass & that);
	int			Count_Differences(const VisTableClass & that);
	int			Count_True_Bits(void);
	float			Match_Fraction(const VisTableClass & that);

	void			Set_Vis_Sector_ID(int id)								{ VisSectorID = id; }
	int			Get_Vis_Sector_ID(void)									{ return VisSectorID; }
	void			Set_Time_Stamp(int timestamp)							{ Timestamp = timestamp; }
	int			Get_Time_Stamp(void) const								{ return Timestamp; }

protected:

	void			Alloc_Buffer(int bitcount);

	int			Get_Byte_Count(void) const;
	int			Get_Long_Count(void) const;
	uint8_t *		Get_Bytes(void) const;
	uint32_t *		Get_Longs(void) const;

	int			BitCount;
	uint32_t *		Buffer;

	int			VisSectorID;
	int			Timestamp;

	// Not implemented:
	bool operator == (const VisTableClass & that);

	friend class CompressedVisTableClass;
};

/*
** CompressedVisTableClass
** This is the form that pvs data is stored in memory when it is not being used.  It
** is basically a wrapper around an allocated array with functions to compress and
** decompress to/from a VisTableClass and functions for saving and loading. 
*/
class CompressedVisTableClass
{	
public:

	CompressedVisTableClass(void);
	CompressedVisTableClass(VisTableClass * bits);
	CompressedVisTableClass(const CompressedVisTableClass &that);
	~CompressedVisTableClass(void);

	const CompressedVisTableClass &operator= (const CompressedVisTableClass &that);

	void			Load(void* hfile);
	void			Save(void* hfile);
	
	void			Load(ChunkLoadClass & cload);
	void			Save(ChunkSaveClass & csave);

protected:

	int			Get_Byte_Count(void) const;
	uint8_t *		Get_Bytes(void);

	void			Compress(uint8_t * src_buffer,int src_size);
	void			Decompress(uint8_t * decomp_buffer,int decomp_size);
	
	int			BufferSize;
	uint8_t *		Buffer;

	// Not implemented:
	bool operator == (const CompressedVisTableClass & that);

	friend class VisTableClass;
};

inline int VisTableClass::Get_Bit(int i) const
{ 

	return (Buffer[i>>5] & (0x80000000u >> (i & 0x1F))); 
}

inline void VisTableClass::Set_Bit(int i,bool onoff) 
{ 

	if (onoff) {
		Buffer[i>>5] |= (0x80000000u >> (i & 0x01F)); 
	} else { 
		Buffer[i>>5] &= ~(0x80000000u >> (i & 0x01F)); 
	} 
}
