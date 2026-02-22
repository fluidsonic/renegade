#pragma once

#include "global.h"

#include	"win.h"

class MonoClass {
	public:
      typedef enum MonoAttribute {
			INVISIBLE=0x00,				// Black on black.
			UNDERLINE=0x01,				// Underline.
			BLINKING=0x90,					// Blinking white on black.
			NORMAL=0x02,					// White on black.
			INVERSE=0x70					// Black on white.
      } MonoAttribute;

		MonoClass(void);
		~MonoClass(void);

		static void Enable(void) {Enabled = true;};
		static void Disable(void) {Enabled = false;};
		static bool Is_Enabled(void) {return Enabled;};

		void Sub_Window(int x=0, int y=0, int w=80, int h=25);
		void Fill_Attrib(int x, int y, int w, int h, MonoAttribute attrib);
		void Clear(void);
		void Set_Cursor(int x, int y);
		void Print(char const *text);
		void Print(int text);
		void Printf(char const *text, ...);
		void Printf(int text, ...);
		void Text_Print(char const *text, int x, int y, MonoAttribute attrib=NORMAL);
		void Text_Print(int text, int x, int y, MonoAttribute attrib=NORMAL);
		void View(void);
		void Scroll(int lines=1);
		void Pan(int cols=1);
		void Set_Default_Attribute(MonoAttribute attrib);

		/*
		**	This merely makes a duplicate of the mono object into a newly created mono
		**	object.
		*/
		MonoClass (MonoClass const &);

		static MonoClass * Current;

	private:

		/*
		**	Handle of the mono page.
		*/
		HANDLE Handle;

		/*
		**	If this is true, then monochrome output is allowed. It defaults to false
		**	so that monochrome output must be explicitly enabled.
		*/
		static bool Enabled;

		MonoClass & operator = (MonoClass const & );
};
