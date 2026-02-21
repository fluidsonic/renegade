//
// rc4.cpp
// RC4 encryption / decryption
//
#include "rc4.h"
#include <memory.h>

static unsigned char RC4_Temp_Byte;
#define RC4_SWAP_BYTE(a,b) RC4_Temp_Byte=a; a=b; b=RC4_Temp_Byte

//
// Don't rely on this to zero the key
//
RC4Class::RC4Class()
{
	memset(Key.State, 0, 256);
	Key.X=0;
	Key.Y=0;
}

//
// Setup the encryption key.  This must be called before you encrypt/decrypt!
//
void RC4Class::Prepare_Key(const unsigned char *key_data_ptr, int key_data_len)
{
	unsigned char index1;
	unsigned char index2;
	unsigned char *state;
	int counter;

	state = &Key.State[0];
	for (counter = 0; counter < 256; counter++)
		state[counter] = (unsigned char)counter;
	Key.X = 0;
	Key.Y = 0;
	index1 = 0;
	index2 = 0;
	for (counter = 0; counter < 256; counter++) {
		index2 = (unsigned char)(key_data_ptr[index1] + state[counter] + index2);
		RC4_SWAP_BYTE(state[counter], state[index2]);
		index1 = (unsigned char)((index1 + 1) % key_data_len);
	}
}

//
// RC4 in standard mode.
//
// This will XOR the buffer with the RC4 stream (like a one time pad).
//
void RC4Class::RC4(unsigned char *buffer_ptr, int buffer_len)
{
	unsigned char x;
	unsigned char y;
	unsigned char *state;
	int counter;

	x = Key.X;
	y = Key.Y;

	state = &Key.State[0];
	for (counter = 0; counter < buffer_len; counter++) {
		x++;
		y = (unsigned char)(y + state[x]);
		RC4_SWAP_BYTE(state[x], state[y]);
		buffer_ptr[counter] ^= state[(state[x] + state[y]) & 255];
	}
	Key.X = x;
	Key.Y = y;
}
