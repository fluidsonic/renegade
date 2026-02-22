#include	"always.h"
#include	"int.h"
#include	"mpmath.h"
#include	"rng.h"

template<> int Int<MAX_UNIT_PRECISION>::Error = 0;
template<> bool Int<MAX_UNIT_PRECISION>::Carry = false;
template<> bool Int<MAX_UNIT_PRECISION>::Borrow = false;
template<> Int<MAX_UNIT_PRECISION> Int<MAX_UNIT_PRECISION>::Remainder = {};

//BigInt Gcd(const BigInt & a, const BigInt & n);

