#pragma once

static_assert(sizeof(char16_t)  == 2, "char16_t must be 2 bytes (UTF-16)");
static_assert(sizeof(char32_t)  == 4, "char32_t must be 4 bytes (UTF-32)");

static_assert(sizeof(float32_t) == 4, "float32_t must be 4 bytes (IEEE 754 single)");
static_assert(sizeof(float64_t) == 8, "float64_t must be 8 bytes (IEEE 754 double)");

static_assert(sizeof(int8_t)    == 1, "int8_t must be 1 byte");
static_assert(sizeof(uint8_t)   == 1, "uint8_t must be 1 byte");
static_assert(sizeof(int16_t)   == 2, "int16_t must be 2 bytes");
static_assert(sizeof(uint16_t)  == 2, "uint16_t must be 2 bytes");
static_assert(sizeof(int32_t)   == 4, "int32_t must be 4 bytes");
static_assert(sizeof(uint32_t)  == 4, "uint32_t must be 4 bytes");
static_assert(sizeof(int64_t)   == 8, "int64_t must be 8 bytes");
static_assert(sizeof(uint64_t)  == 8, "uint64_t must be 8 bytes");
