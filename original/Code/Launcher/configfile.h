#pragma once

#include "global.h"

#include "dictionary.h"
#include "wstring.h"

class ConfigFile
{
 public:
             ConfigFile();
            ~ConfigFile();
 bit8        readFile(IN FILE *config);
 bit8        getString(IN Wstring &key,OUT Wstring &value);
 bit8        getString(IN char *key,OUT Wstring &value);

 bit8        getInt(IN Wstring &key,OUT int32_t &value);
 bit8        getInt(IN char *key,OUT int32_t &value);

 bit8        getInt(IN Wstring &key,OUT int16_t &value);
 bit8        getInt(IN char *key,OUT int16_t &value);

 private:
  Dictionary<Wstring,Wstring> dictionary; // stores the mappings from keys
                                        //  to value strings
};
