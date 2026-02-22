//
// SecureRandomClass - Generate random values
//


#include "global.h"
#include "srandom.h"
#include <stdlib.h>
#include <stdio.h>
#include "osdep.h"
#include <time.h>
#include <assert.h>
#include "sha.h"

// Static class variables
unsigned char	SecureRandomClass::Seeds[SecureRandomClass::SeedLength];
bool				SecureRandomClass::Initialized=false;
unsigned int	SecureRandomClass::RandomCache[SecureRandomClass::SHADigestBytes / sizeof(unsigned int)];
int				SecureRandomClass::RandomCacheEntries=0;
unsigned int	SecureRandomClass::Counter=0;
Random3Class	SecureRandomClass::RandomHelper;

SecureRandomClass::SecureRandomClass()
{
	if (Initialized == false)
	{
		Generate_Seed();
		Initialized=true;
	}
}

SecureRandomClass::~SecureRandomClass()
{
}

//
// Add seed values to our pool of randomness
//
void SecureRandomClass::Add_Seeds(unsigned char *values, int length)
{
	for (int i=0; i<length; i++)
	{
		Seeds[0]^=values[i];

		// Rotate the seeds to the left
		unsigned char uctemp=Seeds[SeedLength-1];
		for (int j=SeedLength-1; j>=1; j--)
			Seeds[j]=Seeds[j-1];
		Seeds[0]=uctemp;
	}

	// We have a better seed pool now so trigger new random values
	RandomCacheEntries=0;
}

//
// Get a 32bit random value
//
unsigned long SecureRandomClass::Randval(void)
{
	if (RandomCacheEntries == 0)
	{
		SHAEngine sha;
		char digest[SHADigestBytes];	// SHA produces a 20 byte hash

		sha.Hash(Seeds, SeedLength);
		sha.Result(digest);

		memcpy(RandomCache, digest, SHADigestBytes);
		RandomCacheEntries=(SHADigestBytes / sizeof(unsigned int));

		unsigned int *int_seeds=(unsigned int *)Seeds;
		int_seeds[0]^=Counter;			// remove the last counter (double xor)
		int_seeds[0]^=(Counter+1);		// put the new counter in place

		int_seeds[(SeedLength/sizeof(int))-1]^=Counter;		// remove the last counter (double xor)
		int_seeds[(SeedLength/sizeof(int))-1]^=(Counter+1);	// put the new counter in place

		Counter++;				// increment counter
	}

	unsigned long retval=RandomCache[--RandomCacheEntries];

	// SHA doesn't have the best distribution properties in the world
	//   We'll XOR the result with the output of another random number
	unsigned long helperval=RandomHelper();
	retval^=helperval;

	return(retval);
}

/////////////////////////////// Private Methods ///////////////////////////////////////

//
// Seed the random number generator.
// The seed is what makes each run of random numbers unique.  If an observer
//   can guess your seed they can predict your random numbers.
//
//	Note the use of XORs everywhere.  The XOR of a good random number and a bad random
//		number is still a good random number.
//
// Caution: Under windows this isn't nearly as safe as under UNIX!
//
void SecureRandomClass::Generate_Seed(void)
{
	int i;

	// Start with some garbage values
	memset(Seeds, 0xAA, SeedLength);

	unsigned int *int_seeds=(unsigned int *)Seeds;
	int int_seed_length=SeedLength/sizeof(unsigned int);

	//
	// On UNIX we've already got a great random number souce.
	// This should be used only for a seed since it's slow.
	//
	FILE *in=fopen("/dev/random","r");
	if (in)
	{
		for (i=0; i<SeedLength; i++)
			Seeds[i]^=fgetc(in); 
		fclose(in);
	}
	else
		assert(0);

	for (i=0; i<int_seed_length; i++)
	{
		if ((i % 4) == 0)
			int_seeds[i]^=time(NULL);
		else if ((i % 4) == 1)
			int_seeds[i]^=getpid();
		else if ((i % 4) == 2)
			int_seeds[i]^=GetTickCount();
		else if ((i % 4) == 3)
			int_seeds[i]^=i;
	}
}
