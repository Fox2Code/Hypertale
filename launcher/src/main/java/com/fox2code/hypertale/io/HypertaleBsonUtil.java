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

import com.hypixel.hytale.common.util.ArrayUtil;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;
import java.nio.ByteBuffer;

public final class HypertaleBsonUtil {
	public static final BsonDocumentCodec BSON_DOCUMENT_CODEC = new BsonDocumentCodec();
	public static final DecoderContext DECODER_CONTEXT = DecoderContext.builder().build();
	public static final EncoderContext ENCODER_CONTEXT = EncoderContext.builder().build();

	/**
	 * Replace a {@link com.hypixel.hytale.server.core.util.BsonUtil#writeToBytes(BsonDocument)} and
	 * {@link ByteBuffer#wrap(byte[])} chained call with a more memory efficient setup.
	 *
	 * @param document the bson document
	 * @return a ByteBuffer of that document.
	 */
	public static ByteBuffer writeToByteBuffer(BsonDocument document) {
		if (document == null) {
			return ByteBuffer.wrap(ArrayUtil.EMPTY_BYTE_ARRAY);
		} else {
			try (BasicOutputBuffer buffer = new BasicOutputBuffer(8192)) {
				BSON_DOCUMENT_CODEC.encode(new BsonBinaryWriter(buffer), document, ENCODER_CONTEXT);
				return ByteBuffer.wrap(buffer.getInternalBuffer(), 0, buffer.getPosition());
			}
		}
	}
}
