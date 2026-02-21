// cpudetect.cpp - stub for macOS (CPU detection is x86/Windows specific)
#include "cpudetect.h"
#include <sys/sysctl.h>

// Static member definitions
StringClass CPUDetectClass::ProcessorLog;
StringClass CPUDetectClass::CompactLog;

int CPUDetectClass::ProcessorType = 0;
int CPUDetectClass::ProcessorFamily = 0;
int CPUDetectClass::ProcessorModel = 0;
int CPUDetectClass::ProcessorRevision = 0;
int CPUDetectClass::ProcessorSpeed = 0;
__int64 CPUDetectClass::ProcessorTicksPerSecond = 1000000000LL;
double CPUDetectClass::InvProcessorTicksPerSecond = 1.0 / 1000000000.0;

CPUDetectClass::ProcessorManufacturerType CPUDetectClass::ProcessorManufacturer = CPUDetectClass::MANUFACTURER_UNKNOWN;
CPUDetectClass::IntelProcessorType CPUDetectClass::IntelProcessor = CPUDetectClass::INTEL_PROCESSOR_UNKNOWN;
CPUDetectClass::AMDProcessorType CPUDetectClass::AMDProcessor = CPUDetectClass::AMD_PROCESSOR_UNKNOWN;
CPUDetectClass::VIAProcessorType CPUDetectClass::VIAProcessor = CPUDetectClass::VIA_PROCESSOR_UNKNOWN;
CPUDetectClass::RiseProcessorType CPUDetectClass::RiseProcessor = CPUDetectClass::RISE_PROCESSOR_UNKNOWN;

unsigned CPUDetectClass::FeatureBits = 0;
unsigned CPUDetectClass::ExtendedFeatureBits = 0;

unsigned CPUDetectClass::L2CacheSize = 0;
unsigned CPUDetectClass::L2CacheLineSize = 0;
unsigned CPUDetectClass::L2CacheSetAssociative = 0;
unsigned CPUDetectClass::L1DataCacheSize = 0;
unsigned CPUDetectClass::L1DataCacheLineSize = 0;
unsigned CPUDetectClass::L1DataCacheSetAssociative = 0;
unsigned CPUDetectClass::L1InstructionCacheSize = 0;
unsigned CPUDetectClass::L1InstructionCacheLineSize = 0;
unsigned CPUDetectClass::L1InstructionCacheSetAssociative = 0;
unsigned CPUDetectClass::L1InstructionTraceCacheSize = 0;
unsigned CPUDetectClass::L1InstructionTraceCacheSetAssociative = 0;

unsigned CPUDetectClass::TotalPhysicalMemory = 0;
unsigned CPUDetectClass::AvailablePhysicalMemory = 0;
unsigned CPUDetectClass::TotalPageMemory = 0;
unsigned CPUDetectClass::AvailablePageMemory = 0;
unsigned CPUDetectClass::TotalVirtualMemory = 0;
unsigned CPUDetectClass::AvailableVirtualMemory = 0;

unsigned CPUDetectClass::OSVersionNumberMajor = 10;
unsigned CPUDetectClass::OSVersionNumberMinor = 0;
unsigned CPUDetectClass::OSVersionBuildNumber = 0;
unsigned CPUDetectClass::OSVersionPlatformId = 0;
StringClass CPUDetectClass::OSVersionExtraInfo;

bool CPUDetectClass::HasCPUIDInstruction = false;
bool CPUDetectClass::HasRDTSCInstruction = false;
bool CPUDetectClass::HasSSESupport = false;
bool CPUDetectClass::HasSSE2Support = false;
bool CPUDetectClass::HasCMOVSupport = false;
bool CPUDetectClass::HasMMXSupport = false;
bool CPUDetectClass::Has3DNowSupport = false;
bool CPUDetectClass::HasExtended3DNowSupport = false;
char CPUDetectClass::VendorID[20] = "Apple";
char CPUDetectClass::ProcessorString[48] = "Apple Silicon";

const char * CPUDetectClass::Get_Processor_Manufacturer_Name() { return "Apple"; }
bool CPUDetectClass::CPUID(unsigned&, unsigned&, unsigned&, unsigned&, unsigned) { return false; }

void CPUDetectClass::Init_CPUID_Instruction() {}
void CPUDetectClass::Init_Processor_Speed() {}
void CPUDetectClass::Init_Processor_String() {}
void CPUDetectClass::Init_Processor_Manufacturer() {}
void CPUDetectClass::Init_Processor_Family() {}
void CPUDetectClass::Init_Processor_Features() {}
void CPUDetectClass::Init_Cache() {}
void CPUDetectClass::Init_Intel_Processor_Type() {}
void CPUDetectClass::Init_AMD_Processor_Type() {}
void CPUDetectClass::Init_VIA_Processor_Type() {}
void CPUDetectClass::Init_Rise_Processor_Type() {}
void CPUDetectClass::Process_Cache_Info(unsigned) {}
void CPUDetectClass::Process_Extended_Cache_Info() {}
void CPUDetectClass::Init_Processor_Log() {}
void CPUDetectClass::Init_Compact_Log() {}

void CPUDetectClass::Init_Memory()
{
    uint64_t mem = 0;
    size_t len = sizeof(mem);
    if (sysctlbyname("hw.memsize", &mem, &len, NULL, 0) == 0) {
        // Cap at UINT_MAX since the fields are unsigned (32-bit)
        TotalPhysicalMemory = (mem > 0xFFFFFFFFULL) ? 0xFFFFFFFFU : (unsigned)mem;
        AvailablePhysicalMemory = TotalPhysicalMemory / 2;
        TotalPageMemory = TotalPhysicalMemory;
        AvailablePageMemory = TotalPhysicalMemory / 2;
        TotalVirtualMemory = TotalPhysicalMemory;
        AvailableVirtualMemory = TotalPhysicalMemory / 2;
    }
}

void CPUDetectClass::Init_OS()
{
    OSVersionPlatformId = 2; // VER_PLATFORM_WIN32_NT
    OSVersionNumberMajor = 10;
    OSVersionNumberMinor = 0;
    OSVersionExtraInfo = "macOS";
}

static class CPUDetectInitClass
{
public:
    CPUDetectInitClass()
    {
        CPUDetectClass::Init_Memory();
        CPUDetectClass::Init_OS();
    }
} _CPU_Detect_Init;
