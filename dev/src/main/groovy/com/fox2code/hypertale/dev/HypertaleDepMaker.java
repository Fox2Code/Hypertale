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
package com.fox2code.hypertale.dev;

import com.fox2code.hypertale.launcher.BuildConfig;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class HypertaleDepMaker {
	public static final String HYTALE_CACHE_PATH_ROOT = "com/hypixel/hytale/";
	public static final String HYPERTALE_VERSION_SUFFIX = "+hypertale-" + BuildConfig.HYPERTALE_VERSION;
	public static final String HYPERTALE_CACHE_PATH_ROOT =
			"com/hypixel/hytale/hypertale-" + BuildConfig.HYPERTALE_VERSION + "/";
	public static final String HYPERTALE_DEPENDENCY =
			"com.hypixel:hytale:hypertale-" + BuildConfig.HYPERTALE_VERSION;

	public static void setupHytaleFromJar(File hypertaleGradleCache, File file, String version) throws IOException {
		File folderBase = new File(hypertaleGradleCache, HYTALE_CACHE_PATH_ROOT + version);
		injectPom(new File(folderBase, "hytale-" + version + ".pom"), version);
		createLinkReplace(new File(folderBase, "hytale-" + version + ".jar"), file);
	}

	public static void setupHypertale(File hypertaleGradleCache, String baseVersion) throws IOException {
		final String version = baseVersion + HYPERTALE_VERSION_SUFFIX;
		File folderBase = new File(hypertaleGradleCache, HYTALE_CACHE_PATH_ROOT + version);
		injectPom(new File(folderBase, "hytale-" + version + ".pom"), version);
	}

	public static File getHypertaleFolderBase(File hypertaleGradleCache, String baseVersion) {
		final String version = baseVersion + HYPERTALE_VERSION_SUFFIX;
		return new File(hypertaleGradleCache, HYTALE_CACHE_PATH_ROOT + version);
	}

	static void injectPom(File file, String ver) throws IOException {
		File parent = file.getParentFile();
		if (!parent.isDirectory() && !parent.mkdirs())
			throw new IOException("Failed to create cache dir, is file-system read-only?");
		Files.writeString(file.toPath(), "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<project xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\"\n" +
				"         xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
				"    <modelVersion>4.0.0</modelVersion>\n" +
				"    <groupId>com.hypixel</groupId>\n" +
				"    <artifactId>hytale</artifactId>\n" +
				"    <version>"+ver+"</version>\n" +
				"    <packaging>jar</packaging>\n" +
				"    <dependencies>\n" +
				"    </dependencies>\n" +
				"    <properties>\n" +
				"        <maven.compiler.source>25</maven.compiler.source>\n" +
				"        <maven.compiler.target>25</maven.compiler.target>\n" +
				"    </properties>\n" +
				"</project>", StandardCharsets.UTF_8);
	}

	static void createLinkReplace(File link, File target) throws IOException {
		if (!target.exists()) {
			throw new IOException("Target file does not exists!");
		}
		if (link.exists() && !link.delete()) {
			throw new IOException("Failed to delete original link!");
		}
		Files.createLink(link.toPath(), target.toPath());
	}
}
