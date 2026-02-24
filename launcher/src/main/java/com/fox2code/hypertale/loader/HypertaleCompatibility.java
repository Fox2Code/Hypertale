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

public final class HypertaleCompatibility {
	public static final String entryModSyncBootstrap = "de/onyxmoon/modsync/bootstrap/ModSyncBootstrap.class";
	public static final String classModSyncBootstrap = "de.onyxmoon.modsync.bootstrap.ModSyncBootstrap";
	public static final String entryHyxinMixinService = "com/build_9/hyxin/mixin/MixinService.class";
	public static final String entryHyxinTransformer = "com/build_9/hyxin/mixin/HyxinTransformer.class";
	// Use 16, 256, and true color ANSI codes for color, if it is not supported, it is usually ignored!
	public static final String ANSI_MAGENTA = "\u001B[35;95m\u001B[38;5;201m\u001B[38;2;255;0;255m";
}
