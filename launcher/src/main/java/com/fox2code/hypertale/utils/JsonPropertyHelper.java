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
package com.fox2code.hypertale.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public final class JsonPropertyHelper {
	private JsonPropertyHelper() {}

	public static boolean getBoolean(JsonObject jsonObject, String key) {
		return getBoolean(jsonObject, key, false);
	}

	public static boolean getBoolean(JsonObject jsonObject, String key, boolean def) {
		JsonElement jsonElement = jsonObject.get(key);
		if (jsonElement != null && jsonElement.isJsonPrimitive() &&
				jsonElement instanceof JsonPrimitive jsonPrimitive &&
				(jsonPrimitive.isBoolean() || jsonPrimitive.isString())) {
			return jsonPrimitive.getAsBoolean();
		}
		return def;
	}

	public static String getString(JsonObject jsonObject, String key) {
		return getString(jsonObject, key, null);
	}

	public static String getString(JsonObject jsonObject, String key, String def) {
		JsonElement jsonElement = jsonObject.get(key);
		if (jsonElement != null && jsonElement.isJsonPrimitive() &&
				jsonElement instanceof JsonPrimitive jsonPrimitive &&
				jsonPrimitive.isString()) {
			return jsonPrimitive.getAsString();
		}
		return def;
	}
}
