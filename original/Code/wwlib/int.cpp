#include	"always.h"
#include	"int.h"
#include	"mpmath.h"
#include	"rng.h"

#if defined(__clang__) && !defined(_MSC_VER)
template<> int Int<MAX_UNIT_PRECISION>::Error = 0;
template<> bool Int<MAX_UNIT_PRECISION>::Carry = false;
template<> bool Int<MAX_UNIT_PRECISION>::Borrow = false;
template<> Int<MAX_UNIT_PRECISION> Int<MAX_UNIT_PRECISION>::Remainder = {};
#else
int bignum::Error = 0;
bool bignum::Carry = false;
bool bignum::Borrow = false;
bignum bignum::Remainder;
#endif

//BigInt Gcd(const BigInt & a, const BigInt & n);

