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
import com.fox2code.hypertale.launcher.DependencyHelper;
import com.fox2code.hypertale.unpick.HypertaleUnpicker;
import com.fox2code.hypertale.utils.HypertalePlatform;
import com.fox2code.hypertale.utils.SourceUtil;
import org.jetbrains.java.decompiler.main.Fernflower;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import org.objectweb.asm.Type;

public final class HypertalePatcher {
	private static final String hypertalePatchLockProperty =
			"com.fox2code.hypertale.dev.HypertalePatcher.hypertalePatchLock";
	private static final ReentrantLock hypertalePatchLock;

	static {
		// Ensure hypertale has a JVM wide lock even if loaded multiple times in different class loaders
		final ReentrantLock hypertalePatchLockTmp;
		synchronized (System.getProperties()) {
			Object propertyLock = System.getProperties().get(hypertalePatchLockProperty);
			if (propertyLock instanceof ReentrantLock reentrantLock) {
				hypertalePatchLockTmp = reentrantLock;
			} else {
				hypertalePatchLockTmp = new ReentrantLock();
				System.getProperties().put(hypertalePatchLockProperty, hypertalePatchLockTmp);
			}
		}
		hypertalePatchLock = hypertalePatchLockTmp;
	}

	private static File getPatchedHypretaleFile(File hypertaleGradleFolder, String versionBase) {
		File folderBase = HypertaleDepMaker.getHypertaleFolderBase(hypertaleGradleFolder, versionBase);
		return new File(folderBase, "hytale-" + versionBase + HypertaleDepMaker.HYPERTALE_VERSION_SUFFIX + ".jar");
	}

	public static File getPatchedHypertale(File hypertaleGradleFolder, String baseVersion) {
		File hypertaleJar = getPatchedHypretaleFile(hypertaleGradleFolder, baseVersion);
		return hypertaleJar.isFile() ? hypertaleJar : null;
	}

	public static void patch(File javaExec, File hypertaleGradleFolder,
							 HypertaleDevConfig config, boolean force)
			throws IOException, InterruptedException {
		File hypertaleJar = getPatchedHypretaleFile(hypertaleGradleFolder, config.getHytaleBranch());
		File hypertaleCache = new File(hypertaleJar.getParentFile(),
				hypertaleJar.getName().replace(".jar", ".dat"));
		File hypertalePom = new File(hypertaleJar.getParentFile(),
				hypertaleJar.getName().replace(".jar", ".pom"));
		File hypertaleUnpicked = new File(hypertaleJar.getParentFile(),
				hypertaleJar.getName().replace(".jar", "-unpick.jar"));
		File hypertaleSources = new File(hypertaleJar.getParentFile(),
				hypertaleJar.getName().replace(".jar", "-sources.jar"));
		File hytaleJar = HypertaleHytaleDownloader.getVanillaServer(hypertaleGradleFolder, config);
		if (hytaleJar == null) {
			HypertaleHytaleDownloader.downloadHytale(hypertaleGradleFolder, config, false);
			hytaleJar = HypertaleHytaleDownloader.getVanillaServer(hypertaleGradleFolder, config);
			if (hytaleJar == null) throw new IOException("Failed to download HytaleServer.jar");
		}
		HypertaleDevData hypertaleDevDataActual = new HypertaleDevData();
		hypertaleDevDataActual.vineflowerVersion = BuildConfig.VINEFLOWER_VERSION.hashCode();
		hypertaleDevDataActual.originalJarSize = hytaleJar.length();
		hypertaleDevDataActual.modifiedJarSize = hypertaleJar.length();
		if (hypertaleCache.exists() && hypertaleJar.exists() && hypertalePom.exists() && !force) {
			try {
				if (hypertaleDevDataActual.equals(new HypertaleDevData(hypertaleCache))) {
					if (config.getDecompileHytale() && !hypertaleSources.exists()) {
						decompile(hypertaleGradleFolder, javaExec,
								hypertaleJar, hypertaleUnpicked, hypertaleSources);
					}
					return; // Skip patching if not needed
				}
			} catch (IOException ignored) {}
		}
		if (hypertaleJar.exists() && !hypertaleJar.delete()) {
			throw new IOException("Failed to delete old hypertale jar!");
		}
		if (hypertaleUnpicked.exists() && !hypertaleUnpicked.delete()) {
			throw new IOException("Failed to delete old hypertale unpick jar!");
		}
		if (hypertaleSources.exists() && !hypertaleSources.delete()) {
			throw new IOException("Failed to delete old hypertale sources jar!");
		}
		HypertaleDepMaker.setupHypertale(hypertaleGradleFolder, config.getHytaleBranch());
		HypertaleJavaExec hypertaleJavaExec = new HypertaleJavaExec(javaExec);
		for (DependencyHelper.Dependency dependency : DependencyHelper.patcherDependencies) {
			try {
				hypertaleJavaExec.addFile(SourceUtil.getSourceFile(Class.forName(
						dependency.classCheck(), false, HypertalePatcher.class.getClassLoader())));
			} catch (ClassNotFoundException _) {}
		}
		hypertaleJavaExec.addFile(SourceUtil.getSourceFile(HypertalePlatform.class));
		hypertaleJavaExec.setMainClass("com.fox2code.hypertale.launcher.Main");
		hypertalePatchLock.lock();
		try {
			hypertaleJavaExec.execRunPatcher(hypertaleGradleFolder, hytaleJar, hypertaleJar);
		} finally {
			hypertalePatchLock.unlock();
		}
		hypertaleDevDataActual.modifiedJarSize = hypertaleJar.length();
		hypertaleDevDataActual.writeTo(hypertaleCache);
		if (config.getDecompileHytale()) {
			decompile(hypertaleGradleFolder, javaExec, hypertaleJar, hypertaleUnpicked, hypertaleSources);
		}
	}

	private static void decompile(File hypertaleGradleFolder, File javaExec,
								  File hypertaleJar, File hypertaleUnpicked, File hypertaleSources)
			throws IOException, InterruptedException {
		if (!hypertaleUnpicked.exists()) {
			hypertalePatchLock.lock();
			try {
				HypertaleUnpicker.patch(hypertaleJar, hypertaleUnpicked);
			} finally {
				hypertalePatchLock.unlock();
			}
		}
		HypertaleJavaExec hypertaleJavaExec = new HypertaleJavaExec(javaExec);
		hypertaleJavaExec.addFile(SourceUtil.getSourceFile(HypertalePlatform.class));
		hypertaleJavaExec.addFile(SourceUtil.getSourceFile(HypertalePatcher.class));
		hypertaleJavaExec.addFile(SourceUtil.getSourceFile(Fernflower.class));
		hypertaleJavaExec.addFile(SourceUtil.getSourceFile(Type.class));
		hypertaleJavaExec.setMainClass("com.fox2code.hypertale.decompiler.HypertaleDecompiler");
		hypertalePatchLock.lock();
		try {
			if (!hypertaleSources.exists()) {
				hypertaleJavaExec.execDecompile(hypertaleGradleFolder,
						hypertaleUnpicked, hypertaleSources);
			}
		} finally {
			hypertalePatchLock.unlock();
		}
	}
}
