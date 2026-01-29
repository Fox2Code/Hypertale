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

import com.fox2code.hypertale.utils.IOUtils;
import com.fox2code.hypertale.utils.NetUtils;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.jar.JarFile;

public final class DependencyHelper {
	private static final boolean DEBUG = false;
	private static final File librariesDir = new File(
			System.getProperty("hypertale.librariesDir", "libraries")).getAbsoluteFile();
	public static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";

	public static final Dependency[] patcherDependencies = new Dependency[]{
			new Dependency("org.ow2.asm:asm:" + BuildConfig.ASM_VERSION, MAVEN_CENTRAL,
					"org.objectweb.asm.ClassVisitor", null,
					"6f3828a215c920059a5efa2fb55c233d6c54ec5cadca99ce1b1bdd10077c7ddd"),
			new Dependency("org.ow2.asm:asm-tree:" + BuildConfig.ASM_VERSION, MAVEN_CENTRAL,
					"org.objectweb.asm.tree.ClassNode", null,
					"0f3555096b720b820bbacab0b515589bee0200bee099bda14c561738ae837ba1"),
			new Dependency("org.ow2.asm:asm-analysis:" + BuildConfig.ASM_VERSION,
					MAVEN_CENTRAL, "org.objectweb.asm.tree.analysis.Analyzer", null,
					"6260bffc8ec008dd1b713702c7994e2c94d188a3da5bef9e87278a16df6a7522"),
			new Dependency("org.ow2.asm:asm-commons:" + BuildConfig.ASM_VERSION,
					MAVEN_CENTRAL, "org.objectweb.asm.commons.InstructionAdapter", null,
					"c2319e014ce7199f2b7f7d56d6bb991863168c3f4b6cd6c9f542a4937ef7ef88"),
			new Dependency("org.ow2.asm:asm-util:" + BuildConfig.ASM_VERSION,
					MAVEN_CENTRAL, "org.objectweb.asm.util.CheckClassAdapter", null,
					"c5ebbbeaf68126af094b42fa4800f59bc4413abd02d95b9aefad722cd257e207"),
			new Dependency("it.unimi.dsi:fastutil:" + BuildConfig.FASTUTIL_VERSION,
					MAVEN_CENTRAL, "it.unimi.dsi.fastutil.Arrays", null,
					"9094ae67d01d0ad246f886f11ad557fc2e79c72cbf3feed83e1512a8ae90a74a"),
			new Dependency("com.google.code.gson:gson:" + BuildConfig.GUAVA_VERSION,
					MAVEN_CENTRAL, "com.google.gson.Gson", null,
					"dd0ce1b55a3ed2080cb70f9c655850cda86c206862310009dcb5e5c95265a5e0"),
			new Dependency("com.google.guava:failureaccess:" + BuildConfig.GUAVA_FAILURE_ACCESS_VERSION,
					MAVEN_CENTRAL, "com.google.common.util.concurrent.internal.InternalFutureFailureAccess", null,
					"cbfc3906b19b8f55dd7cfd6dfe0aa4532e834250d7f080bd8d211a3e246b59cb"),
			new Dependency("com.google.guava:guava:" + BuildConfig.GUAVA_VERSION,
					MAVEN_CENTRAL, "com.google.common.io.Files", null,
					"1e301f0c52ac248b0b14fdc3d12283c77252d4d6f48521d572e7d8c4c2cc4ac7")
	};

