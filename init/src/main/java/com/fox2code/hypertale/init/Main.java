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
package com.fox2code.hypertale.init;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

public final class Main {
	private static final String entryModSyncBootstrap = "de/onyxmoon/modsync/bootstrap/ModSyncBootstrap.class";
	private static final String classModSyncBootstrap = "de.onyxmoon.modsync.bootstrap.ModSyncBootstrap";
	private static final File hypertaleInit;
	private static final File earlyplugins;
	private static final File assets;

	static {
		if (Boolean.getBoolean("hypertale.useInitWrapper")) {
			throw new IllegalStateException("HypertaleInit already loaded with loaded with: " +
					System.getProperty("hypertale.initMethod", "unknown"));
		}
		Locale.setDefault(Locale.ENGLISH);
		System.setProperty("java.awt.headless", "true");
		System.setProperty("file.encoding", "UTF-8");
		System.setProperty("hypertale.useInitWrapper", "true");
		System.setProperty("hypertale.initMethod", "bootstrap");
		hypertaleInit = HypertaleInfo.getSourceFile(Main.class);
		earlyplugins = new File("earlyplugins").getAbsoluteFile();
		assets = new File("Assets.zip").getAbsoluteFile();
	}

	static void main(String[] args) {
		if (args.length == 0 && !isRunningFromTerminal()) {
			// Avoid running a server from a double click.
			System.out.println("[Hypertale] File double click detected! Skipping running!");
			System.out.println("[Hypertale] Append \"--\" as an argument to skip this check!");
			return;
		}
		if (Agent.instrumentation == null) {
			if (Boolean.getBoolean("hypertale.unsupportedHost")) {
				System.out.println("[HypertaleInit] Failed to start up Hypertale!");
				return;
			}
			System.out.println("[HypertaleInit] Unsupported host!");
			System.gc();
			runSelfSameJVMArgs(args);
			return;
		}
		// ModSync: Boostrap support
		if (earlyplugins.isDirectory()) {
			for (File file : Objects.requireNonNull(earlyplugins.listFiles())) {
				if (file.getName().endsWith(".jar")) {
					boolean valid = false;
					try (ZipFile zipFile = new ZipFile(file)) {
						valid = zipFile.getEntry(entryModSyncBootstrap) != null;
					} catch (Exception _) {}
					if (valid) {
						System.setProperty("hypertale.modSyncBootstrapInit", "true");
						File hytaleJar = HypertaleInfo.getHytaleLaunchServerJar();
						if (hytaleJar != null) {
							try (URLClassLoader urlClassLoader = new URLClassLoader(
									new URL[]{hytaleJar.toURI().toURL(), file.toURI().toURL()},
									Main.class.getClassLoader())) {
								// true means the static constructor is called!
								Class.forName(classModSyncBootstrap, true, urlClassLoader);
							} catch (Throwable _) {}
						}
						break;
					}
				}
			}
		}
		HypertaleInfo hypertaleInfo = HypertaleInfo.findHypertale();
		String mainClassName;
		int hypertaleInitVer = 0;
		File launchJar;
		if (hypertaleInfo == null) {
			launchJar = HypertaleInfo.getHytaleLaunchServerJar();
			mainClassName = "com.hypixel.hytale.Main";
			if (launchJar == null) {
				System.out.println("[HypertaleInit] Failed to find any valid launch point");
				return;
			}
			if (args.length == 0 && assets.isFile()) {
				args = new String[]{"--assets", assets.getName()};
			}
		} else {
			hypertaleInitVer = hypertaleInfo.hypertaleInitVer();
			launchJar = hypertaleInfo.hypertale();
			mainClassName = hypertaleInfo.mainClassName();
		}
		try {
			Agent.instrumentation.appendToSystemClassLoaderSearch(new JarFile(launchJar));

			HypertaleInfo.runHypertale(Main.class.getClassLoader(), mainClassName, hypertaleInitVer, args);
		} catch (InvocationTargetException e) {
			sneakyThrow(e.getTargetException());
			throw new RuntimeException("Server failed to start-up!", e);
		} catch (Exception e) {
			throw new RuntimeException("Invalid Hytale server jar!", e);
		} finally {
			Agent.instrumentation = null;
		}
	}

	private static void runSelfSameJVMArgs(String[] args) {
		ArrayList<String> command = new ArrayList<>();
		if (System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win")) {
			command.add(new File(System.getProperty("java.home") + "\\bin\\javaw.exe").getAbsolutePath());
		} else {
			command.add(new File(System.getProperty("java.home") + "/bin/java").getAbsolutePath());
		}
		command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
		command.add("-Dhypertale.unsupportedHost=true");
		command.add("-jar");
		command.add(hypertaleInit.getAbsolutePath());
		command.addAll(Arrays.asList(args));
		try {
			System.exit(new ProcessBuilder(command).inheritIO().start().waitFor());
		} catch (InterruptedException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	// https://errorprone.info/bugpattern/SystemConsoleNull
	@SuppressWarnings("SystemConsoleNull")
	private static boolean isRunningFromTerminal() {
		Console console = System.console();
		return console != null && console.isTerminal();
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
		throw (E) e;
	}
}
