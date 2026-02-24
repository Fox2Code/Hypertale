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

import com.fox2code.hypertale.loader.HypertaleConfig;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;

/**
 * This class is overwritten by Hypertale Offline and Hypertale Plus
 * <p>
 * This class is loaded before any library is loaded
 */
final class MainPlus {
	static void setEditionProperties() {
		System.setProperty("hypertale.edition", "OSS");
		System.setProperty("hypertale.premium", "false");
	}

	static Instrumentation tryGetInstrumentationFallback() {
		return null;
	}

	@SuppressWarnings("DoNotCallSuggester")
	static void patchAsClassPath() throws IOException {
		throw new IOException("Only premium versions of Hypertale can use this feature!");
	}

	@SuppressWarnings("DoNotCallSuggester")
	static void launchPatchedAsClassPath(String[] args) throws IOException {
		throw new IOException("Only premium versions of Hypertale can use this feature!");
	}

	static boolean checkHytaleJarFile(File file) {
		if (HypertaleConfig.checkJarValidity()) {
			throw new RuntimeException("Only premium versions of Hypertale can use this feature!");
		}
		return file.isFile();
	}

	static boolean checkHaltLaunchGame(String[] args) {
		return false;
	}

	static boolean forceOffline() {
		// Hypertale Offline Edition overwrite this method
		return false;
	}
}