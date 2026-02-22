#pragma once

#include "global.h"

#include <stdio.h>
#include <stdlib.h>
#include "wstypes.h"

class Wstring
{
 public: 
           Wstring();
           Wstring(IN const Wstring &other);
           Wstring(IN char *string);
          ~Wstring();

   void    clear(void);

   bit8    cat(IN char *string);
   bit8    cat(uint32_t size,IN char *string);
   bit8    cat(IN Wstring &string);

   void    cellCopy(OUT char *dest, uint32_t len);
   char    remove(int32_t pos, int32_t count);
   bit8    removeChar(char c);
   void    removeSpaces(void);
   char   *get(void);
   char    get(uint32_t index);
   uint32_t  length(void);
   bit8    insert(char c, uint32_t pos);
   bit8    insert(char *instring, uint32_t pos);
   bit8    replace(IN char *replaceThis,IN char *withThis);
   char    set(IN char *str);
   char    set(uint32_t size,IN char *str);
   bit8    set(char c, uint32_t index);
   void    setSize(int32_t bytes);  // create an empty string
   void    toLower(void);
   void    toUpper(void);
   bit8    truncate(uint32_t len);
   bit8    truncate(char c);  // trunc after char c
   int32_t  getToken(int offset,char *delim,Wstring &out);
   int32_t  getLine(int offset, Wstring &out);

   bit8    operator==(IN char *other);
   bit8    operator==(IN Wstring &other);
   bit8    operator!=(IN char *other);
   bit8    operator!=(IN Wstring &other);

   Wstring  &operator=(IN char *other);
   Wstring  &operator=(IN Wstring &other);
   Wstring  &operator+=(IN char *other);
   Wstring  &operator+=(IN Wstring &other);
   Wstring   operator+(IN char *other);
   Wstring   operator+(IN Wstring &other);

 private:
   char    *str;    // Pointer to allocated string.
};
