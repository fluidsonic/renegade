#pragma once

#include "global.h"

#include "vector2.h"
#include "vector2i.h"
#include "vector3.h"
#include "floodfillbox.h"

///////////////////////////////////////////////////////////////////////
// Forward declarations
///////////////////////////////////////////////////////////////////////
class AABoxClass;

///////////////////////////////////////////////////////////////////////
//
//	FloodfillGridClass
//
///////////////////////////////////////////////////////////////////////
class FloodfillGridClass
{
public:

	////////////////////////////////////////////////////////////////////
	//	Public constructors/destructors
	////////////////////////////////////////////////////////////////////
	FloodfillGridClass (void);
	virtual ~FloodfillGridClass (void);

	////////////////////////////////////////////////////////////////////
	//	Public methods
	////////////////////////////////////////////////////////////////////

	//
	//	Initialization methods
	//
	void						Initialize (const Vector3 &box_extents, const Vector3 &min_extents, const Vector3 &max_extents);

	//
	//	Insertion/removal methods
	//
	void						Add_Box (FloodfillBoxClass *box);
	bool						Remove_Box (FloodfillBoxClass *box);
	void						Reset (void);

	//
	//	Collection methods
	//
	void						Collect_Boxes (const AABoxClass &vol);
	BODY_BOX_LIST &		Get_Collection_List (void);
	FloodfillBoxClass *	Find_Box (const Vector3 &pos);
	int						Compute_Box_Count(const AABoxClass & vol);

	//
	// Accessors
	//
	Vector3					Get_Floodfill_Box_Extents(void);
	AABoxClass				Convert_To_AABox(FloodfillBoxClass * floodbox);

protected:

	////////////////////////////////////////////////////////////////////
	//	Protected methods
	////////////////////////////////////////////////////////////////////
	int				Get_Cell_Index (const Vector3 &pos);
	void				Point_To_Cell (const Vector3 &pos, int *cell_x, int *cell_y);

private:

	////////////////////////////////////////////////////////////////////
	//	Private member data
	////////////////////////////////////////////////////////////////////
	FloodfillBoxClass **	m_Grid;
	Vector3					m_BoxExtent;
	Vector2					m_CellSize;
	Vector2					m_WorldMin;
	Vector2					m_WorldMax;
	int						m_CellsX;
	int						m_CellsY;

	BODY_BOX_LIST			m_CollectionList;
};

////////////////////////////////////////////////////////////////////
// Get_Collection_List
////////////////////////////////////////////////////////////////////
inline BODY_BOX_LIST &
FloodfillGridClass::Get_Collection_List (void)
{
	return m_CollectionList;
}

////////////////////////////////////////////////////////////////////
// Get_Floodfill_Box_Size
////////////////////////////////////////////////////////////////////
inline Vector3 FloodfillGridClass::Get_Floodfill_Box_Extents(void)
{
	return m_BoxExtent;
}

////////////////////////////////////////////////////////////////////
// Point_To_Cell
////////////////////////////////////////////////////////////////////
inline void
FloodfillGridClass::Point_To_Cell (const Vector3 &pos, int *cell_x, int *cell_y)
{
	//
	//	Convert from 'world-coords' to 'grid-coords'
	//
	float cell_x_pos = pos.X - m_WorldMin.X;
	float cell_y_pos = pos.Y - m_WorldMin.Y;

	//
	//	Calculate cell indicies from the coordinates
	//
	(*cell_x) = int(cell_x_pos / m_CellSize.X);
	(*cell_y) = int(cell_y_pos / m_CellSize.Y);

	//
	//	Clamp the cells to the bounds
	//
	(*cell_x) = max ((*cell_x), 0);
	(*cell_y) = max ((*cell_y), 0);
	(*cell_x) = min ((*cell_x), m_CellsX - 1);
	(*cell_y) = min ((*cell_y), m_CellsY - 1);
	return;
}

////////////////////////////////////////////////////////////////////
// Get_Cell_Index
////////////////////////////////////////////////////////////////////
inline int
FloodfillGridClass::Get_Cell_Index (const Vector3 &pos)
{
	int cell_x = 0;
	int cell_y = 0;
	Point_To_Cell (pos, &cell_x, &cell_y);

	/*int min_x = 0;
	int min_y = 0;
	Point_To_Cell (pos - (m_BoxExtent-Vector3 (0.075F, 0.075F, 0.075F)), &min_x, &min_y);

	int max_x = 0;
	int max_y = 0;
	Point_To_Cell (pos + (m_BoxExtent-Vector3 (0.075F, 0.075F, 0.075F)), &max_x, &max_y);

	*/
	return (cell_y * m_CellsX) + cell_x;
}

////////////////////////////////////////////////////////////////////
// Add_Box
////////////////////////////////////////////////////////////////////
inline void
FloodfillGridClass::Add_Box (FloodfillBoxClass *box)
{
	//
	//	Calculate what cell this box should live in
	//
	int index = Get_Cell_Index (box->Get_Position ());

	//
	//	Link the box into the system at the given index
	//
	FloodfillBoxClass *cell_contents = m_Grid[index];
	m_Grid[index] = box;
	box->Set_Grid_Link (cell_contents);
	return ;
}

////////////////////////////////////////////////////////////////////
// Remove_Box
////////////////////////////////////////////////////////////////////
inline bool
FloodfillGridClass::Remove_Box (FloodfillBoxClass *box)
{
	//
	//	Calculate what cell this box lives in
	//
	int index = Get_Cell_Index (box->Get_Position ());

	//
	//	Attempt to find the box in this grid
	//
	bool found = false;
	FloodfillBoxClass *curr_box = NULL;
	FloodfillBoxClass *prev_box = NULL;
	for (	curr_box = m_Grid[index];
			curr_box != NULL && !found;
			curr_box = curr_box->Get_Grid_Link ())
	{
		//
		//	Is this the box we are looking for?
		//
		if (box == curr_box) {

			//
			//	Unlink this box from the grid
			//
			if (prev_box != NULL) {
				prev_box->Set_Grid_Link (box->Get_Grid_Link ());
			} else {
				m_Grid[index] = box->Get_Grid_Link ();
			}

			box->Set_Grid_Link (NULL);
			found = true;
		}

		prev_box = curr_box;
	}

	return found;
}
