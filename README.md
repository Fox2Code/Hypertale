<p align="center">
  <img src="launcher/src/main/resources/Common/UI/Custom/Hypertale_Hypertale_Banner.png" alt="Hypertale Banner">
</p>

# Hypertale

**Hypertale** is a high-performance optimization mod and API extension toolkit for the **Hytale** (Early Access, Jan 13, 2026) server.
It leverages advanced bytecode manipulation to reduce memory pressure, increase TPS stability, and provide an extended set of APIs for the next generation Hytale mods.

**Downloads**: [GitHub Releases](https://github.com/Fox2Code/Hypertale/releases) | [ModTale](https://modtale.net/mod/hypertale) | [Orbis](https://www.orbis.place/mod/hypertale)

###### Note: You can download the project from multiple sources if you like it! \:D

---

## Installation

### Quick Start
For most users, grab the latest version from the [GitHub Releases](https://github.com/Fox2Code/Hypertale/releases) page,
and drop it into your `mods` folder.

### Smart Installation
Hypertale features a "Smart-Installation" system:
*   Uses a JAR-replacement system to safely patch the server before it is even running.
*   If it fails or is unavailable, it automatically leverages the Hytale `earlyplugins` system for seamless integration.

### Build from Source
If you prefer to build it yourself, Hypertale automatically locates your local Hytale installation to gather necessary dependencies.
1. Install git and OpenJDK 25.
2. Clone the repository.
3. Run `./gradlew build`.
4. Find your jar in `launcher/build/libs/Hypertale-<version>.jar`.

---

## Key Features

### High-Performance Optimization
Hypertale targets the core bottlenecks of the Hytale server, specifically focusing on **Memory Pressure** (the rate of memory allocation). High allocation rates trigger frequent Java Garbage Collection (GC) pauses, causing "stuttering" even on powerful hardware.

Hypertale provides two levels of optimization controlled via `.hypertale/hypertale.ini`:
*   **Regular Optimization** (`optimizePluginOnlyAPIs=true`): Safe, high-efficiency implementations that maintain 100% compatibility with the standard Hytale API.
*   **Aggressive Optimization** (`aggressivelyOptimizePluginOnlyAPIs=true`): Extreme performance paths that may change API semantics (e.g., returning internal lists instead of copies).

### "Transparency-First" Developer Tools
Modding a closed-source game shouldn't be a guessing game. The `dev` module transforms Hypertale into a comprehensive development platform:
*   **Transparency-First Patching**: In developer mode, Hypertale injects **all three code paths** (Vanilla, Regular, and Aggressive) into patched methods. Open them in the decompiler to see how they work.
*   **Reverse Engineering Suite**:
    *   **Vineflower Decompiler**: Integrated and tuned specifically for Hytale's bytecode.
    *   **HypertaleUnpicker**: Automatically resolves inlined constants into readable references.
*   **Auto-Workspace Setup**: The Gradle plugin automatically locates your Hytale installation, extracts dependencies, and configures your IDE.


## Modding with Hypertale

### `manifest.json` Extensions
Hypertale reads custom fields in your mod's manifest to enable advanced features:

| Field                    | Type      | Description                                                                                                |
|:-------------------------|:----------|:-----------------------------------------------------------------------------------------------------------|
| `HypertalePreLoad`       | `boolean` | If `true`, Hypertale will load your mod in the same ClassLoader as the Hytale server (bypasses isolation). |
| `HypertaleJavaAgent`     | `string`  | Specify a class to be loaded as a Java Agent for instrumentation.                                          |
| `HypertaleMixinConfig`   | `string`  | Path to a Mixin configuration file to be applied to the server.                                            |
| `HypertaleServerVersion` | `string`  | Set a specific server version compatibility (supports `*` for any).                                        |

### API Extensions
Hypertale adds the `.hypertale()` extension to core Hytale classes, providing access to optimized methods and new functionality:
*   `com.hypixel.hytale.server.core.entity.entities.Player`
*   `com.hypixel.hytale.server.core.plugin.PluginBase`
*   `com.hypixel.hytale.server.core.universe.PlayerRef`
*   `com.hypixel.hytale.server.core.universe.world.World`

You can also check out [the official Hypertale Javadoc](https://fox2code.com/javadoc/hypertale/)

---

## Compatibility & Support

Hypertale includes custom compatibility or official support for:
*   **ModSync**: Full bootstrap support.
*   **Mod List Mods**: Integrated icon support.
*   [**Spark**](https://github.com/lucko/spark): Extensively tested with Spark for performance profiling.
*   [**Hyxin**](https://github.com/Build-9/Hyxin): Support to load Hyxin mods is built-in into Hypertale!

**Example Mod**: [HypertaleExampleMod](https://github.com/Fox2Code/HypertaleExampleMod)

---

## License
Hypertale is licensed under the [MIT License](LICENSE).