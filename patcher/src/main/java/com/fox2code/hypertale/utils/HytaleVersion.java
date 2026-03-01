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
package com.fox2code.hypertale.utils;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public final class HytaleVersion {
	public static final String HYTALE_VERSION = fetchHytaleVersion();

	private HytaleVersion() {}

	private static String fetchHytaleVersion() {
		// Prefer checking the pom.properties first!
		Properties properties = new Properties();
		try (InputStream inputStream = new BufferedInputStream(Objects.requireNonNull(
				JavaPlugin.class.getClassLoader().getResourceAsStream(
						"META-INF/maven/com.hypixel.hytale/Common/pom.properties")))) {
			properties.load(inputStream);
		} catch (Exception _) {}
		String hytaleVersion = properties.getProperty("version", "").trim();
		if (!hytaleVersion.isEmpty()) {
			return hytaleVersion;
		}
		// Check the manifest next!
		Package pkg = JavaPlugin.class.getPackage();
		hytaleVersion = pkg == null ? null : pkg.getImplementationVersion();
		if (hytaleVersion != null && !hytaleVersion.isEmpty()) {
			return hytaleVersion;
		}
		// Just say unknown!
		return "unknown";
	}
}
