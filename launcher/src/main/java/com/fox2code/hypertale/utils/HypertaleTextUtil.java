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

import org.jspecify.annotations.NonNull;

public final class HypertaleTextUtil {
	private static final long MEM_1KB = 1024;
	private static final long MEM_1MB = MEM_1KB * MEM_1KB;
	private static final long MEM_1GB = MEM_1MB * MEM_1KB;

	private HypertaleTextUtil() {}

	public static @NonNull String makeUptimeString(long seconds) {
		long days = seconds / 86400;
		seconds %= 86400;
		long hours = seconds / 3600;
		seconds %= 3600;
		long minutes = seconds / 60;
		seconds %= 60;
		StringBuilder sb = new StringBuilder();
		if (days > 0) sb.append(days).append("d ");
		if (hours > 0) sb.append(hours).append("h ");
		if (minutes > 0) sb.append(minutes).append("m ");
		sb.append(seconds).append("s");
		return sb.toString();
	}

	public static @NonNull String makeMemoryString(long bytes) {
		if (bytes > MEM_1GB) {
			return String.format("%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0);
		}
		if (bytes > MEM_1MB) {
			return String.format("%.2f MB", bytes / 1024.0 / 1024.0);
		}
		return String.format("%d KB", bytes / 1024);
	}

	public static @NonNull String makeCpuString(double cpu) {
		return String.format("%.2f%%", cpu * 100);
	}
}
