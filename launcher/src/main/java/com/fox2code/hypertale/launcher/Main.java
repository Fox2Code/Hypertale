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
package com.fox2code.hypertale.launcher;

import com.fox2code.hypertale.io.HypertaleData;
import com.fox2code.hypertale.loader.HypertaleCompatibility;
import com.fox2code.hypertale.loader.HypertaleConfig;
import com.fox2code.hypertale.loader.HypertaleModGatherer;
import com.fox2code.hypertale.loader.HypertaleModLoader;
import com.fox2code.hypertale.patcher.PatcherMain;
import com.fox2code.hypertale.patcher.mixin.MixinLoader;
import com.fox2code.hypertale.utils.*;
import com.hypixel.hytale.LateMain;
import com.hypixel.hytale.logger.backend.HytaleConsole;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;

public final class Main {
	public static void hypertaleInitV1Main(String[] args) throws Throwable {
		main(args);
	}

	static void main(String[] args) throws IOException, InterruptedException {
		Locale.setDefault(Locale.ENGLISH);
		System.setProperty("java.awt.headless", "true");
		System.setProperty("file.encoding", "UTF-8");
		HypertaleAgent.tryLoadEarlyAgent();
		System.setProperty("rellatsnI.tnega.yddubetyb.ten", HypertaleAgent.class.getName());
		if (Boolean.getBoolean("hypertale.gradleInit")) {
			System.setProperty("hypertale.initMethod", "gradle");
		} else if (!Boolean.getBoolean("hypertale.useInitWrapper")) {
			System.setProperty("hypertale.initMethod", "direct");
		}
		MainPlus.setEditionProperties();
		if (args.length == 0 && !isRunningFromTerminal()) {
			// Avoid running a server from a double click.
			System.out.println("[Hypertale] File double click detected! Skipping running!");
			System.out.println("[Hypertale] Append \"--\" as an argument to skip this check!");
			return;
		}
		boolean dev;
		if (args.length == 1 && "--".equals(args[0])) {
			args = EmptyArrays.EMPTY_STRING_ARRAY;
		} else if (args.length >= 1 && "--version".equals(args[0])) {
			System.out.println("Hypertale version " + BuildConfig.HYPERTALE_VERSION);
			return;
		} else if (args.length == 1 && "--download-libraries".equals(args[0])) {
			for (DependencyHelper.Dependency dependency : DependencyHelper.patcherDependencies) {
				DependencyHelper.loadDependency(dependency);
			}
			return;
		} else if (args.length == 1 && "--run-patcher".equals(args[0])) {
			try {
				EarlyLogger.start(true);
				if (!HypertalePaths.hypertaleCache.isDirectory() &&
						!HypertalePaths.hypertaleCache.mkdirs()) {
					throw new IOException("Failed to create hypertale folder!");
				}
				HypertaleConfig.load();
				HypertaleModGatherer modGatherer = HypertaleModGatherer.gatherMods(EmptyArrays.EMPTY_STRING_ARRAY);
				for (DependencyHelper.Dependency dependency : DependencyHelper.patcherDependencies) {
					DependencyHelper.loadDependency(dependency);
				}
				for (File mod : modGatherer.getMods()) {
					DependencyHelper.addFileToClasspath(mod);
				}
				for (File mod : modGatherer.getLibraries()) {
					DependencyHelper.addFileToClasspath(mod);
				}
				File input = HypertalePaths.getHytaleJar();
				if (HypertalePaths.hypertalePrePatcher.isFile()) {
					JarFile prePatcher = new JarFile(HypertalePaths.hypertalePrePatcher);
					String mainClass = prePatcher.getManifest().getMainAttributes().getValue("Main-Class");
					if (mainClass == null) {
						mainClass = StreamSupport.stream(((Iterable<JarEntry>)() ->
								prePatcher.entries().asIterator()).spliterator(), false)
								.map(ZipEntry::getName).filter(path -> path.endsWith("Patcher.class"))
								.findFirst().orElse(null);
						if (mainClass != null) {
							mainClass = mainClass.substring(0, mainClass.length() - 6).replace('/', '.');
						}
					}
					prePatcher.close();
					if (mainClass != null) {
						DependencyHelper.addFileToClasspath(HypertalePaths.hypertalePrePatcher);
						try {
							Method method = Class.forName(mainClass).getDeclaredMethod("main", String[].class);
							if (!Modifier.isPublic(method.getModifiers())) {
								method.setAccessible(true);
							}
							try {
								EarlyLogger.log("Pre-patching the game with " + mainClass);
								method.invoke(null, (Object) new String[]{input.getAbsolutePath(),
										HypertalePaths.hypertalePrePatched.getAbsolutePath()});
								if (HypertalePaths.hypertalePrePatched.isFile()) {
									input = HypertalePaths.hypertalePrePatched;
								}
							} catch (Exception e) {
								throw new Error("Failed to pre-patch from " + mainClass, e);
							}
						} catch (Exception e) {
							EarlyLogger.log("Failed to initialize pre-patcher:\n" +
									StackTraceStringifier.stringifyStackTrace(e));
						}
					} else {
						EarlyLogger.log("Found pre-patcher but failed to find patching class!");
					}
				}
				DependencyHelper.addFileToClasspath(input);
				PatcherMain.patch(input, HypertalePaths.hypertaleCacheJar, true, true);
				if (HypertalePaths.hypertalePrePatched.exists() && !HypertalePaths.hypertalePrePatched.delete()) {
					HypertalePaths.hypertalePrePatched.deleteOnExit();
				}
				EarlyLogger.log("HytaleServer patched successfully!");
			} finally {
				EarlyLogger.stop();
			}
			return;
		} else if (args.length == 3 &&
				((dev = "--run-patcher-dev".equals(args[0])) || "--run-patcher".equals(args[0]))) {
			final File input = new File(args[1]);
			DependencyHelper.addFileToClasspath(input);
			for (DependencyHelper.Dependency dependency : DependencyHelper.patcherDependencies) {
				DependencyHelper.loadDependency(dependency);
			}
			PatcherMain.devMode = dev;
			PatcherMain.patch(input, new File(args[2]), true, true);
			return;
		} else if (args.length == 1 && "--launch-dev".equals(args[0])) {
			EarlyLogger.start(false);
			EarlyLogger.log("Version " + BuildConfig.HYPERTALE_VERSION);
			launchGame(EmptyArrays.EMPTY_STRING_ARRAY, true);
			return;
		} else if (args.length == 1 && "--patch-class-path".equals(args[0])) {
			if (Boolean.getBoolean("hypertale.premium")) {
				EarlyLogger.start(false);
				MainPlus.patchAsClassPath();
			} else {
				System.out.println("This feature is only available on premium builds of Hypertale!");
			}
			return;
		}
		EarlyLogger.start(false);
		EarlyLogger.log("Version " + BuildConfig.HYPERTALE_VERSION);
		File hytaleJar = HypertalePaths.getHytaleJar();
		if (!hytaleJar.exists()) {
			EarlyLogger.log("Cannot find original HytaleServer.jar");
			return;
		}
		// TODO: Detect singleplayer mode
		HypertaleConfig.load();
		HypertaleData cachedData = null;
		if (HypertalePaths.hypertaleCacheJar.exists() &&
				HypertalePaths.hypertaleCacheData.exists()) {
			try {
				cachedData = new HypertaleData(HypertalePaths.hypertaleCacheData);
			} catch (IOException ioe) {
				EarlyLogger.log("Invalid cache, ignoring...");
			}
		}
		HypertaleModGatherer modGatherer = HypertaleModGatherer.gatherMods(args);
		HypertaleData actualData = new HypertaleData();
		actualData.hypertaleJarSize = HypertalePaths.hypertaleJar.length();
		actualData.originalJarSize = hytaleJar.length();
		actualData.modifiedJarSize = HypertalePaths.hypertaleCacheJar.length();
		actualData.patchConfigFlags = HypertaleConfig.patchConfigFlags();
		actualData.modHash = modGatherer.getModHash();
		actualData.prePatcherSize = HypertalePaths.hypertalePrePatcher.isFile() ?
				HypertalePaths.hypertalePrePatcher.length() : 0;
		if (args.length == 1 && "--dry".equals(args[0])) {
			return;
		}
		if (!actualData.equals(cachedData)) {
			runPatcher(actualData);
		}
		if (args.length == 1 && "--noop".equals(args[0])) {
			return;
		}
		if (HypertaleConfig.hyperOptimizeClassPath()) {
			MainPlus.launchPatchedAsClassPath(args);
			return;
		} else if (HypertaleConfig.premiumHyperOptimizeClassPath) {
			EarlyLogger.log("HyperOptimizeClassPath is only supported on premium builds of Hypertale!");
		}
		try {
			launchGame(args, false);
		} catch (LinkageError e) {
			if (HypertalePaths.hypertaleCacheData.exists() &&
					!HypertalePaths.hypertaleCacheData.delete()) {
				HypertalePaths.hypertaleCacheData.deleteOnExit();
			}
			throw e;
		}
	}

