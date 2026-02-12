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
package com.fox2code.hypertale.patcher.mixin;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.HashMap;

public final class MixinPropertyService implements IGlobalPropertyService {
	public static final HashMap<String, Object> mixinProperties = new HashMap<>();

	static class Key implements IPropertyKey {

		private final String key;

		Key(String key) {
			this.key = key;
		}

		@Override
		public String toString() {
			return this.key;
		}
	}

	@Override
	public IPropertyKey resolveKey(String name) {
		return new Key(name);
	}

	@Override
	@SuppressWarnings({"TypeParameterUnusedInFormals", "unchecked"})
	public <T> T getProperty(IPropertyKey key) {
		return (T) mixinProperties.get(key.toString());
	}

	@Override
	public void setProperty(IPropertyKey key, Object value) {
		mixinProperties.put(key.toString(), value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getProperty(IPropertyKey key, T defaultValue) {
		return (T) mixinProperties.getOrDefault(key.toString(), defaultValue);
	}

	@Override
	public String getPropertyString(IPropertyKey key, String defaultValue) {
		return mixinProperties.getOrDefault(key.toString(), defaultValue).toString();
	}
}
