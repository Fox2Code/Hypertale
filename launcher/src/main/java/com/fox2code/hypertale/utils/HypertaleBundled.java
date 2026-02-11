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

/**
 * Automatically gets bundled into your mod unless disabled in the Hypertale Gradle plugin!
 *
 * <p>This interface provides fields for detecting Hypertale's presence and runtime state in your mod.</p>
 */
public interface HypertaleBundled {
	/**
	 * Indicates whether Hypertale is loaded in the current runtime.
	 *
	 * <p>This field can be safely accessed even when Hypertale is absent, as the Hypertale Gradle
	 * plugin injects this class into your mod at compile time. Use this field to conditionally
	 * execute Hypertale-specific code in your mod.</p>
	 */
	boolean hasHypertale = HypertaleBundled.class.getClassLoader() == JavaPlugin.class.getClassLoader() &&
			JavaPlugin.class.getClassLoader().getResource( // Check plugin existence!
					"com/fox2code/hypertale/utils/HypertaleBundled.class") != null;
	/**
	 * Indicates whether the code is running in a separate Hypertale patcher process
	 * rather than the main application process.
	 *
	 * <p>Use this field to handle differences between the patcher process and the main
	 * runtime environment in your mod.</p>
	 */
	boolean inHypertalePatcherProcess = hasHypertale && Boolean.getBoolean("hypertale.patcherProcess");
}
