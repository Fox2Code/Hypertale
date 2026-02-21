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
package com.fox2code.hypertale.test;

import com.fox2code.hypertale.patcher.HypertaleASMConstants;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;

public class ASMConstantsTest {
	private static final HashSet<String> IGNORED_CONSTANTS = new HashSet<>(Arrays.asList(
			"LAMBDA_ARGS"
	));

	@Test
	public void checkASMConstants() throws IllegalAccessException {
		final ClassLoader classLoader = ASMConstantsTest.class.getClassLoader();
		for (Field field : HypertaleASMConstants.class.getFields()) {
			if (field.getType() != String.class || IGNORED_CONSTANTS.contains(field.getName())) {
				continue;
			}
			String asmDescription = field.get(null).toString();
			if (classLoader.getResource(asmDescription + ".class") == null) {
				throw new RuntimeException("ASM constant for " + field.getName() + " not found");
			}
		}
	}
}
