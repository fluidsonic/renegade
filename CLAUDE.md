# Project: C&C Renegade Kotlin Server

## Build Commands

### Kotlin Server
- NEVER set JAVA_HOME or GRADLE_USER_HOME — user's shell has these configured
- Run: `/Users/marc/Documents/ccr/kotlin-server/gradlew -p /Users/marc/Documents/ccr/kotlin-server <task>`

### C++ Port (original/)
- Source root: `/Users/marc/Documents/ccr/original/`
- Build dir: `/Users/marc/Documents/ccr/original/build/`
- Build: `cmake --build /Users/marc/Documents/ccr/original/build --target Commando -j8`
- Configure (if build.ninja missing/stale or files deleted): `cmake -S /Users/marc/Documents/ccr/original -B /Users/marc/Documents/ccr/original/build -G Ninja`
- Run: `MallocNanoZone=0 /Users/marc/Documents/ccr/original/build/Code/Commando/Commando.app/Contents/MacOS/Commando`
- Build type: Debug with ASan (`-fsanitize=address` in root CMakeLists.txt); `MallocNanoZone=0` required on macOS

## Workflow Preferences
- Write all documentation to `/docs/*.md`. Update documentation as soon as year learn something new.
- Write every accepted plan to `/plans/*.md`. Write immediately once a plan was approved. Make that behavior part of each plan itself.
- Automatically update `/CLAUDE.md` as you learn new things to remember in every conversation.
- `/CLAUDE.md`, `/docs/*.md`, `/plans/*.md` can be written/edited in plan mode.
- Always use `<project root>/.tmp/` for temporary files (scripts, scratch files, etc.). Never use `/tmp` or `$TMPDIR`.
