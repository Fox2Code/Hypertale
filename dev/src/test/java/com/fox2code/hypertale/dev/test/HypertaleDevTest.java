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
package com.fox2code.hypertale.dev.test;

import com.fox2code.hypertale.dev.HypertaleDevConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class HypertaleDevTest {
	@Test
	public void hypertaleConfigTest() {
		HypertaleDevConfig hypertaleDevConfig = new HypertaleDevConfig();
		String branchType = hypertaleDevConfig.getHytaleBranch();
		Assertions.assertNotNull(branchType, "Initial branchType must not be null");
		boolean failed = false;
		try {
			hypertaleDevConfig.setHytaleBranch("invalid");
		} catch (RuntimeException e) {
			failed = true;
		}
		Assertions.assertTrue(failed, "Setting an invalid Hytale branch should throw an exception.");
		Assertions.assertEquals(branchType, hypertaleDevConfig.getHytaleBranch(),
				"Setting an invalid Hytale branch should not change the actual config branch");
		hypertaleDevConfig.setBuildConfigPackage("com.fox2code.test");
		hypertaleDevConfig.setBuildConfigPackage("com.fox2code.hypertale_example");
		hypertaleDevConfig.setBuildConfigPackage(null);
		hypertaleDevConfig.setHytaleBranch(branchType);
		hypertaleDevConfig.internalMakeImmutableForTesting();
		failed = false;
		try {
			hypertaleDevConfig.setHytaleBranch(branchType);
		} catch (RuntimeException e) {
			failed = true;
		}
		Assertions.assertTrue(failed, "Setting an Hytale branch after resolve throw an exception.");
	}
}
