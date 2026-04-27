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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Used to check if hypertale should be enabled or disabled on start
 * as we should not load the GSON library on start if Hypertale is disabled.
 */
public final class InternalMicroJsonScanner {
	private InternalMicroJsonScanner() {}

	public static String readValue(File file, String keyPath) {
		if (!file.isFile()) return null;

		try {
			return readValue(Files.readAllBytes(file.toPath()), keyPath);
		} catch (IOException e) {
			return null;
		}
	}


	public static String readValue(byte[] buf, String keyPath) {
		int n = buf.length;

		String[] keys = keyPath.split("\\.");
		int targetDepth = 0;   // which depth we expect the next key at
		int currentDepth = 0;  // brace depth as we scan
		boolean inString = false;

		// Build KMP table for first key
		byte[] needle = makeNeedle(keys[targetDepth]);
		int[] fail = buildFail(needle);
		int m = 0; // KMP match state

		for (int i = 0; i < n; i++) {
			byte b = buf[i];

			// Track string boundaries so braces inside strings are ignored
			if (inString) {
				if (b == '\\') { i++; continue; } // skip escaped char
				if (b == '"') inString = false;
				continue;
			}
			if (b == '"') { inString = true; }

			// Track brace depth (only outside strings)
			if (b == '{') { currentDepth++; continue; }
			if (b == '}') { currentDepth--; m = 0; continue; } // leaving scope, reset KMP

			// Only run KMP when we're at the depth we care about
			if (currentDepth != targetDepth + 1) continue;

			// KMP step
			while (m > 0 && b != needle[m]) m = fail[m - 1];
			if (b == needle[m]) m++;

			if (m == needle.length) {
				m = 0;

				// Verify it's followed by optional whitespace then ':'
				int j = i + 1;
				while (j < n && isWhitespace(buf[j])) j++;
				if (j >= n || buf[j] != ':') continue; // was a value not a key
				j++;

				targetDepth++;

				if (targetDepth == keys.length) {
					// Final key — extract value
					return extractValue(buf, j, n);
				} else {
					// Intermediate key — skip into its object, update needle
					needle = makeNeedle(keys[targetDepth]);
					fail = buildFail(needle);
					// currentDepth will increment when we hit the '{' naturally
				}
			}
		}

		return null;
	}

	private static String extractValue(byte[] buf, int from, int n) {
		int i = from;
		while (i < n && isWhitespace(buf[i])) i++;
		if (i >= n) return null;

		if (buf[i] == 't') return "true";
		if (buf[i] == 'f') return "false";
		if (buf[i] == 'n') return "null";

		if (buf[i] == '-' || (buf[i] >= '0' && buf[i] <= '9')) {
			int start = i;
			while (i < n && buf[i] != ',' && buf[i] != '}' && !isWhitespace(buf[i])) i++;
			return new String(buf, start, i - start, StandardCharsets.UTF_8);
		}

		if (buf[i] == '"') {
			int start = ++i;
			while (i < n) {
				if (buf[i] == '\\') { i += 2; continue; }
				if (buf[i] == '"') return new String(buf, start, i - start, StandardCharsets.UTF_8);
				i++;
			}
		}

		return null;
	}

	private static byte[] makeNeedle(String key) {
		return ("\"" + key + "\"").getBytes(StandardCharsets.UTF_8);
	}

	private static int[] buildFail(byte[] p) {
		int[] f = new int[p.length];
		for (int i = 1, k = 0; i < p.length; i++) {
			while (k > 0 && p[i] != p[k]) k = f[k - 1];
			if (p[i] == p[k]) k++;
			f[i] = k;
		}
		return f;
	}

	private static boolean isWhitespace(byte b) {
		return b == ' ' || b == '\t' || b == '\n' || b == '\r';
	}
}