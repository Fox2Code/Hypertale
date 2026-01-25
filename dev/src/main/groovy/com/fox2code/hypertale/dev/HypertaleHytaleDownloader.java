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

import com.fox2code.hypertale.utils.HypertalePlatform;

import java.io.File;
import java.io.IOException;

public final class HypertaleHytaleDownloader {
	public static File getHytaleHome(HypertaleDevConfig config) {
		String hytaleHomeConfigValue = config.localProperties.getProperty("hytale_home");
		if (hytaleHomeConfigValue != null) {
			File hytaleHomeConfig = new File(hytaleHomeConfigValue);
			if (hytaleHomeConfig.exists()) {
				return hytaleHomeConfig;
			}
		}
		return HypertalePlatform.getPlatform().getHytaleHome();
	}

	public static File getVanillaServer(File hypertaleGradleFolder, HypertaleDevConfig config) {
		File latestFolder = HypertalePlatform.getPlatform().getLatestFolder(
				getHytaleHome(config), config.getHytaleBranch());
		File server = new File(latestFolder, "Server" + File.separator + "HytaleServer.jar");
		File serverBackup = new File(hypertaleGradleFolder, "HytaleServer.jar");
		return server.isFile() ? server : serverBackup.isFile() ? serverBackup : null;
	}

	public static File getVanillaAssets(File hypertaleGradleFolder, HypertaleDevConfig config) {
		File latestFolder = HypertalePlatform.getPlatform().getLatestFolder(
				getHytaleHome(config), config.getHytaleBranch());
		File assets = new File(latestFolder, "Assets.zip");
		File assetsBackup = new File(hypertaleGradleFolder, "Assets.zip");
		return assets.isFile() ? assets : assetsBackup.isFile() ? assetsBackup : null;
	}

	public static void downloadHytale(File hypertaleGradleFolder, HypertaleDevConfig config, boolean force) throws IOException {
		File hytaleJar;
		if (!force && (hytaleJar = getVanillaServer(hypertaleGradleFolder, config)) != null) {
			HypertaleDepMaker.setupHytaleFromJar(hypertaleGradleFolder, hytaleJar, config.getHytaleBranch());
			// Hytale already properly installed
			return;
		}
		// TODO: Add download modal?
		throw new IOException("Hytale was not found on this device!");
	}
}
