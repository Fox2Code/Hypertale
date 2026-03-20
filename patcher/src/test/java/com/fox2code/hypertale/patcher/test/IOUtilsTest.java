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
package com.fox2code.hypertale.patcher.test;

import com.fox2code.hypertale.utils.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IOUtilsTest {
	@Test
	public void testUtf8Base64Simple() {
		String testString = "Hello, World!";
		String encoded = IOUtils.toUtf8Base64(testString);
		String decoded = IOUtils.fromUtf8Base64(encoded);
		Assertions.assertEquals(testString, decoded);
	}

	@Test
	public void testUtf8Base64Empty() {
		String testString = "";
		String encoded = IOUtils.toUtf8Base64(testString);
		Assertions.assertEquals(testString, encoded);
		Assertions.assertEquals(testString, IOUtils.fromUtf8Base64(encoded));
	}
}
