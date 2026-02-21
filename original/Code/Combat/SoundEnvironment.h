#ifndef SOUNDENVIRONMENT_H
#define SOUNDENVIRONMENT_H

// Includes.
#include "refcount.h"

// Class declarations.
class CameraClass;
class PhysicsSceneClass;

// A simple class that, upon each update, will determine of there is an object vertically
// above the camera (at any height). If so then an amplitude value is attenuated. If, the
// camera is also in an environment zone, then the amplitude is attenuated further. Amplitude
// values are in the range 0..1. This class also uses a small mixing buffer so that amplitude
// changes occur smoothly over time. 
class SoundEnvironmentClass : public RefCountClass
{
	public:
		 SoundEnvironmentClass();
		~SoundEnvironmentClass();
		
		void  Reset();
		void  Update (PhysicsSceneClass *scene, CameraClass *camera);
		float Get_Amplitude() 		{return (AmplitudeSum / AMPLITUDE_BUFFER_SIZE);}

		void Add_User()		{UserCount++;}		// Call to indicate that you want to start using this object.
		void Remove_User()	{UserCount--;}		// Call to indicate that you no longer need this object.

	protected:
		
		enum {
			AMPLITUDE_BUFFER_SIZE = 8
		};

		unsigned	 UserCount;
		unsigned	 AmplitudeIndex;
		float		 AmplitudeSum;
		float		*AmplitudeBuffer;
};

#endif // SOUNDENVIRONMENT_H

