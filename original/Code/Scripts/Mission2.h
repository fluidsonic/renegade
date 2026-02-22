#pragma once

#include "global.h"

// Defines and Includes

#include "toolkit.h"

// Predefined Constants

#define M02_OBJCONTROLLER 1111112
#define M02_AREACOUNT     26

// Public Variables

Vector3 Objective_Radar_Locations [20] = 
{
	Vector3 (1113.53f,877.16f,17.47f), // Destroy or Deactivate Obelisk
	Vector3 (1001.69f,903.86f,0.82f),  // Destroy the Dam's MCT
	Vector3 (1209.60f,571.97f,18.15f), // Destroy the Hand of Nod
	Vector3 (1163.07f,513.01f,20.52f), // Commandeer the Nod Cargo Plane
	Vector3 (139.51f,-5.62f,-12.83f), // Destroy the Nod Helipad
	Vector3 (130.88f,21.10f,-8.49f), // Secure the Nod Guard Tower
	Vector3 (556.17f,23.18f,-53.88f), // Capture the Second House
	Vector3 (495.01f,114.70f,-55.84f), // Capture the First House
	Vector3 (648.77f,300.74f,-59.99f), // Capture the Third House
	Vector3 (425.43f,840.52f,8.09f), // Secure the Ski Resort
	Vector3 (468.59f,582.74f,-43.48f), // Destroy the First SAM
	Vector3 (493.76f,619.03f,-36.62f), // Destroy the Nod Convoy
	Vector3 (572.80f,873.57f,-2.83f), // Destroy the Second SAM
	Vector3 (801.57f,1054.95f,22.00f), // Destroy the Third SAM
	Vector3 (783.25f,945.26f,23.04f), // Destroy the Fourth SAM
	Vector3 (1062.0f,978.7f,-18.3f), // Destroy the Power Plant
	Vector3 (1300.2f,614.1f,19.2f), // Eliminate Tiberium Station C
	Vector3 (1310.1f,696.5f,19.5f), // Eliminate Tiberium Station A
	Vector3 (1342.2f,655.1f,19.3f), // Eliminate Tiberium Station B
	Vector3 (570.06f,261.47f,-60.0f) // Commandeer the GDI Mammoth Tank
};

// Enumerations

// Timer Enumerations

typedef enum
{
	M2TIMER_START = STIMER_MISSION2,
} M2TIMER;

inline void Send_Custom_To_SAM_Sites (GameObject * obj, int type, int param)
{
	GameObject * sam = Commands->Find_Object(1100085);
	if (sam)
	{
		Commands->Send_Custom_Event(obj, sam, type, param, 0.5f);
	}
	sam = Commands->Find_Object(1100094);
	if (sam)
	{
		Commands->Send_Custom_Event(obj, sam, type, param, 0.5f);
	}
	sam = Commands->Find_Object(1100120);
	if (sam)
	{
		Commands->Send_Custom_Event(obj, sam, type, param, 0.5f);
	}
	sam = Commands->Find_Object(1100130);
	if (sam)
	{
		Commands->Send_Custom_Event(obj, sam, type, param, 0.5f);
	}
}
