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

import com.fox2code.hypertale.launcher.BuildConfig;
import com.fox2code.hypertale.utils.HypertalePaths;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class HypertaleConfig {
	private static final Properties defaultProperties;
	private static final String HYPERTALE_CONFIG_COMMENT =
			"Configuration for Hypertale " + BuildConfig.HYPERTALE_VERSION + "\n" +
			"Do not modify unless you know what you are doing!";
	public static String hytaleBranch = "release";
	public static String secondaryJarName = "Server.jar";
	public static boolean optimizePluginOnlyAPIs = true;
	public static boolean aggressivelyOptimizePluginOnlyAPIs = false;
	public static int watchdogWarnLagAfter = 10;
	public static int watchdogExitAfter = 30;

	private HypertaleConfig() {}

	static {
		defaultProperties = new Properties();
		for (Field field : HypertaleConfig.class.getFields()) {
			if (field.getModifiers() == (Modifier.PUBLIC | Modifier.STATIC)) {
				try {
					defaultProperties.put(field.getName(), String.valueOf(field.get(null)));
					// Get system properties for initial configuration.
					String val = System.getProperty("hypertale." + field.getName());
					if (val != null && !val.isEmpty()) {
						if (field.getType() == String.class) {
							field.set(null, val);
						} else if (field.getType() == boolean.class) {
							field.setBoolean(null, Boolean.parseBoolean(val));
						} else if (field.getType() == int.class) {
							try {
								field.setInt(null, Integer.parseInt(val));
							} catch (NumberFormatException ignored) {}
						}
					}
				} catch (IllegalAccessException ignored) {}
			}
		}
		fixupConfiguration();
	}

	public static void load() throws IOException {
		Properties properties;
		if (!HypertalePaths.hypertaleConfig.exists()) {
			if (!HypertalePaths.hypertaleConfig.createNewFile()) {
				throw new IOException("Failed to create config file!");
			}
			properties = new Properties(defaultProperties);
			properties.load(new InputStreamReader(new BufferedInputStream(
					new FileInputStream(HypertalePaths.hypertaleConfig)),
					StandardCharsets.UTF_8));
		} else {
			properties = defaultProperties;
			properties.store(new OutputStreamWriter(new BufferedOutputStream(
					new FileOutputStream(HypertalePaths.hypertaleConfig)),
							StandardCharsets.UTF_8), HYPERTALE_CONFIG_COMMENT);
		}

		for (Field field : HypertaleConfig.class.getFields()) {
			try {
				if (field.getModifiers() == (Modifier.PUBLIC | Modifier.STATIC)) {
					String val = properties.getProperty(field.getName());
					if (field.getType() == String.class) {
						field.set(null, val);
					} else if (field.getType() == boolean.class) {
						field.setBoolean(null, Boolean.parseBoolean(val));
					} else if (field.getType() == int.class) {
						try {
							field.setInt(null, Integer.parseInt(val));
						} catch (NumberFormatException ignored) {}
					}
				}
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Failed to set field " + field.getName(), e);
			}
		}
		fixupConfiguration();
	}

	public static void save() throws IOException {
		fixupConfiguration();
		if (!HypertalePaths.hypertaleConfig.exists() &&
				!HypertalePaths.hypertaleConfig.createNewFile()) {
			throw new IOException("Failed to create config file!");
		}
		Properties properties = new Properties();
		for (Field field : HypertaleConfig.class.getFields()) {
			if (field.getModifiers() == (Modifier.PUBLIC | Modifier.STATIC)) {
				try {
					properties.put(field.getName(), String.valueOf(field.get(null)));
				} catch (IllegalAccessException ignored) {}
			}
		}
		properties.store(new OutputStreamWriter(new BufferedOutputStream(
						new FileOutputStream(HypertalePaths.hypertaleConfig)),
						StandardCharsets.UTF_8), HYPERTALE_CONFIG_COMMENT);
	}

	public static int patchConfigFlags() {
		int configFlags = 0;
		if (optimizePluginOnlyAPIs) {
			configFlags |= 0x01;
			if (aggressivelyOptimizePluginOnlyAPIs) {
				configFlags |= 0x02;
			}
		}
		return configFlags;
	}

	private static void fixupConfiguration() {
		if (aggressivelyOptimizePluginOnlyAPIs && !optimizePluginOnlyAPIs) {
			aggressivelyOptimizePluginOnlyAPIs = false;
		}
		if ("HytaleServer.jar".equals(secondaryJarName) || secondaryJarName.isEmpty()) {
			secondaryJarName = "Server.jar";
		}
	}
}
