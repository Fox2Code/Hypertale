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

import java.io.File;
import java.util.Locale;

public enum HypertalePlatform {
	WINDOWS("start") {
		@Override
		public File getHytaleHome() {
			return new File(System.getenv("APPDATA") + "\\Hytale");
		}

		@Override
		public File getLatestFolder(File hytaleHome, String branch) {
			return new File(hytaleHome, "install\\" + branch + "\\package\\game\\latest");
		}
	},
	MACOS("open") {
		@Override
		public File getHytaleHome() {
			return new File(System.getProperty("user.home") + "/Application Support/Hytale");
		}

		@Override
		public File getLatestFolder(File hytaleHome, String branch) {
			return new File(hytaleHome, "install/" + branch + "/package/game/latest");
		}
	},
	LINUX("xdg-open") {
		@Override
		public File getHytaleHome() {
			// Hytale official documentation isn't accurate, but let use it as a fallback!
			File flatpakPath = new File(System.getProperty("user.home") +
					"/.var/app/com.hypixel.HytaleLauncher/data/Hytale");
			File fallbackPath = new File(System.getProperty("user.home") + "/Hytale");
			return flatpakPath.exists() || !fallbackPath.exists() ? flatpakPath : fallbackPath;
		}

		@Override
		public File getLatestFolder(File hytaleHome, String branch) {
			return new File(hytaleHome, "install/" + branch + "/package/game/latest");
		}
	};

	private static final HypertalePlatform platform;

	static {
		String name = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		if (name.startsWith("win")) {
			platform = WINDOWS;
		} else if (name.startsWith("mac") ||
				name.startsWith("darwin")) {
			platform = MACOS;
		} else if (name.contains("nix") ||
				name.contains("nux") ||
				name.contains("aix")) {
			platform = LINUX;
		} else {
			throw new Error("Unsupported system");
		}
	}

	public final String open;

	HypertalePlatform(String open) {
		this.open = open;
	}

	public abstract File getHytaleHome();

	public abstract File getLatestFolder(File hytaleHome, String branch);

	public File getLatestFolder(String branch) {
		return this.getLatestFolder(this.getHytaleHome(), branch);
	}

	public static HypertalePlatform getPlatform() {
		return platform;
	}
}
