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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.WrappedByteBuf;
import io.netty.util.ReferenceCounted;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Hyper-specialized ByteBuf made to reduce memory pressure caused by Hytale networking.
 */
public final class HypertaleRecyclableByteBuf extends WrappedByteBuf implements ReferenceCounted {
	private static final Comparator<HypertaleRecyclableByteBuf> SIZE_COMPARATOR =
			Comparator.comparingInt(HypertaleRecyclableByteBuf::capacity);
	private static final ArrayList<HypertaleRecyclableByteBuf> recyclableByteBufCache = new ArrayList<>(8);
	private static final int MAX_CONCURRENT_BUFFER_COUNT = 8;
	private static final int SIZE_ALIGN = 4096;
	private static final int INITIAL_BUFFER_SIZE = 65536;
	private int refCount;
	private boolean released;

	private HypertaleRecyclableByteBuf() {
		super(Unpooled.directBuffer(INITIAL_BUFFER_SIZE));
		this.refCount = 1;
		this.released = false;
	}

	@Override
	public ByteBuffer nioBuffer() {
		// By default nioBuffer() create a clone of the memory, using internalNioBuffer() doesn't.
		return this.buf.internalNioBuffer(this.buf.readerIndex(), this.buf.readableBytes());
	}

	@Override
	public ByteBuf capacity(int newCapacity) {
		final int oldCapacity = this.buf.capacity();
		newCapacity = Math.max(newCapacity, oldCapacity);
		newCapacity = ((newCapacity + SIZE_ALIGN - 1) / SIZE_ALIGN) * SIZE_ALIGN;
		if (newCapacity > oldCapacity) {
			super.capacity(newCapacity);
		}
		return this;
	}

	@Override
	public ByteBuf retain() {
		this.guardReleased();
		this.refCount++;
		return this;
	}

	@Override
	public ByteBuf retain(int i) {
		this.guardReleased();
		this.refCount += i;
		return this;
	}

	@Override
	public ByteBuf touch() {
		this.guardReleased();
		return this;
	}

	@Override
	public ByteBuf touch(Object hint) {
		this.guardReleased();
		return this;
	}

	@Override
	public boolean release() {
		this.guardReleased();
		this.refCount--;
		return this.checkRelease();
	}

	@Override
	public boolean release(int i) {
		this.guardReleased();
		this.refCount -= i;
		return this.checkRelease();
	}

	private boolean checkRelease() {
		if (this.refCount <= 0) {
			this.released = true;
			addRecyclableToList(this);
			return true;
		}
		return false;
	}

	private void guardReleased() {
		if (this.released) {
			throw new IllegalStateException("Buffer has been released!");
		}
	}

	public static ByteBuf recyclableNetworkBuffer(int length) {
		if (length == 0) return Unpooled.EMPTY_BUFFER;
		return recyclableBuffer(length == INITIAL_BUFFER_SIZE);
	}

	public static HypertaleRecyclableByteBuf recyclableBuffer(boolean preferBig) {
		HypertaleRecyclableByteBuf recyclableByteBuf = null;
		synchronized (recyclableByteBufCache) {
			if (!recyclableByteBufCache.isEmpty()) {
				if (preferBig) {
					recyclableByteBuf = recyclableByteBufCache.removeLast();
				} else {
					recyclableByteBuf = recyclableByteBufCache.removeFirst();
				}
			}
		}
		if (recyclableByteBuf != null) {
			recyclableByteBuf.setIndex(0, 0);
			recyclableByteBuf.markReaderIndex();
			recyclableByteBuf.markWriterIndex();
			recyclableByteBuf.released = false;
			recyclableByteBuf.refCount = 1;
		} else {
			recyclableByteBuf = new HypertaleRecyclableByteBuf();
		}
		return recyclableByteBuf;
	}

	private static void addRecyclableToList(HypertaleRecyclableByteBuf hypertaleRecyclableByteBuf) {
		synchronized (recyclableByteBufCache) {
			int index = Collections.binarySearch(recyclableByteBufCache, hypertaleRecyclableByteBuf, SIZE_COMPARATOR);
			if (index < 0)
				index = -1 - index;
			else // Increase the set index if found to make the latest used buffer rest a bit!
				index++;
			recyclableByteBufCache.add(index, hypertaleRecyclableByteBuf);
			// Release smaller buffers if bigger buffers are needed!
			if (recyclableByteBufCache.size() > MAX_CONCURRENT_BUFFER_COUNT &&
					recyclableByteBufCache.getFirst().capacity() == INITIAL_BUFFER_SIZE) {
				hypertaleRecyclableByteBuf = recyclableByteBufCache.removeFirst();
				hypertaleRecyclableByteBuf.buf.release();
			}
		}
	}

	static {
		// Preallocate buffers we know we will need for our first player
		addRecyclableToList(new HypertaleRecyclableByteBuf());
		addRecyclableToList(new HypertaleRecyclableByteBuf());
	}
}
