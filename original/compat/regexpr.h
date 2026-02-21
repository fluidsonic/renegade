#pragma once
#ifndef REGEXPR_H
#define REGEXPR_H

// RegularExpressionClass stub - gnu_regex not available in GPL release
class RegularExpressionClass {
public:
    RegularExpressionClass(const char* expression = 0) : Valid(false) { (void)expression; }
    RegularExpressionClass(const RegularExpressionClass& copy) : Valid(false) { (void)copy; }
    ~RegularExpressionClass(void) {}

    bool Compile(const char* expression) { (void)expression; return false; }
    bool Is_Valid(void) const { return Valid; }
    bool Match(const char* string) const { (void)string; return false; }

    RegularExpressionClass& operator=(const RegularExpressionClass& rhs) { (void)rhs; return *this; }
    bool operator==(const RegularExpressionClass& rhs) const { (void)rhs; return false; }
    bool operator!=(const RegularExpressionClass& rhs) const { (void)rhs; return true; }

private:
    bool Valid;
};

#endif // REGEXPR_H