	public static void loadDependency(Dependency dependency) {
		if (hasClass(dependency.classCheck)) return;
		if (!librariesDir.isDirectory() && !librariesDir.mkdirs())
			throw new IOError(new IOException("Failed to create libraries directory"));
		String postURL = resolvePostURL(dependency.name);
		File file = new File(librariesDir, fixUpPath(postURL));
		if (!file.isAbsolute()) file = file.getAbsoluteFile();
		boolean justDownloaded = false;
		checkHashOrDelete(file, dependency, false);
		if (!file.exists()) {
			File parentFile = file.getParentFile();
			if (!parentFile.isDirectory() && !parentFile.mkdirs()) {
				throw new RuntimeException("Cannot create dependency directory for " + dependency.name);
			}
			IOException fallBackIoe = null;
			try (OutputStream os = Files.newOutputStream(file.toPath())) {
				justDownloaded = true;
				NetUtils.downloadTo(URI.create(dependency.repository.endsWith(".jar") ?
						dependency.repository : dependency.repository + "/" + postURL).toURL(), os);
			} catch (IOException ioe) {
				if (dependency.fallbackUrl != null) {
					fallBackIoe = ioe;
				} else {
					if (file.exists() && !file.delete()) file.deleteOnExit();
					throw new RuntimeException("Cannot download " + dependency.name, ioe);
				}
			}
			if (fallBackIoe != null) {
				try (OutputStream os = Files.newOutputStream(file.toPath())) {
					justDownloaded = true;
					NetUtils.downloadTo(URI.create(dependency.fallbackUrl).toURL(), os);
				} catch (IOException ioe) {
					if (file.exists() && !file.delete()) file.deleteOnExit();
					throw new RuntimeException("Cannot download " + dependency.name, fallBackIoe);
				}
			}
		}
		checkHashOrDelete(file, dependency, true);
		try {
			HypertaleAgent.getInstrumentation().appendToSystemClassLoaderSearch(new JarFile(file));
			if (hasClass(dependency.classCheck)) {
				if (DEBUG) {
					EarlyLogger.log("Loaded " +
							dependency.name + " -> " + file.getPath());
				}
			} else {
				if (!justDownloaded) {
					// Assume file is corrupted if load failed.
					if (file.exists() && !file.delete()) file.deleteOnExit();
					loadDependency(dependency);
					return;
				}
				throw new RuntimeException("Failed to load " +
						dependency.name + " -> " + file.getPath());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String fixUpPath(String path) {
		return File.separatorChar == '\\' ?
				path.replace('/', '\\') : path;
	}

	public static boolean hasClass(String cls) {
		try {
			Class.forName(cls, false, DependencyHelper.class.getClassLoader());
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	private static String resolvePostURL(String string) {
		String[] depKeys = string.split(":");
		// "org.joml:rrrr:${jomlVersion}"      => ${repo}/org/joml/rrrr/1.9.12/rrrr-1.9.12.jar
		// "org.joml:rrrr:${jomlVersion}:rrrr" => ${repo}/org/joml/rrrr/1.9.12/rrrr-1.9.12-rrrr.jar
		if (depKeys.length == 3) {
			return depKeys[0].replace('.','/')+"/"+depKeys[1]+"/"+depKeys[2]+"/"+depKeys[1]+"-"+depKeys[2]+".jar";
		}
		if (depKeys.length == 4) {
			return depKeys[0].replace('.','/')+"/"+depKeys[1]+"/"+depKeys[2]+"/"+depKeys[1]+"-"+depKeys[2]+"-"+depKeys[3]+".jar";
		}
		throw new RuntimeException("Invalid Dep");
	}

	private static void checkHashOrDelete(File file, Dependency dependency, boolean errorOut) {
		if (dependency.sha256Sum == null || !file.exists()) return;
		String hashString;
		try {
			hashString = IOUtils.toHex(IOUtils.sha256Of(file));
		} catch (IOException e) {
			hashString = "";
		}
		if (!dependency.sha256Sum.equals(hashString)) {
			boolean deleteSuccessful = file.delete();
			if (errorOut) {
				throw new RuntimeException("Remote dependency " + dependency.name + " checksum mismatch " +
						"(got: " + hashString + ", expected: " + dependency.sha256Sum + ")");
			}
			if (!deleteSuccessful) {
				throw new RuntimeException("Can't delete dependency with checksum mismatch " + dependency.name);
			}
		}
	}

	public record Dependency(String name, String repository, String classCheck, String fallbackUrl, String sha256Sum) {
		public Dependency(String name, String repository, String classCheck) {
			this(name, repository, classCheck, null, null);
		}

		public Dependency(String name, String repository, String classCheck, String fallbackUrl) {
			this(name, repository, classCheck, fallbackUrl, null);
		}
	}
}
