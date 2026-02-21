# C&C Renegade Client Setup (macOS)

## Overview

The game runs via [CrossOver](https://www.codeweavers.com/crossover) (Wine-based, supports 32-bit Windows apps on Apple Silicon).
Game files are stored in `./client/`, which is symlinked into CrossOver's bottle directory.

## Prerequisites

- CrossOver 26+ installed at `/Applications/CrossOver.app`
- Game downloaded from Steam (App ID: `2229890`) via DepotDownloader

## Directory Structure

```
client/                         ← Wine prefix (symlinked from CrossOver Bottles/Renegade)
└── drive_c/
    └── Program Files/
        └── Renegade/
            ├── Game.exe        ← main game executable
            ├── Renegade.exe    ← launcher (not used)
            └── ...
```

The CrossOver bottle symlink:
```
~/Library/Application Support/CrossOver/Bottles/Renegade → ./client/
```

## Initial Setup

### 1. Install CrossOver

Download from [codeweavers.com/crossover](https://www.codeweavers.com/crossover) and install to `/Applications/`.

### 2. Create the bottle

```bash
bash scripts/setup-client.sh
```

### 3. Download game files

```bash
brew tap steamre/tools
brew install depotdownloader

depotdownloader \
  -app 2229890 \
  -os windows \
  -username YOUR_STEAM_USERNAME \
  -dir "./client/drive_c/Program Files/Renegade"
```

### 4. Install DirectX

In CrossOver → Renegade bottle → Install Software → **DirectX for Modern Games**.

## Launching

Use the launch script:

```bash
./scripts/launch-client.sh
```

Or via CrossOver GUI: open CrossOver → Renegade bottle → Run Command → `C:\Program Files\Renegade\Game.exe`.
