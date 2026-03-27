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
package com.fox2code.hypertale.init.test;

import com.fox2code.hypertale.init.HypertaleInfo;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Locale;

public class InitFindClientTest {
	@Test
	public void testFindClient() {
		// First check if Hytale client is installed, skip test if not
		File check = null;
		if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux")) {
			check = new File(System.getProperty("user.home"),
					".var/app/com.hypixel.HytaleLauncher/data/Hytale/UserData/Mods");
		}
		if (check == null || !check.exists() ||
				HypertaleInfo.findHypertale(check) == null) {
			//noinspection DataFlowIssue
			Assumptions.assumeTrue(false, "Unsupported setup, skipping test");
			return;
		}
		// Test Hypertale info
		HypertaleInfo hypertaleInfo = HypertaleInfo.findHypertaleSingleplayer();
		if (hypertaleInfo == null) {
			throw new IllegalStateException("Failed to find Hypertale client");
		}
	}
}
