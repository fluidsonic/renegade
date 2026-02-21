#ifndef __DLGDOWNLOAD_H__
#define __DLGDOWNLOAD_H__

#include <PopupDialog.h>
#include <WWOnline/WOLDownload.h>

class DlgDownload :
		public PopupDialogClass
	{
	public:
		static bool DoDialog(const WCHAR* title, const WWOnline::DownloadList& files, bool quiet = false);

	protected:
		DlgDownload();
		~DlgDownload();

		// Prevent copy and assignment
		DlgDownload(const DlgDownload&);
		const DlgDownload& operator=(const DlgDownload&);

		bool FinalizeCreate(const WWOnline::DownloadList& files);

		void On_Init_Dialog(void);
		void On_Command(int ctrl, int message, DWORD param);
		void On_Periodic(void);

		void UpdateProgress(WWOnline::DownloadEvent& event);
		static void HandleCallback(WWOnline::DownloadEvent& event, unsigned long userdata);

	private:
		RefPtr<WWOnline::DownloadWait> mWait;

		unsigned long mStartTime;
		bool mDownloading;
		static bool mQuietMode;
	};

#endif // __DLGDOWNLOAD_H__