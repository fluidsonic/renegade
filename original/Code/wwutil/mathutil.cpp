//
// Filename:     mathutil.cpp
// Project:      wwutil
// Author:       Tom Spencer-Smith
// Date:         June 1998
// Description:  
//
//-----------------------------------------------------------------------------
#include "mathutil.h" // I WANNA BE FIRST!

#include <math.h>
#include <stdlib.h>
#include "wwmath.h"
#include "miscutil.h"

const double cMathUtil::PI   = 3.1415927;
const double cMathUtil::PI_2 = 1.5707963;

//-----------------------------------------------------------------------------
//
// Returns a unit vector
//
void cMathUtil::Angle_To_Vector(double angle, double & dx, double & dy)
{

	double angleRadians;

	if (angle >= 0 && angle < 90) {
		angleRadians = angle * PI / 180.0;
		dx = WWMath::Sin(angleRadians);
		dy = WWMath::Cos(angleRadians);
	} else if (angle >= 90 && angle < 180) {
		angleRadians = (angle - 90) * PI / 180.0;
		dx = WWMath::Cos(angleRadians);
		dy = -WWMath::Sin(angleRadians);
	} else if (angle >= 180 && angle < 270) {
		angleRadians = (angle - 180) * PI / 180.0;
		dx = -WWMath::Sin(angleRadians);
		dy = -WWMath::Cos(angleRadians);
	} else {
		angleRadians = (angle - 270) * PI / 180.0;
		dx = -WWMath::Cos(angleRadians);
		dy = WWMath::Sin(angleRadians);
	}

   double len;
	len = ::sqrt(dx * dx + dy * dy); 

	//
   // Correction for Irish nature of windows y coords
   //
   dy *= -1;
}

//-----------------------------------------------------------------------------
void cMathUtil::Vector_To_Angle(double dx, double dy, double & angle)
{
	double theta;

	if (dx == 0 && dy == 0) {
		theta = 0;
	}

	if (dx == 0) {
		if (dy <= 0) {
			theta = 0;
		} else {
			theta = PI;
		}
	} else {
		theta = WWMath::Atan(-dy / dx);
		if (dx < 0) {
			theta += PI;
		}
		theta += 3 * PI_2;
		if (theta >= 2 * PI) {
			theta -= 2 * PI;
		}
		theta = 2 * PI - theta;
		if (theta == 2 * PI) {
			theta = 0;
		}
	}

	angle = theta * 180.0 / PI;
}

//-----------------------------------------------------------------------------
double cMathUtil::Simple_Distance(double x1, double y1, double x2, double y2)
{
	double dx = x2 - x1;
	double dy = y2 - y1;
	return(::sqrt(dx * dx + dy * dy));
}

//-----------------------------------------------------------------------------
int cMathUtil::Round(double arg)
{
   //return (int)(arg + 0.5);

	if (arg > MISCUTIL_EPSILON) {
		return (int) (arg + 0.5f);
	} else if (arg < -MISCUTIL_EPSILON) {
		return (int) (arg - 0.5f);
	} else {
		return 0;
	}
}

//-----------------------------------------------------------------------------
void cMathUtil::Rotate_Vector(double & vx, double & vy, double angle)
{
   double angle_radians = angle * PI / 180.0;

   double vx1 = vx;
   double vy1 = vy;

   vx = vx1 * ::WWMath::Cos(angle_radians) - vy1 * ::WWMath::Sin(angle_radians);
   vy = vx1 * ::WWMath::Sin(angle_radians) + vy1 * ::WWMath::Cos(angle_radians);
}

//-----------------------------------------------------------------------------
double cMathUtil::Get_Uniform_Pdf_Double(double lower, double upper)
{

   double x = lower + ::rand() / (double) RAND_MAX * (upper - lower);

   
   return x;
}

//-----------------------------------------------------------------------------
double cMathUtil::Get_Normalized_Uniform_Pdf_Double()
{
   return Get_Uniform_Pdf_Double(0, 1);
}

//-----------------------------------------------------------------------------
int cMathUtil::Get_Uniform_Pdf_Int(int lower, int upper)
{
   int x = lower + ::rand() % (upper - lower + 1);

   
   return x;
}

//-----------------------------------------------------------------------------
double cMathUtil::Get_Hat_Pdf_Double(double lower, double upper)
{

   double x;
   
   if (::fabs(upper - lower) < MISCUTIL_EPSILON) {
      x = lower;
   } else {

      double dx = (upper - lower) / 2.0f;
      double dy = 1 / dx;
      double m = dy / dx;
      double c = -m * lower;
   
      x = Get_Uniform_Pdf_Double(lower, lower + dx);
      double y = Get_Uniform_Pdf_Double(0, dy);

      if (y > m * x + c) {
         x += dx;
      }
   }

   return x;
}

//-----------------------------------------------------------------------------
double cMathUtil::Get_Normalized_Hat_Pdf_Double()
{
   return Get_Hat_Pdf_Double(0, 1);
}

//-----------------------------------------------------------------------------
int cMathUtil::Get_Hat_Pdf_Int(int lower, int upper)
{
   return Round(Get_Hat_Pdf_Double(lower, upper));
}

