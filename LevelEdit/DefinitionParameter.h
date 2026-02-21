#if defined(_MSC_VER)
#pragma once
#endif

#ifndef __DEFINITION_PARAMETER_H
#define __DEFINITION_PARAMETER_H

#include "refcount.h"
#include "utils.h"


/////////////////////////////////////////////////////////////////////////////
//	Forward declarations
/////////////////////////////////////////////////////////////////////////////
class DefinitionClass;
class ParameterClass;


/////////////////////////////////////////////////////////////////////////////
//
// DefinitionParameterClass
//
/////////////////////////////////////////////////////////////////////////////
class DefinitionParameterClass : public RefCountClass
{
public:

	////////////////////////////////////////////////////////////
	//	Public constructors/destructor
	////////////////////////////////////////////////////////////
	DefinitionParameterClass (void)
		:	m_Definition (NULL),
			m_Parameter (NULL),
			m_Index (0),
			m_Parent (NULL) {}

	virtual ~DefinitionParameterClass (void) { MEMBER_RELEASE (m_Parent); }

	////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////
	
	//
	//	Parameter info
	//
	DefinitionClass *	Get_Definition (void) const				{ return m_Definition; }
	ParameterClass *	Get_Parameter (void) const					{ return m_Parameter; }
	int					Get_Index (void) const						{ return m_Index; }
	void					Set_Definition (DefinitionClass *def)	{ m_Definition = def; }
	void					Set_Parameter (ParameterClass *param)	{ m_Parameter = param; }
	void					Set_Index (int index)						{ m_Index = index; }

	//
	//	Parent info
	//
	DefinitionParameterClass *	Peek_Parent (void) const					{ return m_Parent; }
	void					Set_Parent (DefinitionParameterClass *	parent)	{ MEMBER_ADD (m_Parent, parent); }

private:
	
	////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////
	DefinitionClass *				m_Definition;
	ParameterClass *				m_Parameter;
	int								m_Index;

	DefinitionParameterClass *	m_Parent;
};


#endif //__DEFINITION_PARAMETER_H
