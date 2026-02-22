#pragma once
// Guarantee IEEE 754 float sizes at compile time.
// float and double are used directly (no aliases).

static_assert(sizeof(float)  == 4, "float must be 32-bit IEEE 754");
static_assert(sizeof(double) == 8, "double must be 64-bit IEEE 754");
