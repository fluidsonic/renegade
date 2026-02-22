//#define INDEX_BUFFER_LOG

#include "global.h"
#include "dx8indexbuffer.h"
#include "dx8wrapper.h"
#include "dx8caps.h"
#include "sphere.h"
#include "thread.h"

#define DEFAULT_IB_SIZE 5000

static bool _DynamicSortingIndexArrayInUse=false;
static SortingIndexBufferClass* _DynamicSortingIndexArray;
static unsigned short _DynamicSortingIndexArraySize=0;
static unsigned short _DynamicSortingIndexArrayOffset=0;	

static bool _DynamicDX8IndexBufferInUse=false;
static DX8IndexBufferClass* _DynamicDX8IndexBuffer=NULL;
static unsigned short _DynamicDX8IndexBufferSize=DEFAULT_IB_SIZE;
static unsigned short _DynamicDX8IndexBufferOffset=0;	

static int _IndexBufferCount;
static int _IndexBufferTotalIndices;
static int _IndexBufferTotalSize;

// ----------------------------------------------------------------------------
//
//
//
// ----------------------------------------------------------------------------

IndexBufferClass::IndexBufferClass(unsigned type_, unsigned short index_count_)
	:
	index_count(index_count_),
	type(type_),
	engine_refs(0)
{

	_IndexBufferCount++;
	_IndexBufferTotalIndices+=index_count;
	_IndexBufferTotalSize+=index_count*sizeof(unsigned short);
}

IndexBufferClass::~IndexBufferClass()
{
	_IndexBufferCount--;
	_IndexBufferTotalIndices-=index_count;
	_IndexBufferTotalSize-=index_count*sizeof(unsigned short);
}

unsigned IndexBufferClass::Get_Total_Buffer_Count()
{
	return _IndexBufferCount;
}

unsigned IndexBufferClass::Get_Total_Allocated_Indices()
{
	return _IndexBufferTotalIndices;
}

unsigned IndexBufferClass::Get_Total_Allocated_Memory()
{
	return _IndexBufferTotalSize;
}

void IndexBufferClass::Add_Engine_Ref() const
{
	engine_refs++; 
}

void IndexBufferClass::Release_Engine_Ref() const
{
	engine_refs--;
}

// ----------------------------------------------------------------------------
//
//
//
// ----------------------------------------------------------------------------

void IndexBufferClass::Copy(unsigned int* indices,unsigned first_index,unsigned count)
{

	if (first_index) {
		DX8IndexBufferClass::AppendLockClass l(this,first_index,count);
		unsigned short* inds=l.Get_Index_Array();
		for (unsigned v=0;v<count;++v) {
			*inds++=(unsigned short)(*indices++);
		}
	}
	else {
		DX8IndexBufferClass::WriteLockClass l(this);
		unsigned short* inds=l.Get_Index_Array();
		for (unsigned v=0;v<count;++v) {
			*inds++=(unsigned short)(*indices++);
		}
	}
}

// ----------------------------------------------------------------------------

void IndexBufferClass::Copy(unsigned short* indices,unsigned first_index,unsigned count)
{

	if (first_index) {
		DX8IndexBufferClass::AppendLockClass l(this,first_index,count);
		unsigned short* inds=l.Get_Index_Array();
		for (unsigned v=0;v<count;++v) {
			*inds++=*indices++;
		}
	}
	else {
		DX8IndexBufferClass::WriteLockClass l(this);
		unsigned short* inds=l.Get_Index_Array();
		for (unsigned v=0;v<count;++v) {
			*inds++=*indices++;
		}
	}
}

// ----------------------------------------------------------------------------
//
//
// ----------------------------------------------------------------------------

