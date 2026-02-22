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
package com.build_9.hyxin;

import com.fox2code.hypertale.loader.HypertaleModGatherer;
import com.fox2code.hypertale.loader.HypertaleURLResourceLoader;
import com.google.errorprone.annotations.DoNotCall;
import org.objectweb.asm.ClassReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Stub of Hyxin's {@code LaunchEnvironment} class, used to help improve compatibility with Hyxin mods!
 */
public final class LaunchEnvironment {
	private static final LaunchEnvironment INSTANCE = new LaunchEnvironment();

	private LaunchEnvironment() {}

	public static LaunchEnvironment get() {
		return INSTANCE;
	}

	@DoNotCall
	public static void create(ClassLoader systemLoader, ClassLoader earlyPluginLoader) {
		throw new IllegalStateException("Cannot call create() on Hypertale!");
	}

	@DoNotCall
	public void captureRuntimeLoader(ClassLoader loader) {
		throw new IllegalStateException("Cannot call captureRuntimeLoader() on Hypertale!");
	}

	public ClassLoader getSystemLoader() {
		return HypertaleModGatherer.class.getClassLoader();
	}

	public ClassLoader getEarlyPluginLoader() {
		return HypertaleURLResourceLoader.EARLY_PLUGINS;
	}

	public ClassLoader getRuntimeLoader() {
		return HypertaleModGatherer.class.getClassLoader();
	}

	public ClassLoader findLoaderForClass(String resourceName) throws ClassNotFoundException {
		try {
			return this.findLoaderFor(resourceName.replace(".", "/").concat(".class"));
		} catch (IOException var3) {
			throw new ClassNotFoundException("Could not find class '" + resourceName + "'.", var3);
		}
	}

	public ClassLoader findLoaderFor(String resourceName) throws IOException {
		ClassLoader mainClassLoader = HypertaleModGatherer.class.getClassLoader();
		if (mainClassLoader != null && mainClassLoader.getResource(resourceName) != null) {
			return mainClassLoader;
		} else if (HypertaleURLResourceLoader.EARLY_PLUGINS.getResource(resourceName) != null) {
			return HypertaleURLResourceLoader.EARLY_PLUGINS;
		} else {
			throw new FileNotFoundException("Could not find resource '" + resourceName + "' on any class loader.");
		}
	}

	public InputStream findResourceStream(String resourceName) throws IOException {
		return this.findLoaderFor(resourceName).getResourceAsStream(resourceName);
	}

	public ClassReader getClassReader(String name) throws IOException, ClassNotFoundException {
		String filePath = name.replace('.', '/') + ".class";
		try (InputStream inputStream = this.findResourceStream(filePath)) {
			return new ClassReader(inputStream);
		} catch (RuntimeException var3) {
			throw new ClassNotFoundException("Could not find class '" + filePath + "'.");
		}
	}
}
