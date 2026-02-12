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
package com.fox2code.hypertale.loader;

import com.fox2code.hypertale.launcher.DependencyHelper;
import com.fox2code.hypertale.launcher.EarlyLogger;
import com.fox2code.hypertale.launcher.HypertaleAgent;
import com.fox2code.hypertale.patcher.mixin.MixinLoader;
import com.fox2code.hypertale.utils.JsonPropertyHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.plugin.PluginManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class HypertaleModLoader {
	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private static final LinkedHashMap<File, JsonObject> loadedModsLate = new LinkedHashMap<>();
	private static final HashSet<String> preloadedPlugins = new HashSet<>();

	static {
		preloadedPlugins.add("Hypertale:Hypertale");
	}

	public static void loadHypertaleMods(HypertaleModGatherer modGatherer) throws IOException {
		LinkedHashMap<File, JsonObject> loadedModsEarly = new LinkedHashMap<>();
		// Load class path mods, allow dev mode to load the mod in development
		for (HypertaleModGatherer.ClassPathModCandidate candidate : modGatherer.getClassPathManifests()) {
			JsonObject jsonObject;
			try (InputStreamReader inputStreamReader = new InputStreamReader(
					candidate.manifest().openStream(), StandardCharsets.UTF_8)) {
				jsonObject = gson.fromJson(inputStreamReader, JsonObject.class);
			}
			processJsonAsMod(loadedModsEarly, candidate.file(), jsonObject, false, true);
		}
		loadHypertaleMods(modGatherer, false, loadedModsEarly);
		loadHypertaleMods(modGatherer, true, loadedModsEarly);
		loadMods(loadedModsEarly, true);
	}

	public static void loadHypertaleMods(
			HypertaleModGatherer modGatherer, boolean libraries,
			LinkedHashMap<File, JsonObject> loadedModsEarly) throws IOException {
		for (File file : (libraries ? modGatherer.getLibraries() : modGatherer.getHypertaleMods())) {
			try (JarFile jarFile = new JarFile(file)) {
				JarEntry hypertaleModInfo = jarFile.getJarEntry("manifest.json");
				if (hypertaleModInfo == null) continue;
				JsonObject jsonObject;
				try (InputStreamReader inputStreamReader = new InputStreamReader(
						jarFile.getInputStream(hypertaleModInfo), StandardCharsets.UTF_8)) {
					jsonObject = gson.fromJson(inputStreamReader, JsonObject.class);
				} catch (JsonParseException jsonParseException) {
					EarlyLogger.log("Failed to load mod info of " + file.getName());
					continue;
				}
				processJsonAsMod(loadedModsEarly, file, jsonObject, libraries, false);
			}
		}
	}

	private static void processJsonAsMod(LinkedHashMap<File, JsonObject> loadedModsEarly, File file,
										 JsonObject jsonObject, boolean isLibrary, boolean isClassPath)
			throws IOException {
		String id = JsonPropertyHelper.getBoolean(jsonObject, "Group") + ":" +
				JsonPropertyHelper.getBoolean(jsonObject, "Name");
		if (JsonPropertyHelper.getBoolean(jsonObject, "HypertalePreLoad", isLibrary)) {
			if (!isClassPath) {
				DependencyHelper.addFileToClasspath(file);
			}
			loadedModsEarly.put(file, jsonObject);
			if (!preloadedPlugins.add(id)) {
				throw new IllegalArgumentException("Tried to pre-load duplicate plugin");
			}
		} else {
			loadedModsLate.put(file, jsonObject);
		}
	}

	static void loadModsLate() {
		loadMods(loadedModsLate, false);
	}

	private static void loadMods(LinkedHashMap<File, JsonObject> loadedMods, boolean early) {
		Instrumentation instrumentation = HypertaleAgent.getInstrumentation();
		for (Map.Entry<File, JsonObject> entry : loadedMods.entrySet()) {
			JsonObject jsonObject = entry.getValue();
			String modIdentifier = JsonPropertyHelper.getString(jsonObject, "Group") +
					":" + JsonPropertyHelper.getString(jsonObject, "Name");
			String javaAgent = JsonPropertyHelper.getString(jsonObject, "HypertaleJavaAgent");
			if (javaAgent != null && instrumentation != null) {
				try {
					Class<?> agentClass;
					agentClass = Class.forName(javaAgent, true, early ?
							HypertaleModLoader.class.getClassLoader() :
							PluginManager.get().getBridgeClassLoader());
					agentClass.getDeclaredMethod(
									early ? "premain" : "agentmain",
									String.class, Instrumentation.class)
							.invoke(null, "", instrumentation);
				} catch (Exception e) {
					EarlyLogger.log("Failed to load java-agent " + javaAgent +
							" from " + entry.getKey().getName());
				}
			}
			String mixin = JsonPropertyHelper.getString(jsonObject, "HypertaleMixinConfig");
			if (mixin != null) {
				MixinLoader.addMixinConfigurationSafe(modIdentifier, mixin);
			}
		}
	}

	public static boolean isPreloadedPlugin(PluginIdentifier pluginIdentifier) {
		return preloadedPlugins.contains(pluginIdentifier.toString());
	}
}
