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

import com.sun.management.OperatingSystemMXBean;

final class HypertaleJVMHelperOpenJDK extends HypertaleJVMHelper {
	private final OperatingSystemMXBean sunOperatingSystemMXBean;

	private HypertaleJVMHelperOpenJDK(OperatingSystemMXBean sunOperatingSystemMXBean) {
		this.sunOperatingSystemMXBean = sunOperatingSystemMXBean;
	}

	static HypertaleJVMHelper tryInitialize(
			java.lang.management.OperatingSystemMXBean operatingSystemMXBean) {
		if (operatingSystemMXBean instanceof OperatingSystemMXBean sunOperatingSystemMXBean) {
			return new HypertaleJVMHelperOpenJDK(sunOperatingSystemMXBean);
		}
		return null;
	}

	@Override
	public double getCpuLoad() {
		return this.sunOperatingSystemMXBean.getCpuLoad();
	}

	@Override
	public double getProcessCpuLoad() {
		return this.sunOperatingSystemMXBean.getProcessCpuLoad();
	}

	@Override
	public long getMaxMemory() {
		return Math.min(Runtime.getRuntime().maxMemory(),
				this.sunOperatingSystemMXBean.getTotalMemorySize() -
						MEMORY_NEGATIVE_OFFSET);
	}

	@Override
	public long getFreeMemory() {
		Runtime runtime = Runtime.getRuntime();
		long jvmFreeMemory = runtime.maxMemory() - runtime.totalMemory();
		long sunFreeMemory = this.sunOperatingSystemMXBean.getFreeMemorySize() - MEMORY_NEGATIVE_OFFSET;
		return Math.min(jvmFreeMemory, sunFreeMemory) + runtime.freeMemory();
	}
}