IndexBufferClass::WriteLockClass::WriteLockClass(IndexBufferClass* index_buffer_) : index_buffer(index_buffer_)
{
	DX8_THREAD_ASSERT();
	index_buffer->Add_Ref();
	switch (index_buffer->Type()) {
	case BUFFER_TYPE_DX8:
		DX8_Assert();
		DX8_ErrorCode(static_cast<DX8IndexBufferClass*>(index_buffer)->Get_DX8_Index_Buffer()->Lock(
			0,
			index_buffer->Get_Index_Count()*sizeof(WORD),
			(unsigned char**)&indices,
			0));
		break;
	case BUFFER_TYPE_SORTING:
		indices=static_cast<SortingIndexBufferClass*>(index_buffer)->index_buffer;
		break;
	default:
		break;
	}
}

// ----------------------------------------------------------------------------
//
//
// ----------------------------------------------------------------------------

IndexBufferClass::WriteLockClass::~WriteLockClass()
{
	DX8_THREAD_ASSERT();
	switch (index_buffer->Type()) {
	case BUFFER_TYPE_DX8:
		DX8_Assert();
		DX8_ErrorCode(static_cast<DX8IndexBufferClass*>(index_buffer)->index_buffer->Unlock());
		break;
	case BUFFER_TYPE_SORTING:
		break;
	default:
		break;
	}
	index_buffer->Release_Ref();
}

// ----------------------------------------------------------------------------

IndexBufferClass::AppendLockClass::AppendLockClass(IndexBufferClass* index_buffer_,unsigned start_index, unsigned index_range)
	:
	index_buffer(index_buffer_)
{
	DX8_THREAD_ASSERT();
	index_buffer->Add_Ref();
	switch (index_buffer->Type()) {
	case BUFFER_TYPE_DX8:
		DX8_Assert();
		DX8_ErrorCode(static_cast<DX8IndexBufferClass*>(index_buffer)->index_buffer->Lock(
			start_index*sizeof(unsigned short),
			index_range*sizeof(unsigned short),
			(unsigned char**)&indices,
			NULL));	// Optional pointer to receive the buffer size
		break;
	case BUFFER_TYPE_SORTING:
		indices=static_cast<SortingIndexBufferClass*>(index_buffer)->index_buffer+start_index;
		break;
	default:
		break;
	}
}

// ----------------------------------------------------------------------------

IndexBufferClass::AppendLockClass::~AppendLockClass()
{
	DX8_THREAD_ASSERT();
	switch (index_buffer->Type()) {
	case BUFFER_TYPE_DX8:
		DX8_Assert();
		DX8_ErrorCode(static_cast<DX8IndexBufferClass*>(index_buffer)->index_buffer->Unlock());
		break;
	case BUFFER_TYPE_SORTING:
		break;
	default:
		break;
	}
	index_buffer->Release_Ref();
}

// ----------------------------------------------------------------------------
//
//
//
// ----------------------------------------------------------------------------

DX8IndexBufferClass::DX8IndexBufferClass(unsigned short index_count_,UsageType usage)
	:
	IndexBufferClass(BUFFER_TYPE_DX8,index_count_)
{
	DX8_THREAD_ASSERT();
	unsigned usage_flags=
		D3DUSAGE_WRITEONLY|
		((usage&USAGE_DYNAMIC) ? D3DUSAGE_DYNAMIC : 0)|
		((usage&USAGE_NPATCHES) ? D3DUSAGE_NPATCHES : 0)|
		((usage&USAGE_SOFTWAREPROCESSING) ? D3DUSAGE_SOFTWAREPROCESSING : 0);
	if (!DX8Wrapper::Get_Current_Caps()->Support_TnL()) {
		usage_flags|=D3DUSAGE_SOFTWAREPROCESSING;
	}

	HRESULT ret=DX8Wrapper::_Get_D3D_Device8()->CreateIndexBuffer(
		sizeof(WORD)*index_count,
		usage_flags,
		D3DFMT_INDEX16,
		(usage&USAGE_DYNAMIC) ? D3DPOOL_DEFAULT : D3DPOOL_MANAGED,
		&index_buffer);

	if (SUCCEEDED(ret)) {
		return;
	}

	// Vertex buffer creation failed, so try releasing least used textures and flushing the mesh cache.

	// Free all textures that haven't been used in the last 5 seconds
	TextureClass::Invalidate_Old_Unused_Textures(5000);

	// Invalidate the mesh cache
	WW3D::_Invalidate_Mesh_Cache();

	// Try again...
	ret=DX8Wrapper::_Get_D3D_Device8()->CreateIndexBuffer(
		sizeof(WORD)*index_count,
		usage_flags,
		D3DFMT_INDEX16,
		(usage&USAGE_DYNAMIC) ? D3DPOOL_DEFAULT : D3DPOOL_MANAGED,
		&index_buffer);

	if (SUCCEEDED(ret)) {
	}

	// If it still fails it is fatal
	DX8_ErrorCode(ret);
}

