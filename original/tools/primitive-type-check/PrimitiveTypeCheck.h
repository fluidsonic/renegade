#pragma once
#include "clang-tidy/ClangTidyCheck.h"

namespace ccr {

/// Warns when a declaration uses a raw C++ built-in type instead of a
/// fixed-width alias ((u)intN_t, charN_t).
///
/// Allowed raw built-ins: bool, void, nullptr_t, char8_t, char16_t, char32_t,
/// float (size guaranteed 4 bytes), double (size guaranteed 8 bytes).
/// Everything else (int, char, long, short, …) is flagged.
/// Typedefs are NOT flagged — int32_t, size_t all pass through.
class PrimitiveTypeCheck : public clang::tidy::ClangTidyCheck {
public:
    PrimitiveTypeCheck(llvm::StringRef Name, clang::tidy::ClangTidyContext *Context)
        : ClangTidyCheck(Name, Context) {}

    void registerMatchers(clang::ast_matchers::MatchFinder *Finder) override;
    void check(const clang::ast_matchers::MatchFinder::MatchResult &Result) override;

private:
    /// Returns true if QT is a raw BuiltinType that should be flagged.
    /// Returns false for typedefs (even if the underlying type is an int).
    static bool isForbiddenRawBuiltin(clang::QualType QT);

    /// Reports a diagnostic if QT is forbidden and Loc is in Code/.
    void checkType(clang::QualType QT, clang::SourceLocation Loc,
                   const clang::ASTContext &Ctx) const;
};

} // namespace ccr
