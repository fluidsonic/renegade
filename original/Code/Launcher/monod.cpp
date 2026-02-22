#include "monod.h"

MonoD::MonoD(void)
{
}

MonoD::~MonoD()
{
}

int MonoD::print(const char *str, int len)
{
    for (int i=0; i<len; i++)
      fprintf(stderr,"%c",str[i]); 
    return(len);
}
