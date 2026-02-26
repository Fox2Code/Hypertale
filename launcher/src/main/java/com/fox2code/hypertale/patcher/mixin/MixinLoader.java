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
package com.fox2code.hypertale.patcher.mixin;

import com.bawnorton.mixinsquared.MixinSquaredBootstrap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.hypixel.hytale.logger.backend.HytaleLogManager;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;

public final class MixinLoader {
	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private static final HashMap<String, String> activeConfigurations = new HashMap<>();
	private static final HashMap<String, String> mixinPackages = new HashMap<>();
	private static final HashSet<String> modWithMixins = new HashSet<>();
	private static boolean preInitialized = false, initialized = false, postInitialized = false;
	private static IMixinTransformer mixinTransformer;
	private static final HashSet<String> mixinExclusions = new HashSet<>();

	static {
		// Hypertale + Mixin system packages that must be excluded from mixin processing
		mixinExclusions.add("com.fox2code.hypertale.");
		mixinExclusions.add("org.objectweb.asm.");
		mixinExclusions.add("org.spongepowered.asm.");
		mixinExclusions.add("com.google.gson.");
		mixinExclusions.add("com.build_9.hyxin.");
		// Hytale classes that have miscellaneous reason to be excluded from mixin processing
		mixinExclusions.add("org.bouncycastle.");
		mixinExclusions.add("com.hypixel.hytale.plugin.early.");
	}

	private MixinLoader() {}

	public static void preInitializeMixin() {
		if (preInitialized) throw new IllegalStateException("Duplicate call to initializeMixin");
		// Must set HytaleLogManager before installing the mixin transformer
		System.setProperty("java.util.logging.manager", HytaleLogManager.class.getName());
		System.setProperty("mixin.bootstrapService", MixinBootstrapService.class.getName());
		System.setProperty("mixin.service", MixinService.class.getName());
		MixinBootstrap.init();
		MixinEnvironment.getCurrentEnvironment()
				.setOption(MixinEnvironment.Option.DISABLE_REFMAP, true);
		MixinEnvironment.getCurrentEnvironment()
				.setOption(MixinEnvironment.Option.DEBUG_INJECTORS, true);
		MixinEnvironment.getCurrentEnvironment()
				.setOption(MixinEnvironment.Option.DEBUG_VERBOSE, true);
		for (MixinEnvironment.Phase phase : new MixinEnvironment.Phase[]{
				MixinEnvironment.Phase.PREINIT, MixinEnvironment.Phase.INIT, MixinEnvironment.Phase.DEFAULT}) {
			MixinEnvironment.getEnvironment(phase).setSide(MixinEnvironment.Side.SERVER);
		}
		MixinBootstrap.getPlatform().inject();
		mixinTransformer = // Inject mixin transformer into the class loader.
				(IMixinTransformer) MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
		com.build_9.hyxin.mixin.MixinService.transformer = mixinTransformer; // Hyxin support
		MixinExtrasBootstrap.init();
		MixinSquaredBootstrap.init();
		preInitialized = true;
	}

	public static void initialize() {
		if (initialized) throw new IllegalStateException("Duplicate call to initialize!");
		if (!preInitialized) throw new IllegalStateException("Mixins are not pre initialized!");
		initialized = true;
		((MixinService) org.spongepowered.asm.service.MixinService.getService()).onStartup();
		if (MixinEnvironment.getEnvironment(MixinEnvironment.Phase.DEFAULT) != MixinEnvironment.getCurrentEnvironment())
			throw new Error("Mixin phase mismatch");
	}

	public static void postInitialize() {
		if (postInitialized) throw new IllegalStateException("Duplicate call to postInitialize!");
		if (!initialized) throw new IllegalStateException("Mixins are not initialized!");
		postInitialized = true;
	}

	public static void addMixinConfigurationSafe(
			String modId, String mixin) {
		if (!initialized) {
			throw new IllegalStateException("Trying to use Mixin service before it has been initialized");
		}
		if (postInitialized) {
			throw new IllegalStateException("Trying to register a mixin after the game has started");
		}
		if (mixin == null || modId == null) {
			throw new IllegalArgumentException("Null mixin or modId");
		}
		String oldModId = activeConfigurations.get(mixin);
		if (oldModId == null) {
			String mixinPackage = null;
			boolean hasMixins = false;
			try (InputStream resource = MixinLoader.class.getClassLoader().getResourceAsStream(mixin)) {
				if (resource != null) {
					JsonObject jsonObject = gson.fromJson(
							new InputStreamReader(resource, StandardCharsets.UTF_8), JsonObject.class);
					if (jsonObject.has("package")) {
						mixinPackage = jsonObject.get("package").getAsString();
					}
					if (jsonObject.has("plugin")) {
						hasMixins = true;
					}
					if (jsonObject.has("mixins") && !hasMixins) {
						hasMixins = !jsonObject.get("mixins").getAsJsonArray().isEmpty();
					}
				} else {
					throw new IllegalArgumentException("Mixin configuration not found: " + mixin);
				}
			} catch (Exception e) {
				throw new RuntimeException("Failed to read mixin configuration: " + mixin, e);
			}
			if (mixinPackage != null) {
				if (!mixinPackage.endsWith(".")) {
					mixinPackage += ".";
				}
				String oldMixin = mixinPackages.putIfAbsent(mixinPackage, mixin);
				if (oldMixin != null) {
					throw new IllegalStateException("Mixin package '" + mixinPackage + "' is already used by configuration: " + oldMixin);
				}
				activeConfigurations.put(mixin, modId);
				Mixins.addConfiguration(mixin, null);

				if (hasMixins) {
					modWithMixins.add(modId);
				}
				System.out.println("Loaded mixin: " + mixin);
				// Used for spark compatibility
				Mixins.getConfigs().stream().filter(config1 ->
						config1.getName().equals(mixin)).findFirst().ifPresent(config -> {
					config.getConfig().decorate(FabricUtil.KEY_MOD_ID, modId);
					config.getConfig().decorate("foxLoader.modId", modId);
				});
			} else {
				throw new IllegalArgumentException("Mixin configuration '" + mixin + "' has no package defined");
			}
		} else if (!modId.equals(oldModId)) {
			throw new IllegalStateException("Mixin configuration '" + mixin + "' is already registered by mod: " + oldModId);
		}
	}

	public static byte[] transformClass(String name, byte[] bytes) {
		if (isMixinExcluded(name)) return bytes;
		return mixinTransformer.transformClass(MixinEnvironment.getDefaultEnvironment(), name, bytes);
	}

	public static boolean isPostInitialized() {
		return postInitialized;
	}

	public static boolean modHasMixins(String modId) {
		return modWithMixins.contains(modId);
	}

	static boolean isMixinExcluded(String className) {
		for (String mixinPackage : mixinExclusions) {
			if (className.startsWith(mixinPackage)) {
				return true;
			}
		}
		return false;
	}

	static void addMixinExclusion(String packageName) {
		if (packageName == null || packageName.startsWith(".") ||
				!packageName.endsWith(".") || packageName.indexOf('/') != -1) {
			throw new IllegalArgumentException("Invalid package name: " + packageName);
		}
		if (packageName.startsWith("com.hypixel.hytale.") ||
				"com.hypixel.hytale.".startsWith(packageName)) {
			throw new IllegalArgumentException("Cannot exclude hytale itself: " + packageName);
		}
		if (!isMixinExcluded(packageName)) {
			mixinExclusions.add(packageName);
		}
	}
}
