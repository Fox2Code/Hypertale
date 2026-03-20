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
package com.fox2code.hypertale.utils.adapter;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.sneakythrow.SneakyThrow;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Used by Hypertale to simultaneously support release and pre-release versions of HytaleServer.
 */
public final class HypertaleMSAdapter<T> {
	private final Class<T> clazz;
	private final String methodName;
	private final Method messageMethodCall;
	private final Method stringMethodCall;

	public HypertaleMSAdapter(Class<T> clazz, String methodName) {
		this.clazz = clazz;
		this.methodName = methodName;
		Method messageMethodCall = null;
		Method stringMethodCall = null;
		try {
			messageMethodCall = clazz.getMethod(methodName, Message.class);
		} catch (NoSuchMethodException _) {}
		try {
			stringMethodCall = clazz.getMethod(methodName, String.class);
		} catch (NoSuchMethodException _) {}
		this.messageMethodCall = messageMethodCall;
		this.stringMethodCall = stringMethodCall;
	}

	public Class<?> getDeclaringClass() {
		return this.clazz;
	}

	public String getMethodName() {
		return this.methodName;
	}

	public void call(T object, String message) {
		if (this.stringMethodCall != null) {
			try {
				this.stringMethodCall.invoke(this.clazz.cast(object), message);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw SneakyThrow.sneakyThrow(e.getTargetException());
			}
		} else if (this.messageMethodCall != null) {
			try {
				this.messageMethodCall.invoke(this.clazz.cast(object), Message.raw(message));
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw SneakyThrow.sneakyThrow(e.getTargetException());
			}
		}
	}

	public void call(T object, Message message) {
		if (this.messageMethodCall != null) {
			try {
				this.messageMethodCall.invoke(this.clazz.cast(object), message);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw SneakyThrow.sneakyThrow(e.getTargetException());
			}
		} else if (this.stringMethodCall != null) {
			try {
				this.stringMethodCall.invoke(this.clazz.cast(object), message.getRawText());
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw SneakyThrow.sneakyThrow(e.getTargetException());
			}
		}
	}
}
