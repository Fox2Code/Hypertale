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

import com.fox2code.hypertale.utils.HytaleVersion;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import io.netty.handler.codec.CodecException;
import org.bson.BsonDocument;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Codec that will prefer picking "Hypertale" prefixed variant of itself if it exists!
 *
 * @param <T> the type of the value
 */
public final class HypertaleKeyedCodec<T> extends KeyedCodec<T> {
	private final KeyedCodec<T> hypertaleCodec;
	private final String hypertaleKey;
	private final T validResult;


	public HypertaleKeyedCodec(@NonNull String key, Codec<T> codec, boolean required) {
		super(key, codec, required);
		this.hypertaleKey = "Hypertale" + key;
		this.hypertaleCodec = new KeyedCodec<>(this.hypertaleKey, codec, required);
		this.validResult = getValidResult(key, codec);
	}

	public HypertaleKeyedCodec(@NonNull String key, Codec<T> codec) {
		super(key, codec);
		this.hypertaleKey = key;
		this.hypertaleCodec = new KeyedCodec<>(this.hypertaleKey, codec);
		this.validResult = getValidResult(key, codec);
	}

	@Override
	public @NonNull Optional<T> get(@Nullable BsonDocument document, @NonNull ExtraInfo extraInfo) {
		Optional<T> result = Optional.empty();
		try {
			result = this.hypertaleCodec.get(document, extraInfo);
		} catch (CodecException _) {}
		if (result.isPresent()) {
			if (this.validResult != null && "*".equals(result.get())) {
				// if the result is a string, T being a string is a safe assumption!
				return Optional.of(this.validResult);
			}
			return result;
		}
		return super.get(document, extraInfo);
	}

	@SuppressWarnings("unchecked")
	private static <T> T getValidResult(String key, Codec<T> codec) {
		if (Codec.STRING != codec) {
			return null;
		}
		if ("ServerVersion".equals(key)) {
			return (T) HytaleVersion.HYTALE_VERSION;
		}
		return null;
	}
}
