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

import com.fox2code.hypertale.init.HypertaleInfo;
import com.hypixel.hytale.plugin.early.ClassTransformer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class HookClassTransformer implements ClassTransformer {
	private static final boolean isPatcherProcess = Boolean.getBoolean("hypertale.patcherProcess");
	private static final HypertaleInfo hypertaleInfo = isPatcherProcess ? null : HypertaleInfo.findHypertale();

	static {
		if (!isPatcherProcess) {
			if (Boolean.getBoolean("hypertale.useInitWrapper")) {
				throw new IllegalStateException("HypertaleInit already loaded with loaded with: " +
						System.getProperty("hypertale.initMethod", "unknown"));
			}
			System.setProperty("hypertale.useInitWrapper", "true");
			System.setProperty("hypertale.initMethod", "late");
			System.setProperty("hypertale.initWrapperHytalePath",
					HypertaleInfo.getSourceFile(ClassTransformer.class).getAbsolutePath());
			System.getProperties().put("hypertale.initFunction", (Consumer<String[]>) args -> {
				try {
					Objects.requireNonNull(hypertaleInfo);
					HookClassLoader hookClassLoader = new HookClassLoader(
							new URL[]{hypertaleInfo.hypertale().toURI().toURL()},
							ClassTransformer.class.getPackage());
					System.getProperties().put("hypertale.initCLAddURL",
							(Consumer<URL>) hookClassLoader::addURLHelper);
					System.getProperties().put("hypertale.initCLSetClassTransformer",
							(Consumer<BiFunction<String, byte[], byte[]>>)
									hookClassLoader::setClassTransformer);
					Thread.currentThread().setContextClassLoader(hookClassLoader);
					HypertaleInfo.runHypertale(hookClassLoader,
							hypertaleInfo.mainClassName(),
							hypertaleInfo.hypertaleInitVer(), args);
				} catch (Throwable e) {
					sneakyThrow(new Throwable("Failed to start server!", e));
				}

				Thread.setDefaultUncaughtExceptionHandler((_, _) -> {});
				sneakyThrow(new Throwable("You should not be able to see this!"));
			});
		}
	}

	@Nonnull
	@Override
	public byte[] transform(@Nonnull String className, @Nonnull String internalClassName, @Nonnull byte[] bytes) {
		if (hypertaleInfo != null && "com.hypixel.hytale.LateMain".equals(className)) {
			try (InputStream inputStream = HookClassLoader.class.getResourceAsStream("LateMain.class")) {
				return Objects.requireNonNull(inputStream, "Missing LateMain.class").readAllBytes();
			} catch (IOException e) {
				throw new RuntimeException("Failed to read LateMain.class", e);
			}
		}
		return bytes;
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
		throw (E) e;
	}
}
