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
package com.fox2code.hypertale.init.late;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;
import java.util.function.BiFunction;

final class HookClassLoader extends URLClassLoader {
	private final ClassLoader systemClassLoader;
	private final Package hytaleExamplePackage;
	private final Object packageMakeLock = new Object();
	private BiFunction<String, byte[], byte[]> classTransformer;
	private Class<?> firstHytaleClass = null;

	public HookClassLoader(URL[] urls, Package hytaleExamplePackage) {
		super(urls, ClassLoader.getSystemClassLoader().getParent());
		this.hytaleExamplePackage = hytaleExamplePackage;
		this.systemClassLoader = ClassLoader.getSystemClassLoader();
	}

	public void addURLHelper(URL url) {
		Objects.requireNonNull(url, "url must not be null");
		super.addURL(url);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if (name.startsWith("java.") || name.startsWith("javax.") ||
				name.startsWith("sun.") || name.startsWith("com.sun.")) {
			return this.systemClassLoader.loadClass(name);
		}
		if (name.startsWith("com.fox2code.hypertale.init.")) {
			return HookClassLoader.class.getClassLoader().loadClass(name);
		}
		return super.loadClass(name);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		URL resource;
		if (!name.startsWith("com.hypixel.hytale.") ||
				(resource = getResource(name.replace('.', '/') + ".class")) == null) {
			return super.findClass(name);
		}
		synchronized (this.packageMakeLock) {
			if (this.firstHytaleClass == null) {
				Class<?> hytaleClass = null;
				synchronized (this) {
					if (this.firstHytaleClass == null) {
						this.firstHytaleClass = hytaleClass = super.findClass(name);
					}
				}
				if (hytaleClass != null) {
					System.out.println("Loaded first class: " + hytaleClass.getName());
					return hytaleClass;
				}
			}
		}
		final String packageName = name.lastIndexOf('.') == -1 ?
				"" : name.substring(0, name.lastIndexOf('.'));
		if (getDefinedPackage(packageName) == null) {
			definePackage(packageName,
					this.hytaleExamplePackage.getSpecificationTitle(),
					this.hytaleExamplePackage.getSpecificationVersion(),
					this.hytaleExamplePackage.getSpecificationVendor(),
					this.hytaleExamplePackage.getImplementationTitle(),
					this.hytaleExamplePackage.getImplementationVersion(),
					this.hytaleExamplePackage.getImplementationVendor(),
					null);
		}
		try (InputStream inputStream = resource.openStream()) {
			byte[] bytes = inputStream.readAllBytes();
			if (this.classTransformer != null) {
				bytes = this.classTransformer.apply(name, bytes);
			}
			return this.defineClass(name, bytes, 0, bytes.length,
					this.firstHytaleClass.getProtectionDomain());
		} catch (IOException e) {
			throw new ClassNotFoundException(name, e);
		}
	}

	public void setClassTransformer(BiFunction<String, byte[], byte[]> classTransformer) {
		this.classTransformer = classTransformer;
	}
}
