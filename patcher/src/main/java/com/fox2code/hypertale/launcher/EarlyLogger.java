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

import com.fox2code.hypertale.utils.HypertalePaths;

import java.io.*;
import java.util.function.Consumer;

public final class EarlyLogger {
	private static final Object logFileLock = new Object();
	private static Consumer<String> loggerFunction;
	private static PrintStream printStream = null;
	private EarlyLogger() {}

	static void start(boolean append) throws IOException {
		synchronized (logFileLock) {
			if (printStream != null) {
				stopLocked();
			}
			if (loggerFunction == null) {
				if (!HypertalePaths.hypertaleCacheLog.exists() && !(
						HypertalePaths.hypertaleCacheLog.getParentFile().mkdirs() &&
								HypertalePaths.hypertaleCacheLog.createNewFile())) {
					throw new IOException("Failed to create Hypertale log file!");
				}
				printStream = new PrintStream(new BufferedOutputStream(
						new FileOutputStream(HypertalePaths.hypertaleCacheLog, append)), true);
			}
		}
	}

	static void stop() {
		if (printStream != null) {
			synchronized (logFileLock) {
				stopLocked();
			}
		}
	}

	private static void stopLocked() {
		PrintStream printStreamTmp = printStream;
		printStream = null;
		if (printStreamTmp != null) {
			printStreamTmp.close();
		}
	}

	public static void log(String message) {
		Consumer<String> loggerFunction = EarlyLogger.loggerFunction;
		if (loggerFunction != null) {
			loggerFunction.accept(message);
			return;
		}
		message = "[Hypertale] " + message;
		System.out.println(message);
		PrintStream printStreamTmp = printStream;
		if (printStreamTmp != null) {
			printStreamTmp.println(message);
		}
	}

	public static void installLoggerFunction(Consumer<String> loggerFunction) {
		synchronized (logFileLock) {
			if (EarlyLogger.loggerFunction == null) {
				log("Switching to Hytale logging!");
				stopLocked();
				EarlyLogger.loggerFunction = loggerFunction;
			}
		}
	}
}
