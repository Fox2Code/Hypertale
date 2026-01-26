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
import com.fox2code.hypertale.loader.HypertaleConfig;
import com.fox2code.hypertale.loader.ModGatherer;
import com.fox2code.hypertale.loader.ModLoader;
import com.fox2code.hypertale.patcher.Optimizer;
import com.fox2code.hypertale.patcher.PatcherMain;
import com.fox2code.hypertale.utils.*;
import com.hypixel.hytale.LateMain;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
				ModGatherer modGatherer = ModGatherer.gatherMods(EmptyArrays.EMPTY_STRING_ARRAY);
				for (DependencyHelper.Dependency dependency : DependencyHelper.patcherDependencies) {
					DependencyHelper.loadDependency(dependency);
				}
				for (File mod : modGatherer.getMods()) {
					HypertaleAgent.getInstrumentation().appendToSystemClassLoaderSearch(new JarFile(mod));
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
					if (mainClass != null) {
						HypertaleAgent.getInstrumentation().appendToSystemClassLoaderSearch(prePatcher);
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
				HypertaleAgent.getInstrumentation().appendToSystemClassLoaderSearch(new JarFile(input));
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
			HypertaleAgent.getInstrumentation().appendToSystemClassLoaderSearch(new JarFile(input));
			for (DependencyHelper.Dependency dependency : DependencyHelper.patcherDependencies) {
				DependencyHelper.loadDependency(dependency);
			}
			PatcherMain.devMode = dev;
			PatcherMain.patch(input, new File(args[2]), true, true);
			return;
		} else if (args.length == 1 && "--launch-dev".equals(args[0])) {
			launchGame(EmptyArrays.EMPTY_STRING_ARRAY, true);
			return;
		}
		EarlyLogger.start(false);
		EarlyLogger.log("Version " + BuildConfig.HYPERTALE_VERSION);
		File hytaleJar = HypertalePaths.getHytaleJar();
		if (!hytaleJar.exists()) {
			EarlyLogger.log("Cannot find original HytaleServer.jar");
			return;
		}
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
		ModGatherer modGatherer = ModGatherer.gatherMods(args);
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
		ModGatherer modGatherer = ModGatherer.gatherMods(EmptyArrays.EMPTY_STRING_ARRAY);
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
			HypertaleAgent.getInstrumentation().appendToSystemClassLoaderSearch(
					new JarFile(HypertalePaths.hypertaleCacheJar));
		} else if (!dev) {
			throw new FileNotFoundException("Failed to get ./.hypertale/HytaleServer.jar");
		}
		for (DependencyHelper.Dependency dependency : DependencyHelper.patcherDependencies) {
			DependencyHelper.loadDependency(dependency);
		}
		ModLoader.loadHypertaleMods(modGatherer);
		HypertaleAgent.getInstrumentation().addTransformer(Optimizer.classFileTransformer);
		EarlyLogger.log("Launching Hytale...");
		EarlyLogger.stop();
		// We load Hytale is the same classloader! So we can do that!
		LateMain.lateMain(args);
	}

	private static void runPatcher(HypertaleData actualData) throws IOException, InterruptedException {
		EarlyLogger.log("Patching HytaleServer...");
		EarlyLogger.stop();
		ArrayList<String> command = new ArrayList<>();
		if (System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win")) {
			command.add(new File(System.getProperty("java.home") + "\\bin\\javaw.exe").getAbsolutePath());
		} else {
			command.add(new File(System.getProperty("java.home") + "/bin/java").getAbsolutePath());
		}
		command.add("-jar");
		command.add(HypertalePaths.getHypertaleExecJar().getAbsolutePath());
		command.add("--run-patcher");
		int returnCode = new ProcessBuilder(command).inheritIO().start().waitFor();
		if (returnCode != 0 || !HypertalePaths.hypertaleCacheJar.exists()) {
			throw new IOException("Patch failed with return code is " + returnCode);
		}
		EarlyLogger.start(true);
		actualData.modifiedJarSize = HypertalePaths.hypertaleCacheJar.length();
		actualData.writeTo(HypertalePaths.hypertaleCacheData);
	}

	// https://errorprone.info/bugpattern/SystemConsoleNull
	@SuppressWarnings("SystemConsoleNull")
	private static boolean isRunningFromTerminal() {
		Console console = System.console();
		return console != null && console.isTerminal();
	}
}