// ----------------------------------------------------------------------------

DX8IndexBufferClass::~DX8IndexBufferClass()
{
	index_buffer->Release();
}

// ----------------------------------------------------------------------------
//
//
//
// ----------------------------------------------------------------------------

SortingIndexBufferClass::SortingIndexBufferClass(unsigned short index_count_)
	:
	IndexBufferClass(BUFFER_TYPE_SORTING,index_count_)
{

	index_buffer=new unsigned short[index_count];
}

// ----------------------------------------------------------------------------

SortingIndexBufferClass::~SortingIndexBufferClass()
{
	delete[] index_buffer;
}

// ----------------------------------------------------------------------------
//
//
//
// ----------------------------------------------------------------------------

DynamicIBAccessClass::DynamicIBAccessClass(unsigned short type_, unsigned short index_count_)
	:
	IndexCount(index_count_),
	IndexBuffer(0),
	Type(type_)
{
	if (Type==BUFFER_TYPE_DYNAMIC_DX8) {
		Allocate_DX8_Dynamic_Buffer();
	}
	else {
		Allocate_Sorting_Dynamic_Buffer();
	}
}

DynamicIBAccessClass::~DynamicIBAccessClass()
{
	REF_PTR_RELEASE(IndexBuffer);
	if (Type==BUFFER_TYPE_DYNAMIC_DX8) {
		_DynamicDX8IndexBufferInUse=false;
		_DynamicDX8IndexBufferOffset+=IndexCount;
	}
	else {
		_DynamicSortingIndexArrayInUse=false;
		_DynamicSortingIndexArrayOffset+=IndexCount;
	}
}

void DynamicIBAccessClass::_Deinit()
{
	REF_PTR_RELEASE(_DynamicDX8IndexBuffer);
	_DynamicDX8IndexBufferInUse=false;
	_DynamicDX8IndexBufferSize=DEFAULT_IB_SIZE;
	_DynamicDX8IndexBufferOffset=0;

	REF_PTR_RELEASE(_DynamicSortingIndexArray);
	_DynamicSortingIndexArrayInUse=false;
	_DynamicSortingIndexArraySize=0;
	_DynamicSortingIndexArrayOffset=0;	
}

// ----------------------------------------------------------------------------
//
//
//
// ----------------------------------------------------------------------------

DynamicIBAccessClass::WriteLockClass::WriteLockClass(DynamicIBAccessClass* ib_access_)
	:
	DynamicIBAccess(ib_access_)
{
	DX8_THREAD_ASSERT();
	DynamicIBAccess->IndexBuffer->Add_Ref();
	switch (DynamicIBAccess->Get_Type()) {
	case BUFFER_TYPE_DYNAMIC_DX8:
//		assert(!dynamic_dx8_index_buffer->Engine_Refs());
		DX8_Assert();
		DX8_ErrorCode(
			static_cast<DX8IndexBufferClass*>(DynamicIBAccess->IndexBuffer)->Get_DX8_Index_Buffer()->Lock(
			DynamicIBAccess->IndexBufferOffset*sizeof(WORD),
			DynamicIBAccess->Get_Index_Count()*sizeof(WORD),
			(unsigned char**)&Indices,
			!DynamicIBAccess->IndexBufferOffset ? D3DLOCK_DISCARD : D3DLOCK_NOOVERWRITE));
		break;
	case BUFFER_TYPE_DYNAMIC_SORTING:
		Indices=static_cast<SortingIndexBufferClass*>(DynamicIBAccess->IndexBuffer)->index_buffer;
		Indices+=DynamicIBAccess->IndexBufferOffset;
		break;
	default:
		break;
	}
}

