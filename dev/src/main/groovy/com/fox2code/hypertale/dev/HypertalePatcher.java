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
import org.jetbrains.java.decompiler.main.Fernflower;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.Set;
import org.objectweb.asm.Type;

public final class HypertalePatcher {
	private static File getPatchedHypretaleFile(File hypertaleGradleFolder, String versionBase) {
		File folderBase = HypertaleDepMaker.getHypertaleFolderBase(hypertaleGradleFolder, versionBase);
		return new File(folderBase, "hytale-" + versionBase + HypertaleDepMaker.HYPERTALE_VERSION_SUFFIX + ".jar");
	}

	public static File getPatchedHypertale(File hypertaleGradleFolder, String baseVersion) {
		File hypertaleJar = getPatchedHypretaleFile(hypertaleGradleFolder, baseVersion);
		return hypertaleJar.isFile() ? hypertaleJar : null;
	}

	public static void patch(File javaExec, File hypertaleGradleFolder,
							 HypertaleDevConfig config, Set<File> runtimeClasspath, boolean force)
			throws IOException, InterruptedException {
		File hypertaleJar = getPatchedHypretaleFile(hypertaleGradleFolder, config.getHytaleBranch());
		File hypertaleCache = new File(hypertaleJar.getParentFile(),
				hypertaleJar.getName().replace(".jar", ".dat"));
		File hypertaleSources = new File(hypertaleJar.getParentFile(),
				hypertaleJar.getName().replace(".jar", "-sources.jar"));
		File hytaleJar = HypertaleHytaleDownloader.getVanillaServer(hypertaleGradleFolder, config);
		if (hytaleJar == null) {
			HypertaleHytaleDownloader.downloadHytale(hypertaleGradleFolder, config, false);
			hytaleJar = HypertaleHytaleDownloader.getVanillaServer(hypertaleGradleFolder, config);
			if (hytaleJar == null) throw new IOException("Failed to download HytaleServer.jar");
		}
		if (hypertaleCache.exists() && hypertaleJar.exists() && !force) {
			HypertaleDevData hypertaleDevDataActual = new HypertaleDevData();
			hypertaleDevDataActual.vineflowerVersion = BuildConfig.VINEFLOWER_VERSION.hashCode();
			hypertaleDevDataActual.originalJarSize = hypertaleJar.length();
			hypertaleDevDataActual.modifiedJarSize = hypertaleJar.length();
			try {
				if (hypertaleDevDataActual.equals(new HypertaleDevData(hypertaleCache))) {
					if (config.getDecompileHytale() && !hypertaleSources.exists()) {
						decompile(javaExec, hypertaleJar, hypertaleSources);
					}
					return; // Skip patching if not needed
				}
			} catch (IOException ignored) {}
		}
		if (hypertaleJar.exists() && !hypertaleCache.delete()) {
			throw new IOException("Failed to delete old hypertale jar!");
		}
		if (hypertaleSources.exists() && !hypertaleSources.delete()) {
			throw new IOException("Failed to delete old hypertale sources jar!");
		}
		HypertaleDepMaker.setupHypertale(hypertaleGradleFolder, config.getHytaleBranch());
		HypertaleJavaExec hypertaleJavaExec = new HypertaleJavaExec(javaExec);
		for (File file : runtimeClasspath) {
			hypertaleJavaExec.addFile(file);
		}
		hypertaleJavaExec.setMainClass("com.fox2code.hypertale.launcher.Main");
		hypertaleJavaExec.execRunPatcher(hypertaleGradleFolder, hytaleJar, hypertaleJar);
		if (config.getDecompileHytale()) {
			decompile(javaExec, hypertaleJar, hypertaleSources);
		}
	}

	private static void decompile(File javaExec, File hypertaleJar, File hypertaleSources)
			throws IOException, InterruptedException {
		HypertaleJavaExec hypertaleJavaExec = new HypertaleJavaExec(javaExec);
		hypertaleJavaExec.addFile(getSourceFile(HypertalePatcher.class));
		hypertaleJavaExec.addFile(getSourceFile(Fernflower.class));
		hypertaleJavaExec.addFile(getSourceFile(Type.class));
		hypertaleJavaExec.setMainClass("com.fox2code.hypertale.decompiler.HypertaleDecompiler");
		hypertaleJavaExec.execDecompile(hypertaleJar, hypertaleSources);
	}

	public static File getSourceFile(Class<?> cls) {
		CodeSource codeSource = cls.getProtectionDomain().getCodeSource();
		try {
			return new File(codeSource.getLocation().toURI().getPath()).getAbsoluteFile();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
