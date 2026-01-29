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
package com.fox2code.hypertale.decompiler;

import com.fox2code.hypertale.watchdog.HypertaleWatchdogTimer;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.decompiler.SingleFileSaver;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public final class HypertaleDecompiler extends SingleFileSaver implements IResultSaver {
	private static final HashMap<String, Object> options = new HashMap<>();
	private static final Pattern newLine = Pattern.compile("\\r?\\n");
	private static final HashSet<String> START_CHAR_INT_METHOD_NAMES = new HashSet<>(Arrays.asList(
			"indexOf", "lastIndexOf"
	));

	static {
		options.put("asc", "1");
		options.put("bsm", "1");
		options.put("dec", "0");
		options.put("dee", "1");
		options.put("das", "1");
		options.put("sef", "1");
		options.put("jrt", "current");
		options.put("ega", "1"); // Explicit Generic Arguments
		options.put("dcc", "1"); // Decompile complex constant-dynamic expressions
		options.put("nls", "0"); // New Line Separator
		options.put("vvm", "1"); // Verify Variable Merges
		options.put("pll", "125"); // Preferred line length
		options.put("ind", "\t"); // Indent String
		options.put(HypertaleJavadocProvider.PROPERTY_NAME,
				HypertaleJavadocProvider.INSTANCE);
	}

	private final Fernflower engine;
	private final HypertaleWatchdogTimer watchdogTimer;
	private final DecompilerContext decompilerContext;

	private HypertaleDecompiler(File source, File destination) {
		super(destination);
		this.engine = new Fernflower(this, options, new PrintStreamLogger(System.out) {
			@Override
			public void writeMessage(String message, Severity severity) {
				// Unable to simplify switch on enum: is not an error
				// In fact the method it complains about,
				// the switch-enum is decompiled perfectly!
				if (severity == Severity.ERROR && message.startsWith(
						"Unable to simplify switch on enum:")) {
					severity = Severity.WARN;
				}
				super.writeMessage(message, severity);
			}
		});
		this.engine.addSource(source);
		this.decompilerContext = Objects.requireNonNull(DecompilerContext.getCurrentContext());
		this.decompilerContext.classProcessor.addWhitelist("com/hypixel/");
		this.decompilerContext.classProcessor.addWhitelist("java/");
		this.watchdogTimer = new HypertaleWatchdogTimer(true);
		this.watchdogTimer.setEnabled(false);
	}

	public void decompile() {
		this.watchdogTimer.setEnabled(false);
		try {
			DecompilerContext.setCurrentContext(this.decompilerContext);
			HypertaleVariableNamingFactory.ownClasses =
					this.decompilerContext.structContext.getOwnClasses();
			this.engine.decompileContext();
		} finally {
			HypertaleVariableNamingFactory.ownClasses = Collections.emptyList();
			this.engine.clearContext();
			this.watchdogTimer.setEnabled(false);
		}
	}

	@Override
	public synchronized void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content, int[] mapping) {
		if (entryName.endsWith(".java")) {
			content = fixUpContent(entryName.substring(0, entryName.length() - 5).replace('/', '.'), content);
		}
		super.saveClassEntry(path, archiveName, qualifiedName, entryName, content, mapping);
	}

	@Override
	public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
		super.saveClassFile(path, qualifiedName, entryName,
				fixUpContent(qualifiedName, content), mapping);
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("Expecting 2 arguments: [INPUT] [OUTPUT]");
			System.exit(-1);
			return;
		}
		File input = new File(args[0]);
		File output = new File(args[1]);
		if (!input.exists()) {
			System.out.println("Failed to find " + input.getAbsolutePath());
			System.exit(-1);
			return;
		}
		try (HypertaleDecompiler decompiler = new HypertaleDecompiler(input, output)) {
			System.gc();
			Thread.sleep(10);
			decompiler.decompile();
		}
	}

	// Content fixup helper
	public String fixUpContent(String className, String content) {
		watchdogTimer.heartbeat();
		watchdogTimer.setEnabled(true);
		String[] lines = newLine.split(content);
		StringBuilder stringBuilder = new StringBuilder(content.length() + lines.length);
		for (String line : lines) {
			int startAppendNext = 0;
			while (startAppendNext < line.length()) {
				int prevStartAppendNext = startAppendNext;
				startAppendNext = checkSubstitutions(line, stringBuilder, startAppendNext);
				if (prevStartAppendNext == startAppendNext) {
					throw new IllegalStateException(
							"Preventing looping code at char index " +
									startAppendNext + " for\n    \"" + line + "\"");
				}
			}
			stringBuilder.append("\n");
		}
		watchdogTimer.heartbeat();
		return stringBuilder.toString();
	}

	private static int checkSubstitutions(String line, StringBuilder stringBuilder, int startAppendNext) {
		int methodStart = line.indexOf('(', startAppendNext);
		if (methodStart == -1) {
			stringBuilder.append(line, startAppendNext, line.length());
			return line.length();
		}
		int methodEnd = -1;
		int indent = 0;
		for (int i = methodStart + 1; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '(') indent++;
			if (c == ')') indent--;
			if (indent == -1) {
				methodEnd = i;
				break;
			}
		}
		int numberLen = 0;
		int numberStartIndex = methodStart + 1;
		if (numberStartIndex < line.length() &&
				"-0123456789".indexOf(line.charAt(numberStartIndex + numberLen)) != -1) {
			do {
				numberLen++;
				if ((numberStartIndex + numberLen) == line.length()) {
					// If number is going to the end of the line, ignore it.
					methodEnd = -1;
					numberLen = 0;
					break;
				}
			} while ("0123456789".indexOf(line.charAt(numberStartIndex + numberLen)) != -1);
		}
		if (numberLen == 1 && (line.charAt(numberStartIndex) == '-' ||
				line.charAt(numberStartIndex + numberLen) == 'x')) {
			numberLen = 0; // Skip start number if already hexadecimal
		}
		if (methodEnd != -1) {
			int tmp;
			int endNumberLen = 0;
			while ((tmp = "-0123456789".indexOf(line.charAt((methodEnd - 1) - endNumberLen))) != -1) {
				endNumberLen++;
				if (tmp == 0) {
					break;
				}
			}
		}
		int methodDot = line.lastIndexOf('.', methodStart);
		String methodName = line.substring(methodDot + 1, methodStart).trim();
		if (numberLen != 0 && startAppendNext < numberStartIndex) {
			if (START_CHAR_INT_METHOD_NAMES.contains(methodName)) {
				int number = Integer.parseInt(line.substring(
						numberStartIndex, numberStartIndex + numberLen));
				stringBuilder.append(line, startAppendNext, numberStartIndex)
						.append('\'');
				appendEscapedChar(stringBuilder, (char) number);
				stringBuilder.append('\'');
				startAppendNext = numberStartIndex + numberLen;
			}
		}
		if (startAppendNext <= methodStart) {
			// Avoid infinite looping over every method.
			stringBuilder.append(line, startAppendNext, methodStart + 1);
			startAppendNext = methodStart + 1;
		}
		return startAppendNext;
	}

	private static void appendEscapedChar(StringBuilder stringBuilder, char c) {
		switch (c) {
			case '\0': stringBuilder.append("\\0"); break;
			case '\b': stringBuilder.append("\\b"); break;
			case '\t': stringBuilder.append("\\t"); break;
			case '\n': stringBuilder.append("\\n"); break;
			case '\f': stringBuilder.append("\\f"); break;
			case '\r': stringBuilder.append("\\r"); break;
			case '\"': stringBuilder.append("\\\""); break;
			case '\'': stringBuilder.append("\\'"); break;
			case '\\': stringBuilder.append("\\\\"); break;
			default: stringBuilder.append(c); break;
		}
	}
}