	private static void launchGame(String[] args, boolean dev) throws IOException {
		HypertaleModGatherer modGatherer = dev ?
				HypertaleModGatherer.gatherModsDev() :
				HypertaleModGatherer.gatherMods(args);
		if (args.length == 0) {
			if (HypertalePaths.hytaleAssets.exists()) {
				args = new String[]{"--assets", "Assets.zip"};
			} else {
				File vanillaAssets = new File(
						HypertalePlatform.getPlatform().getLatestFolder(HypertaleConfig.hytaleBranch), "Assets.zip");
				if (vanillaAssets.exists()) {
					args = new String[]{"--assets", vanillaAssets.getAbsolutePath()};
				}
			}
		}
		if (HypertalePaths.hypertaleCacheJar.exists()) {
			DependencyHelper.addFileToClasspath(HypertalePaths.hypertaleCacheJar);
		} else if (!dev) {
			throw new FileNotFoundException("Failed to get ./.hypertale/HytaleServer.jar");
		}
		for (DependencyHelper.Dependency dependency : DependencyHelper.patcherDependencies) {
			DependencyHelper.loadDependency(dependency);
		}
		MixinLoader.preInitializeMixin();
		PatchHelper.install();
		if (modGatherer.getModSyncBootstrap() != null &&
				// Don't run modSyncBootstrap twice if init already called it!
				!Boolean.getBoolean("hypertale.modSyncBootstrapInit")) {
			try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{
					modGatherer.getModSyncBootstrap().toURI().toURL()})) {
				Class.forName(HypertaleCompatibility.classModSyncBootstrap, true, urlClassLoader);
			} catch (LinkageError | ClassNotFoundException _) {}
		}
		MixinLoader.initialize();
		HypertaleModLoader.loadHypertaleMods(modGatherer);
		MixinLoader.postInitialize();
		startHytale(args);
	}

	static void startHytale(String... args) {
		if (!EarlyLogger.isDirectLogging()) {
			throw new IllegalStateException("Game looks already launched!");
		}
		if (HypertalePaths.hypertaleCacheJust.isFile() &&
				!HypertalePaths.hypertaleCacheJust.delete()) {
			EarlyLogger.log("Failed to delete \".hypertale/.just\"");
		}
		EarlyLogger.log("Launching Hytale...");
		EarlyLogger.stop();
		// We load Hytale is the same classloader! So we can do that!
		if (HypertalePlatform.getPlatform() != HypertalePlatform.WINDOWS && isRunningFromTerminal()) {
			HytaleConsole.INSTANCE.setTerminal("ansi");
		}
		LateMain.lateMain(args);
	}

	private static void runPatcher(HypertaleData actualData) throws IOException, InterruptedException {
		EarlyLogger.log("Patching HytaleServer...");
		execSelf("--run-patcher");
		actualData.modifiedJarSize = HypertalePaths.hypertaleCacheJar.length();
		actualData.writeTo(HypertalePaths.hypertaleCacheData);
	}

	public static void execSelf(String arg) throws IOException, InterruptedException {
		if (!EarlyLogger.isDirectLogging()) {
			throw new IllegalStateException("Invalid state!");
		}
		EarlyLogger.stop();
		ArrayList<String> command = new ArrayList<>();
		if (System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win")) {
			command.add(new File(System.getProperty("java.home") + "\\bin\\javaw.exe").getAbsolutePath());
		} else {
			command.add(new File(System.getProperty("java.home") + "/bin/java").getAbsolutePath());
		}
		command.add("-jar");
		command.add(HypertalePaths.getHypertaleExecJar().getAbsolutePath());
		command.add(arg);
		int returnCode = new ProcessBuilder(command).inheritIO().start().waitFor();
		if (returnCode != 0 || !HypertalePaths.hypertaleCacheJar.exists()) {
			throw new IOException("Patch failed with return code is " + returnCode);
		}
		EarlyLogger.start(true);
	}

	// https://errorprone.info/bugpattern/SystemConsoleNull
	@SuppressWarnings("SystemConsoleNull")
	private static boolean isRunningFromTerminal() {
		Console console = System.console();
		return console != null && console.isTerminal();
	}
}
