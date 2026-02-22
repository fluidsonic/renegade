#pragma once

#include "global.h"

/**
** LightSolveProgressClass
** This class is used to communicate the current status of the light solve
*/
class LightSolveProgressClass
{
public:
	LightSolveProgressClass(void);

	/*
	** Read status (UI uses this to provide progress display)
	*/
	int				Get_Object_Count(void)								{ return ObjectCount; }
	int				Get_Processed_Object_Count(void)					{ return ProcessedObjectCount; }

	const char *	Get_Current_Mesh_Name(void)						{ return CurrentMeshName; }
	int				Get_Current_Mesh_Vertex_Count(void)				{ return CurrentVertexCount; }
	int				Get_Current_Vertex(void)							{ return CurrentVertex; }

	/*
	** Set status (vertex solver does this)
	*/
	void				Set_Object_Count(int count)						{ ObjectCount = count; }
	void				Increment_Processed_Object_Count(void)			{ ProcessedObjectCount++; }

	void				Set_Current_Mesh_Name(const char * name)		{ CurrentMeshName = name; }
	void				Set_Current_Mesh_Vertex_Count(int vcount)		{ CurrentVertexCount = vcount; }
	void				Set_Current_Vertex(int index)						{ CurrentVertex = index; }	

	/*
	** Cancellation request
	*/
	void				Request_Cancel(void)									{ CancelRequested = true; }
	bool				Is_Cancel_Requested(void)							{ return CancelRequested; }

protected:

	int				ObjectCount;
	int				ProcessedObjectCount;

	const char *	CurrentMeshName;
	int				CurrentVertexCount;
	int				CurrentVertex;

	bool				CancelRequested;
};

inline LightSolveProgressClass::LightSolveProgressClass(void) :
	ObjectCount(0),
	ProcessedObjectCount(0),
	CurrentMeshName(NULL),
	CurrentVertexCount(0),
	CurrentVertex(0),
	CancelRequested(false)
{
}
