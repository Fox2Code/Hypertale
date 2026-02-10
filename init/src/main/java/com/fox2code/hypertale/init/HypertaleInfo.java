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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public record HypertaleInfo(File hypertale, int hypertaleInitVer) {
	private static final File mods = new File("mods").getAbsoluteFile();
	private static final File hytaleServerJar = new File("HytaleServer.jar").getAbsoluteFile();
	private static final File hypertaleConfig = new File(".hypertale/hypertale.ini").getAbsoluteFile();
	private static final File hypertaleInit = getSourceFile(HypertaleInfo.class);
	private static final Attributes.Name hypertaleInitAttribute = new Attributes.Name("Hypertale-Init");

	public static HypertaleInfo findHypertale() {
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
					} catch (Exception _) {
					}
				}
			}
		}
		return launchJar == null ? null : new HypertaleInfo(launchJar, hypertaleInitVer);
	}


	public static File getHytaleLaunchServerJar() {
		File hytaleServer = null;
		if (hytaleServerJar.exists() && !hypertaleInit.getName().equals(hytaleServerJar.getName())) {
			hytaleServer = hytaleServerJar;
		} else {
			// Try the secondary server jar if needed
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

	public static File getSourceFile(Class<?> cls) {
		CodeSource codeSource = cls.getProtectionDomain().getCodeSource();
		try {
			return new File(codeSource.getLocation().toURI().getPath()).getAbsoluteFile();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public static void runHypertale(
			ClassLoader classLoader, String mainClassName, int hypertaleInitVer, String[] args)
			throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		// hypertaleInitV1Main
		Class<?> mainClass = Class.forName(mainClassName, true, classLoader);
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
	}
}
