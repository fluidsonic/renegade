#include "PrimitiveTypeCheck.h"
#include "clang-tidy/ClangTidyModule.h"
#include "clang-tidy/ClangTidyModuleRegistry.h"
#include "clang/AST/ASTContext.h"
#include "clang/ASTMatchers/ASTMatchFinder.h"
#include "clang/ASTMatchers/ASTMatchers.h"

using namespace clang;
using namespace clang::ast_matchers;

namespace ccr {

bool PrimitiveTypeCheck::isForbiddenRawBuiltin(QualType QT) {
    // Strip qualifiers but preserve sugar (typedefs).
    // If it is NOT directly a BuiltinType (e.g. it is a TypedefType), return false.
    const Type *T = QT.getTypePtr();
    const auto *BT = dyn_cast<BuiltinType>(T);
    if (!BT)
        return false;

    switch (BT->getKind()) {
        // ── Allowed raw built-ins ──────────────────────────────────────────
        case BuiltinType::Bool:     // bool
        case BuiltinType::Void:     // void
        case BuiltinType::NullPtr:  // nullptr_t
        case BuiltinType::Char8:    // char8_t  (C++20)
        case BuiltinType::Char16:   // char16_t (C++11)
        case BuiltinType::Float:    // float  (size asserted == 4 in floattypes.h)
        case BuiltinType::Double:   // double (size asserted == 8 in floattypes.h)
            return false;

        // ── Everything else is forbidden ───────────────────────────────────
        // char, signed char, unsigned char
        // short, unsigned short
        // int, signed, unsigned int, unsigned  (naked 'signed'/'unsigned' are int/unsigned int)
        // long, unsigned long
        // long long, unsigned long long
        // long double, char32_t
        // wchar_t (use char16_t for 2-byte UTF-16; wchar_t is 4 bytes on macOS)
        // __int128, __float128, etc.
        default:
            return true;
    }
}

static bool isInCodeDir(SourceLocation Loc, const SourceManager &SM) {
    if (Loc.isInvalid())
        return false;
    llvm::StringRef File = SM.getFilename(SM.getSpellingLoc(Loc));
    return File.contains("/Code/");
}

void PrimitiveTypeCheck::checkType(QualType QT, SourceLocation Loc,
                                   const ASTContext &Ctx) {
    if (!isInCodeDir(Loc, Ctx.getSourceManager()))
        return;
    if (!isForbiddenRawBuiltin(QT))
        return;

    const auto *BT = QT.getTypePtr()->castAs<BuiltinType>();

    switch (BT->getKind()) {
        // char family
        case BuiltinType::Char_S:   // char (signed)
        case BuiltinType::Char_U:   // char (unsigned, platform-dependent)
            diag(Loc, "use int8_t, uint8_t, or char8_t instead of 'char'; "
                      "'char' signedness is implementation-defined");
            break;
        case BuiltinType::SChar:    // signed char
            diag(Loc, "use int8_t instead of 'signed char'");
            break;
        case BuiltinType::UChar:    // unsigned char
            diag(Loc, "use uint8_t instead of 'unsigned char'");
            break;

        // short family
        case BuiltinType::Short:
            diag(Loc, "use int16_t instead of 'short'");
            break;
        case BuiltinType::UShort:
            diag(Loc, "use uint16_t instead of 'unsigned short'");
            break;

        // int family (including naked signed/unsigned)
        case BuiltinType::Int:
            diag(Loc, "use int32_t instead of 'int' or 'signed'");
            break;
        case BuiltinType::UInt:
            diag(Loc, "use uint32_t instead of 'unsigned int' or 'unsigned'");
            break;

        // long family
        case BuiltinType::Long:
            diag(Loc, "use int32_t or int64_t instead of 'long' "
                      "(width is 4 bytes on Windows, 8 bytes on macOS/Linux)");
            break;
        case BuiltinType::ULong:
            diag(Loc, "use uint32_t or uint64_t instead of 'unsigned long' "
                      "(width is 4 bytes on Windows, 8 bytes on macOS/Linux)");
            break;

        // long long family
        case BuiltinType::LongLong:
            diag(Loc, "use int64_t instead of 'long long'");
            break;
        case BuiltinType::ULongLong:
            diag(Loc, "use uint64_t instead of 'unsigned long long'");
            break;

        // wchar_t — most important: must use char16_t
        case BuiltinType::WChar_S:
        case BuiltinType::WChar_U:
            diag(Loc, "use char16_t instead of 'wchar_t'; "
                      "wchar_t is 4 bytes on macOS but the protocol requires 2-byte UTF-16");
            break;

        // long double
        case BuiltinType::LongDouble:
            diag(Loc, "use double instead of 'long double'");
            break;

        // char32_t
        case BuiltinType::Char32:
            diag(Loc, "use char16_t or uint32_t instead of 'char32_t'; "
                      "char32_t is not used in this codebase");
            break;

        // fallback for __int128, __float128, etc.
        default: {
            PrintingPolicy PP = Ctx.getPrintingPolicy();
            PP.SuppressTagKeyword = false;
            diag(Loc,
                 "use a fixed-width type instead of %0; "
                 "prefer (u)intN_t")
                << BT->getName(PP);
            break;
        }
    }
}

void PrimitiveTypeCheck::registerMatchers(MatchFinder *Finder) {
    // Variable declarations (local, global, static member)
    Finder->addMatcher(
        varDecl(unless(isExpansionInSystemHeader()),
                unless(isImplicit()))
            .bind("var"),
        this);

    // Struct / class field declarations
    Finder->addMatcher(
        fieldDecl(unless(isExpansionInSystemHeader()))
            .bind("field"),
        this);

    // Function parameters
    Finder->addMatcher(
        parmVarDecl(unless(isExpansionInSystemHeader()),
                    unless(isImplicit()))
            .bind("parm"),
        this);

    // Function return types
    Finder->addMatcher(
        functionDecl(unless(isExpansionInSystemHeader()),
                     unless(isImplicit()))
            .bind("func"),
        this);
}

void PrimitiveTypeCheck::check(const MatchFinder::MatchResult &Result) {
    const ASTContext &Ctx = *Result.Context;

    if (const auto *VD = Result.Nodes.getNodeAs<VarDecl>("var"))
        checkType(VD->getType(), VD->getTypeSpecStartLoc(), Ctx);

    if (const auto *FD = Result.Nodes.getNodeAs<FieldDecl>("field"))
        checkType(FD->getType(), FD->getTypeSpecStartLoc(), Ctx);

    if (const auto *PD = Result.Nodes.getNodeAs<ParmVarDecl>("parm"))
        checkType(PD->getType(), PD->getTypeSpecStartLoc(), Ctx);

    if (const auto *FuncD = Result.Nodes.getNodeAs<FunctionDecl>("func"))
        checkType(FuncD->getReturnType(),
                  FuncD->getReturnTypeSourceRange().getBegin(), Ctx);
}

} // namespace ccr

// ── Module registration for `clang-tidy --load` ────────────────────────────

class CcrModule : public clang::tidy::ClangTidyModule {
public:
    void addCheckFactories(clang::tidy::ClangTidyCheckFactories &Factories) override {
        Factories.registerCheck<ccr::PrimitiveTypeCheck>("ccr-primitive-type");
    }
};

static clang::tidy::ClangTidyModuleRegistry::Add<CcrModule>
    X("ccr-module", "CCR-specific checks: fixed-width type enforcement.");
