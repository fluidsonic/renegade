#include "global.h"
#include "assetstatus.h"
#include "hashtemplate.h"
#include "wwstring.h"
#include "rawfile.h"

AssetStatusClass AssetStatusClass::Instance;

const char* ReportCategoryNames[AssetStatusClass::REPORT_COUNT]={
	"LOAD_ON_DEMAND_ROBJ",
	"LOAD_ON_DEMAND_HANIM",
	"LOAD_ON_DEMAND_HTREE",
	"MISSING_ROBJ",
	"MISSING_HANIM",
	"MISSING_HTREE"
};

AssetStatusClass::AssetStatusClass()
	:
	Reporting (true),
	LoadOnDemandReporting(false)
{
}

AssetStatusClass::~AssetStatusClass()
{
}

void AssetStatusClass::Add_To_Report(int index, const char* name)
{
	StringClass lower_case_name(name,true);
	_strlwr(lower_case_name.Peek_Buffer());
	// This is a bit slow - two accesses to the same member, but currently there's no better way to do it.
	int count=ReportHashTables[index].Get(lower_case_name);
	count++;
	ReportHashTables[index].Set_Value(lower_case_name,count);
}

void AssetStatusClass::Report_Load_On_Demand_RObj(const char* name)
{
	if (LoadOnDemandReporting) Add_To_Report(REPORT_LOAD_ON_DEMAND_ROBJ,name);
}

void AssetStatusClass::Report_Load_On_Demand_HAnim(const char* name)
{
	if (LoadOnDemandReporting) Add_To_Report(REPORT_LOAD_ON_DEMAND_HANIM,name);
}

void AssetStatusClass::Report_Load_On_Demand_HTree(const char* name)
{
	if (LoadOnDemandReporting) Add_To_Report(REPORT_LOAD_ON_DEMAND_HTREE,name);
}

void AssetStatusClass::Report_Missing_RObj(const char* name)
{
	Add_To_Report(REPORT_MISSING_ROBJ,name);
}

void AssetStatusClass::Report_Missing_HAnim(const char* name)
{
	Add_To_Report(REPORT_MISSING_HANIM,name);
}

void AssetStatusClass::Report_Missing_HTree(const char* name)
{
	Add_To_Report(REPORT_MISSING_HTREE,name);
}
