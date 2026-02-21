#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __HITTESTINFO_H
#define __HITTESTINFO_H


class NodeClass;
class NodeFunctionClass;
class VisGenPointClass;


/////////////////////////////////////////////////////////////////
//
// Structures and typedefs
//
/////////////////////////////////////////////////////////////////
typedef struct _HITTESTINFO
{	
	enum
	{
		Node		= 0,
		Function,
		VisPoint
	} Type;

	union
	{
		NodeClass *				node;
		NodeFunctionClass *	function;
		VisGenPointClass	*	vis_point;
	};

	_HITTESTINFO (NodeClass *new_node)					{ Type = Node; node = new_node; }
	_HITTESTINFO (NodeFunctionClass *new_function)	{ Type = Function; function = new_function; }
	_HITTESTINFO (VisGenPointClass *new_vis_point)	{ Type = VisPoint; vis_point = new_vis_point; }

} HITTESTINFO;


#endif //__HITTESTINFO_H