DynamicIBAccessClass::WriteLockClass::~WriteLockClass()
{
	DX8_THREAD_ASSERT();
	switch (DynamicIBAccess->Get_Type()) {
	case BUFFER_TYPE_DYNAMIC_DX8:
		DX8_Assert();
		DX8_ErrorCode(static_cast<DX8IndexBufferClass*>(DynamicIBAccess->IndexBuffer)->Get_DX8_Index_Buffer()->Unlock());
		break;
	case BUFFER_TYPE_DYNAMIC_SORTING:
		break;
	default:
		break;
	}
	DynamicIBAccess->IndexBuffer->Release_Ref();
}

// ----------------------------------------------------------------------------
//
//
//
// ----------------------------------------------------------------------------

void DynamicIBAccessClass::Allocate_DX8_Dynamic_Buffer()
{
	_DynamicDX8IndexBufferInUse=true;

	// If requesting more indices than dynamic index buffer can fit, delete the ib
	// and adjust the size to the new count.
	if (IndexCount>_DynamicDX8IndexBufferSize) {
		REF_PTR_RELEASE(_DynamicDX8IndexBuffer);
		_DynamicDX8IndexBufferSize=IndexCount;
		if (_DynamicDX8IndexBufferSize<DEFAULT_IB_SIZE) _DynamicDX8IndexBufferSize=DEFAULT_IB_SIZE;
	}

	// Create a new vb if one doesn't exist currently
	if (!_DynamicDX8IndexBuffer) {
		unsigned usage=DX8IndexBufferClass::USAGE_DYNAMIC;
		if (DX8Wrapper::Get_Current_Caps()->Support_NPatches()) {
			usage|=DX8IndexBufferClass::USAGE_NPATCHES;
		}

		_DynamicDX8IndexBuffer=NEW_REF(DX8IndexBufferClass,(
			_DynamicDX8IndexBufferSize,
			(DX8IndexBufferClass::UsageType)usage));
		_DynamicDX8IndexBufferOffset=0;
	}

	// Any room at the end of the buffer?
	if (((unsigned)IndexCount+_DynamicDX8IndexBufferOffset)>_DynamicDX8IndexBufferSize) {
		_DynamicDX8IndexBufferOffset=0;
	}

	REF_PTR_SET(IndexBuffer,_DynamicDX8IndexBuffer);
	IndexBufferOffset=_DynamicDX8IndexBufferOffset;
}

void DynamicIBAccessClass::Allocate_Sorting_Dynamic_Buffer()
{
	_DynamicSortingIndexArrayInUse=true;

	unsigned new_index_count=_DynamicSortingIndexArrayOffset+IndexCount;
	if (new_index_count>_DynamicSortingIndexArraySize) {
		REF_PTR_RELEASE(_DynamicSortingIndexArray);
		_DynamicSortingIndexArraySize=new_index_count;
		if (_DynamicSortingIndexArraySize<DEFAULT_IB_SIZE) _DynamicSortingIndexArraySize=DEFAULT_IB_SIZE;
	}

	if (!_DynamicSortingIndexArray) {
		_DynamicSortingIndexArray=NEW_REF(SortingIndexBufferClass,(_DynamicSortingIndexArraySize));
		_DynamicSortingIndexArrayOffset=0;
	}

	REF_PTR_SET(IndexBuffer,_DynamicSortingIndexArray);
	IndexBufferOffset=_DynamicSortingIndexArrayOffset;
}

void DynamicIBAccessClass::_Reset(bool frame_changed)
{
	_DynamicSortingIndexArrayOffset=0;
	if (frame_changed) _DynamicDX8IndexBufferOffset=0;
}
