class BuildInfoClass
{

	public:

		/*
		** Build types.
		*/
		typedef enum {
			BUILD_RELEASE,
			BUILD_DEBUG,
			BUILD_PROFILE
		} BuildType;

		/*
		** Gets the 32 bit build number.
		*/
		static unsigned long Get_Build_Number(void);

		/*
		** Gets the 32 bit build number as a human readable string.
		*/
		static char *Get_Build_Number_String(void);

		/*
		** Gets the name of the person who built this executable.
		*/
		static char *Get_Builder_Name(void);

		/*
		** Gets the date this executable was built on.
		*/
		static char *Get_Build_Date_String(void);

		/*
		** Gets the initials of the person who built this executable.
		*/
		static char *Get_Builder_Initials(void);

		/*
		** Get a composite build number string with the works.
		*/
		static char *Get_Build_Version_String(void);

		/*
		** Get build type.
		*/
		static BuildType Get_Build_Type(void);
		static char *Get_Build_Type_String(void);

		/*
		** Dump build info to the log file
		*/
		static void Log_Build_Info(void);

		/*
		** Lots of build info together.
		*/
		static char *Composite_Build_Info(void);




	private:
		static char BuildNumber [64];
		static char BuildDate   [64];
};



