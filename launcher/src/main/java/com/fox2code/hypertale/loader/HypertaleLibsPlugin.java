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

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import org.jspecify.annotations.NonNull;

public final class HypertaleLibsPlugin extends JavaPlugin {
	public HypertaleLibsPlugin(@NonNull JavaPluginInit init) {
		super(init);
	}

	@Override
	protected void start() {
		// Do not crash if loaded later
		if (PluginManager.get().hasPlugin(new PluginIdentifier("Hypertale", "Hypertale"), SemverRange.WILDCARD)) {
			this.hypertale().setLoggerName(HypertaleCompatibility.ANSI_MAGENTA + "HypertaleLibraries");
			this.getLogger().atInfo().log("Offline Hypertale: Libraries extracted successfully by Hypertale!");
			this.getLogger().atInfo().log("You can now safely delete it as its content has been extracted! :3");
			this.getLogger().atInfo().log("To check the libraries out, you can check the \"libraries\" folder!");
		} else {
			this.getLogger().atWarning().log("Offline Hypertale: Libraries in use, please keep it in the mod folder!");
		}
	}
}
