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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

public final class Main {
	private static final String entryModSyncBootstrap = "de/onyxmoon/modsync/bootstrap/ModSyncBootstrap.class";
	private static final String classModSyncBootstrap = "de.onyxmoon.modsync.bootstrap.ModSyncBootstrap";
	private static final File hytaleServerJar;
	private static final File hypertaleConfig;
	private static final File hypertaleInit;
	private static final File mods;
	private static final File earlyplugins;
	private static final File assets;
	private static final Attributes.Name hypertaleInitAttribute;

	static {
		Locale.setDefault(Locale.ENGLISH);
		System.setProperty("java.awt.headless", "true");
		System.setProperty("file.encoding", "UTF-8");
		System.setProperty("hypertale.useInitWrapper", "true");
		hytaleServerJar = new File("HytaleServer.jar").getAbsoluteFile();
		hypertaleConfig = new File(".hypertale/hypertale.ini").getAbsoluteFile();
		CodeSource codeSource = Main.class.getProtectionDomain().getCodeSource();
		try {
			hypertaleInit = new File(codeSource.getLocation().toURI().getPath()).getAbsoluteFile();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		mods = new File("mods").getAbsoluteFile();
		earlyplugins = new File("earlyplugins").getAbsoluteFile();
		assets = new File("Assets.zip").getAbsoluteFile();
		hypertaleInitAttribute = new Attributes.Name("Hypertale-Init");
	}

	static void main(String[] args) {
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
						File hytaleJar = getHytaleLaunchServerJar();
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
		File launchJar = null;
		int hypertaleInitVer = 0;
		if (mods.isDirectory()) {
			for (File file : Objects.requireNonNull(mods.listFiles())) {
				if (file.getName().endsWith(".jar")) {
					try (JarFile jarFile = new JarFile(file)) {
						int fileHypertaleInitVer = Integer.parseInt(jarFile.getManifest()
								.getMainAttributes().getValue(hypertaleInitAttribute));
						if (fileHypertaleInitVer > 0) {
							hypertaleInitVer = fileHypertaleInitVer;
							launchJar = file;
						}
					} catch (Exception _) {}
				}
			}
		}
		String mainClassName = "com.fox2code.hypertale.launcher.Main";
		if (launchJar == null) {
			launchJar = getHytaleLaunchServerJar();
			mainClassName = "com.hypixel.hytale.Main";
			if (launchJar == null) {
				System.out.println("[HypertaleInit] Failed to find any valid launch point");
				return;
			}
			if (args.length == 0 && assets.isFile()) {
				args = new String[]{"--assets", assets.getName()};
			}
		}
		try {
			Agent.instrumentation.appendToSystemClassLoaderSearch(new JarFile(launchJar));
			// hypertaleInitV1Main
			Class<?> mainClass = Class.forName(mainClassName);
			Method method;
			try {
				method = mainClass.getDeclaredMethod("main", String[].class);
				if (!Modifier.isPublic(method.getModifiers())) {
					method.setAccessible(true);
				}
			} catch (Exception e) {
				if (hypertaleInitVer == 0) {
					throw e;
				}
				try {
					method = mainClass.getDeclaredMethod(
							"hypertaleInitV1Main", String[].class);
					if (!Modifier.isPublic(method.getModifiers())) {
						method.setAccessible(true);
					}
				} catch (Exception _) {
					throw e;
				}
			}
			method.invoke(null, (Object) args);
		} catch (InvocationTargetException e) {
			sneakyThrow(e.getTargetException());
			throw new RuntimeException("Server failed to start-up!", e);
		} catch (Exception e) {
			throw new RuntimeException("Invalid Hytale server jar!", e);
		} finally {
			Agent.instrumentation = null;
		}
	}

	private static File getHytaleLaunchServerJar() {
		File hytaleServer = null;
		if (hytaleServerJar.exists() && !hypertaleInit.getName().equals(hytaleServerJar.getName())) {
			hytaleServer = hytaleServerJar;
		} else {
			// Try secondary server jar if needed
			Properties properties = new Properties();
			if (hypertaleConfig.isFile()) {
				try (InputStreamReader inputStreamReader = new InputStreamReader(
						new BufferedInputStream(new FileInputStream(
								hypertaleConfig)), StandardCharsets.UTF_8)) {
					properties.load(inputStreamReader);
				} catch (IOException _) {}
			}
			File secondaryServerJar = new File(properties
					.getProperty("secondaryJarName", "Server.jar")).getAbsoluteFile();
			if (secondaryServerJar.exists() && !hypertaleInit.getName().equals(secondaryServerJar.getName())) {
				hytaleServer = secondaryServerJar;
			}
		}
		return hytaleServer;
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

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
		throw (E) e;
	}
}
