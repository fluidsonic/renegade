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
        case BuiltinType::Char32:   // char32_t (C++11)
        case BuiltinType::Float:    // float  (size asserted == 4 in floattypes.h)
        case BuiltinType::Double:   // double (size asserted == 8 in floattypes.h)
            return false;

        // ── Everything else is forbidden ───────────────────────────────────
        // char, signed char, unsigned char
        // short, unsigned short
        // int, unsigned int
        // long, unsigned long
        // long long, unsigned long long
        // long double, wchar_t
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
                                   const ASTContext &Ctx) const {
    if (!isInCodeDir(Loc, Ctx.getSourceManager()))
        return;
    if (!isForbiddenRawBuiltin(QT))
        return;

    const auto *BT = QT.getTypePtr()->castAs<BuiltinType>();
    PrintingPolicy PP = Ctx.getPrintingPolicy();
    PP.SuppressTagKeyword = false;

    diag(Loc,
         "use a fixed-width type instead of %0; "
         "prefer (u)intN_t or charN_t")
        << BT->getName(PP);
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
