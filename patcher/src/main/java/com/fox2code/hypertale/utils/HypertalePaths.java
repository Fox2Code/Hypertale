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

import com.fox2code.hypertale.loader.HypertaleConfig;

import java.io.File;

public final class HypertalePaths {
	public static final File hypertaleJar = SourceUtil.getSourceFile(HypertalePaths.class);
	public static final File hypertaleCache = new File(".hypertale").getAbsoluteFile();
	public static final File hypertaleCacheLog = new File(hypertaleCache, "hypertale.log").getAbsoluteFile();
	public static final File hypertaleCacheData = new File(hypertaleCache, "cache.dat");
	public static final File hypertaleCacheJar = new File(hypertaleCache, "HytaleServer.jar");
	public static final File hypertalePrePatcher = new File(hypertaleCache, "PrePatcher.jar");
	public static final File hypertalePrePatched = new File(hypertaleCache, "PrePatched.jar");
	public static final File hypertaleConfig = new File(hypertaleCache, "hypertale.ini");
	public static final File hytaleAssets = new File("Assets.zip").getAbsoluteFile();
	public static final File hytaleJar = new File("HytaleServer.jar").getAbsoluteFile();
	public static final File hytaleMods = new File("mods").getAbsoluteFile();
	public static final File hytaleEarlyPlugins = new File("earlyplugins").getAbsoluteFile();

	public static File getHytaleJar() {
		if (!HypertalePaths.hypertaleJar.getName().equals(HypertalePaths.hytaleJar.getName()) &&
				HypertalePaths.hytaleJar.exists()) {
			return HypertalePaths.hytaleJar;
		}
		File secondaryHytaleFileName = new File(
				HypertaleConfig.secondaryJarName).getAbsoluteFile();
		if (!HypertalePaths.hypertaleJar.getName().equals(secondaryHytaleFileName.getName()) &&
				secondaryHytaleFileName.exists()) {
			return secondaryHytaleFileName;
		}
		return new File(
				HypertalePlatform.getPlatform().getLatestFolder(
						HypertaleConfig.hytaleBranch),
				"Server" + File.separator + "HytaleServer.jar");
	}
}
