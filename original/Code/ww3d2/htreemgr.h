#pragma once

#include "global.h"

#include "hashtemplate.h"

class FileClass;
class ChunkLoadClass;
class HTreeClass;
class StringClass;

/*

	HTreeManagerClass

	This class is used to keep track of all of the hierarchy trees.
	A hierarchy tree is the base pose for a hierarchy model.

*/
class HTreeManagerClass
{

public:

	HTreeManagerClass(void);
	~HTreeManagerClass(void);

	int							Load_Tree(ChunkLoadClass & cload);
	int							Num_Trees(void) { return NumTrees; }
	HTreeClass *				Get_Tree(const char * name);
	HTreeClass *				Get_Tree(int id);
	uint32_t						Get_Tree_Handle(char * name);
	void							Free_All_Trees(void);

	int							Get_Tree_ID(const char * name);
   char *						Get_Tree_Name(const int id);

private:

	enum {
		MAX_TREES = 4096
	};

	void Free(void);

	int							NumTrees;
	HTreeClass *				TreePtr[MAX_TREES];		// TODO: no no! make this dynamic...
	HashTemplateClass<StringClass,HTreeClass*> TreeHash;

};
