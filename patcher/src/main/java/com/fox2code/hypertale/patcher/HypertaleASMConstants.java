/*
 * MIT License
 * 
 * Copyright (c) 2026 Fox2Code
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.fox2code.hypertale.patcher;

public interface HypertaleASMConstants {
	String LAMBDA_ARGS = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";
	int ASM_API = TransformerUtils.ASM_BUILD;
	// Java ASM Constants
	String ASMClass = "java/lang/Class";
	String ASMObject = "java/lang/Object";
	String StackTraceElement = "java/lang/StackTraceElement";
	String ASMString = "java/lang/String";
	String ASMStringBuilder = "java/lang/StringBuilder";
	String ASMCollection = "java/util/Collection";
	String ASMList = "java/util/List";
	String ASMSet = "java/util/Set";
	// Netty
	String Unpooled = "io/netty/buffer/Unpooled";
	// FastUtil ASM Constants
	String ObjectArrayList = "it/unimi/dsi/fastutil/objects/ObjectArrayList";
	String ObjectList = "it/unimi/dsi/fastutil/objects/ObjectList";
	// Hypixel FastUtil ASM constants
	String Long2ObjectConcurrentHashMap = "com/hypixel/fastutil/longs/Long2ObjectConcurrentHashMap";
	// Hytale ASM Constants
	String BuilderToolsPlugin = "com/hypixel/hytale/builtin/buildertools/BuilderToolsPlugin";
	String PluginIdentifier = "com/hypixel/hytale/common/plugin/PluginIdentifier";
	String HytaleLogger = "com/hypixel/hytale/logger/HytaleLogger";
	String HytaleLogFormatter = "com/hypixel/hytale/logger/backend/HytaleLogFormatter";
	String PacketIO = "com/hypixel/hytale/protocol/io/PacketIO";
	String JavaPlugin = "com/hypixel/hytale/server/core/plugin/JavaPlugin";
	String PluginBase = "com/hypixel/hytale/server/core/plugin/PluginBase";
	String PluginClassLoader = "com/hypixel/hytale/server/core/plugin/PluginClassLoader";
	String PluginManager = "com/hypixel/hytale/server/core/plugin/PluginManager";
	String PendingLoadPlugin = "com/hypixel/hytale/server/core/plugin/pending/PendingLoadPlugin";
	String PlayerRef = "com/hypixel/hytale/server/core/universe/PlayerRef";
	String World = "com/hypixel/hytale/server/core/universe/world/World";
	String FloodLightCalculation = "com/hypixel/hytale/server/core/universe/world/lighting/FloodLightCalculation";
	String FullBrightLightCalculation = "com/hypixel/hytale/server/core/universe/world/lighting/FullBrightLightCalculation";
	String ChunkStore = "com/hypixel/hytale/server/core/universe/world/storage/ChunkStore";
	String IndexedStorageChunkStorageProvider = "com/hypixel/hytale/server/core/universe/world/storage/provider/IndexedStorageChunkStorageProvider";
	String IndexedStorageChunkSaver = "com/hypixel/hytale/server/core/universe/world/storage/provider/IndexedStorageChunkStorageProvider$IndexedStorageChunkSaver";
	String WorldMapManager = "com/hypixel/hytale/server/core/universe/world/worldmap/WorldMapManager";
	String DumpUtil = "com/hypixel/hytale/server/core/util/DumpUtil";
	// Hypertale ASM Constants
	String HypertaleRecyclableByteBuf = "com/fox2code/hypertale/io/HypertaleRecyclableByteBuf";
	String HypertaleConfig = "com/fox2code/hypertale/loader/HypertaleConfig";
	String HypertaleModLoader = "com/fox2code/hypertale/loader/HypertaleModLoader";
	String HypertaleBasePlugin = "com/fox2code/hypertale/plugin/HypertaleBasePlugin";
	String HypertaleJavaPlugin = "com/fox2code/hypertale/plugin/HypertaleJavaPlugin";
	String HypertalePlayerRef = "com/fox2code/hypertale/universe/HypertalePlayerRef";
	String HypertaleWorld = "com/fox2code/hypertale/universe/HypertaleWorld";
	String EmptyArrays = "com/fox2code/hypertale/utils/EmptyArrays";
}
