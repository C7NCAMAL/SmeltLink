> **NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.**

# SmeltLink for Minecraft

SmeltLink is a high-performance, quality-of-life connected furnaces mod for Minecraft Java Edition built on NeoForge 26.3.

SmeltLink connects adjacent furnaces in a coplanar wall into an active cluster, allowing players to manage fuel, inputs, and outputs across multiple furnaces through a single unified interface.

---

## Features

- **Connected Furnace Walls**: Automatically connects adjacent furnaces of the same type (Regular Furnace, Smoker, Blast Furnace) facing the same direction on a 2D plane.
- **Homogeneous Clusters**: Respects furnace variants — Furnaces only connect to Furnaces, Smokers to Smokers, and Blast Furnaces to Blast Furnaces.
- **Smart UI Flow**:
  - **Normal Right-Click**: Opens the individual furnace GUI by default.
  - **Sneak + Right-Click**: Opens the unified SmeltLink cluster GUI (unless holding placeable blocks, preserving normal block placement).
  - **Dynamic Toggle Button**: Switch between Single Mode (`⛓ xN`) and Share Mode (`⚡ xN`) at any time directly from the interface.
- **Cursor Position Preservation**: Seamlessly transitions between Single Furnace and Share Mode screens without jumping or centering the mouse cursor.
- **Capacity Scaling ($64 \times N$)**: Input and fuel slots dynamically scale capacity up to 64 items per connected furnace, with true counts synced to the client.
- **Balanced Two-Pass Distribution**: Items are distributed across active furnaces using balanced round-robin sharing followed by waterfall filling.
- **Fast Scroll Wheel Cluster Resizing**: Scroll while hovering over the GUI to adjust active linked furnace count (±4 per notch, Shift for ±8, Ctrl for ±1) with tactile audio feedback.
- **Creative Mode Cloning**: Full support for middle-click (`CLONE`) on aggregate slots in Creative Mode.
- **Q-Key Item Dropping**: Drop single items (`Q`) or full stacks (`Ctrl+Q`) directly from aggregate slots, with accumulated smelting experience awarded upon output drop.

---

## Requirements

- **Minecraft**: 26.3
- **Mod Loader**: NeoForge 26.3.0.76 or newer
- **Java**: Java 25 or newer

---

## Installation

1. Install [NeoForge](https://neoforged.net/) for Minecraft 26.3.
2. Download the latest `smeltlink-<version>.jar` release.
3. Place the `.jar` file into your Minecraft `.minecraft/mods` folder.
4. Launch Minecraft using the NeoForge profile.

---

## Configuration

Configuration options can be customized in `config/smeltlink-local.toml`:
- `maxClusterSize` (default: 64, range: 1–256): Maximum number of connected furnaces in a single cluster.
- `preserveCursorPosition` (default: true): Retains exact mouse cursor coordinates when switching UI modes.

---

## License

This project is licensed under the [MIT License](LICENSE).
