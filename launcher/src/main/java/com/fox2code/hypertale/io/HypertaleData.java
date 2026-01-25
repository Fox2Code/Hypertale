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
package com.fox2code.hypertale.io;

import java.io.*;
import java.util.Objects;

public final class HypertaleData {
	private static final int HYPERTALE_CACHE_FORMAT_VERSION = 0;
	private final int hypertaleCacheFormatVersion;
	public long hypertaleJarSize;
	public long originalJarSize;
	public long modifiedJarSize;
	public int patchConfigFlags;
	public int modHash;

	public HypertaleData(File file) throws IOException {
		try (DataInputStream dataInputStream = new DataInputStream(
				new BufferedInputStream(new FileInputStream(file)))) {
			int hypertaleCacheFormatVersion = -1;
			try {
				hypertaleCacheFormatVersion = dataInputStream.readInt();
			} catch (Exception ignored) {}
			this.hypertaleCacheFormatVersion = hypertaleCacheFormatVersion;
			if (hypertaleCacheFormatVersion == HYPERTALE_CACHE_FORMAT_VERSION) {
				this.hypertaleJarSize = dataInputStream.readLong();
				this.originalJarSize = dataInputStream.readLong();
				this.modifiedJarSize = dataInputStream.readLong();
				this.patchConfigFlags = dataInputStream.readInt();
				this.modHash = dataInputStream.readInt();
			}
		}
	}

	public HypertaleData() {
		this.hypertaleCacheFormatVersion = HYPERTALE_CACHE_FORMAT_VERSION;
		this.hypertaleJarSize = 0;
		this.originalJarSize = 0;
		this.modifiedJarSize = 0;
		this.patchConfigFlags = 0;
		this.modHash = 0;
	}

	public void writeTo(File file) throws IOException {
		if (!file.exists() && !file.createNewFile()) {
			throw new IOException("Failed to create new file!");
		}
		try (DataOutputStream dataOutputStream = new DataOutputStream(
				new BufferedOutputStream(new FileOutputStream(file)))) {
			dataOutputStream.writeInt(HYPERTALE_CACHE_FORMAT_VERSION);
			dataOutputStream.writeLong(this.hypertaleJarSize);
			dataOutputStream.writeLong(this.originalJarSize);
			dataOutputStream.writeLong(this.modifiedJarSize);
			dataOutputStream.writeInt(this.patchConfigFlags);
			dataOutputStream.writeInt(this.modHash);
		}
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof HypertaleData hypertaleData &&
				this.hypertaleCacheFormatVersion == hypertaleData.hypertaleCacheFormatVersion &&
				this.hypertaleJarSize == hypertaleData.hypertaleJarSize &&
				this.originalJarSize == hypertaleData.originalJarSize &&
				this.modifiedJarSize == hypertaleData.modifiedJarSize &&
				this.patchConfigFlags == hypertaleData.patchConfigFlags &&
				this.modHash == hypertaleData.modHash;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.hypertaleCacheFormatVersion,
				this.hypertaleJarSize, this.originalJarSize,
				this.modifiedJarSize, this.patchConfigFlags, this.modHash);
	}
}
