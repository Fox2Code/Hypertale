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
package com.fox2code.hypertale.dev;

import java.io.*;

public final class HypertaleDevData {
	private static final int HYPERTALE_DEV_CACHE_FORMAT_VERSION = 0;
	private final int hypertaleCacheFormatVersion;
	public long vineflowerVersion;
	public long originalJarSize;
	public long modifiedJarSize;

	public HypertaleDevData(File file) throws IOException {
		try (DataInputStream dataInputStream = new DataInputStream(
				new BufferedInputStream(new FileInputStream(file)))) {
			int hypertaleCacheFormatVersion = -1;
			try {
				hypertaleCacheFormatVersion = dataInputStream.readInt();
			} catch (Exception ignored) {}
			this.hypertaleCacheFormatVersion = hypertaleCacheFormatVersion;
			if (hypertaleCacheFormatVersion == HYPERTALE_DEV_CACHE_FORMAT_VERSION) {
				this.vineflowerVersion = dataInputStream.readLong();
				this.originalJarSize = dataInputStream.readLong();
				this.modifiedJarSize = dataInputStream.readLong();
			}
		}
	}

	public HypertaleDevData() {
		this.hypertaleCacheFormatVersion = HYPERTALE_DEV_CACHE_FORMAT_VERSION;
		this.vineflowerVersion = 0;
		this.originalJarSize = 0;
		this.modifiedJarSize = 0;
	}

	public void writeTo(File file) throws IOException {
		if (!file.exists() && !file.createNewFile()) {
			throw new IOException("Failed to create new file!");
		}
		try (DataOutputStream dataOutputStream = new DataOutputStream(
				new BufferedOutputStream(new FileOutputStream(file)))) {
			dataOutputStream.writeInt(HYPERTALE_DEV_CACHE_FORMAT_VERSION);
			dataOutputStream.writeLong(this.vineflowerVersion);
			dataOutputStream.writeLong(this.originalJarSize);
			dataOutputStream.writeLong(this.modifiedJarSize);
		}
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof HypertaleDevData hypertaleData &&
				this.hypertaleCacheFormatVersion == hypertaleData.hypertaleCacheFormatVersion &&
				this.vineflowerVersion == hypertaleData.vineflowerVersion &&
				this.originalJarSize == hypertaleData.originalJarSize &&
				this.modifiedJarSize == hypertaleData.modifiedJarSize;
	}
}
