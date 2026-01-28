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

import com.fox2code.hypertale.launcher.DependencyHelper;

import com.fox2code.hypertale.utils.IOUtils;
import com.fox2code.hypertale.utils.SourceUtil;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Objects;

public class DependencyHelperTest {
	private static final String INITIAL_STRING = "Library sha256 mismatch detected:";

	@Test
	public void testDependencies() throws Exception {
		StringBuilder errorMessage = new StringBuilder().append(INITIAL_STRING);
		HashSet<String> classChecks = new HashSet<>();
		for (DependencyHelper.Dependency dependency : DependencyHelper.patcherDependencies) {
			if (!classChecks.add(dependency.classCheck())) {
				throw new Exception("Duplicate class check for " + dependency.classCheck());
			}
			String sha256 = IOUtils.toHex(IOUtils.sha256Of(Objects.requireNonNull(
					SourceUtil.getSourceFile(Class.forName(dependency.classCheck())))));
			if (!sha256.equals(dependency.sha256Sum())) {
				errorMessage.append("\n- ").append(dependency.name())
						.append(" (Got ").append(sha256).append(", expected ")
						.append(dependency.sha256Sum()).append(")");
			}
		}
		if (errorMessage.length() != INITIAL_STRING.length()) {
			throw new Exception(errorMessage.toString());
		}
	}
}
