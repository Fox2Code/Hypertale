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
package com.fox2code.hypertale.utils.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public abstract class HypertaleJVMHelper {
	static final int MEMORY_NEGATIVE_OFFSET = 16384; // 64kb
	private static final HypertaleJVMHelper HYPERTALE_JVM_HELPER = initHelper();

	HypertaleJVMHelper() {}

	private static HypertaleJVMHelper initHelper() {
		OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
		HypertaleJVMHelper jvmHelper = null;
		try {
			jvmHelper = HypertaleJVMHelperOpenJDK.tryInitialize(operatingSystemMXBean);
		} catch (Throwable ignored) {}
		if (jvmHelper != null) return jvmHelper;
		return new HypertaleJVMHelperFallback(operatingSystemMXBean);
	}

	public static HypertaleJVMHelper get() {
		return HYPERTALE_JVM_HELPER;
	}

	public abstract double getCpuLoad();

	public abstract double getProcessCpuLoad();

	/**
	 * Query maximum memory the JVM can use.
	 *
	 * @return maximum memory the JVM can use
	 */
	public long getMaxMemory() {
		return Runtime.getRuntime().maxMemory();
	}

	/**
	 * Query remaining memory the server can use.
	 *
	 * @return remaining memory the server can use
	 */
	public long getFreeMemory() {
		Runtime runtime = Runtime.getRuntime();
		return runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory());
	}

	/**
	 * Query total memory the jvm has allocated.
	 *
	 * @return total memory the jvm has allocated
	 */
	public long getAllocatedMemory() {
		return Runtime.getRuntime().totalMemory();
	}

	/**
	 * Query memory actively used by the JVM.
	 *
	 * @return memory actively used by the JVM
	 */
	public long getUsedMemory() {
		Runtime runtime = Runtime.getRuntime();
		return runtime.maxMemory() - runtime.freeMemory();
	}
}
