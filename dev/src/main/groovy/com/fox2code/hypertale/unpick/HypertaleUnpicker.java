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
package com.fox2code.hypertale.unpick;

import com.fox2code.hypertale.launcher.EarlyLogger;
import com.fox2code.hypertale.patcher.*;
import com.fox2code.hypertale.utils.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class HypertaleUnpicker {
	public static void patch(
			File in, File out) throws IOException {
		if (!out.exists() && !out.createNewFile()) {
			throw new IOException("Failed to create output file!");
		}
		try (JarFile jarFile = new JarFile(in)) {
			// progress is false shared... but only run every 1000ms
			final long[] progress = new long[]{0, 0};
			progress[1] = jarFile.stream().filter(HypertaleUnpicker::canUnpick).count();
			HypertaleUnpickConstants hypertaleUnpickConstants =
					HypertaleUnpickConstants.fromPatchedJar(jarFile);
			runLogProgress(progress);
			// Do patch
			try (ZipInputStream zipInputStream = new ZipInputStream(
					new BufferedInputStream(new FileInputStream(in)));
				 ZipOutputStream zipOutputStream = new ZipOutputStream(
						 new BufferedOutputStream(new FileOutputStream(out)))) {
				zipOutputStream.setLevel(9);
				ZipEntry zipEntry;
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(131072);
				while ((zipEntry = zipInputStream.getNextEntry()) != null) {
					final String entryName = zipEntry.getName();
					if (canUnpick(zipEntry)) {
						patchAndInsert(jarFile, byteArrayOutputStream,
								zipOutputStream, zipInputStream,
								entryName, hypertaleUnpickConstants);
						progress[0]++;
					}
					zipInputStream.closeEntry();
				}
				// Finalize
				EarlyLogger.log("Patching at " + progress[0] + "/" + progress[1]);
				zipOutputStream.finish();
			}
		}
	}

	private static boolean canUnpick(ZipEntry zipEntry) {
		final String entryName = zipEntry.getName();
		return !((zipEntry.isDirectory() && zipEntry.getSize() <= 0) ||
				!entryName.startsWith("com/hypixel/") ||
				!entryName.endsWith(".class"));
	}

	private static void runLogProgress(final long[] progress) {
		Thread logThread = new Thread(() -> {
			try {
				int loop = 0;
				while (progress[0] != progress[1]) {
					EarlyLogger.log("Unpicking at " + progress[0] + "/" + progress[1]);
					//noinspection BusyWait
					Thread.sleep(loop++ > 4 ? 1000 : 500);
				}
			} catch (InterruptedException ignored) {}
		}, "Hypertale - Logger progress thread");
		logThread.setPriority(Thread.MIN_PRIORITY);
		logThread.setDaemon(true);
		logThread.start();
	}

	private static void patchAndInsert(
			JarFile jarFile, ByteArrayOutputStream byteArrayOutputStream,
			ZipOutputStream zipOutputStream, InputStream inputStream,
			String path, HypertaleUnpickConstants hypertaleUnpickConstants) throws IOException {
		if (path.endsWith(".class")) {
			byteArrayOutputStream.reset();
			IOUtils.copy(inputStream, byteArrayOutputStream);
			ClassReader classReader = new ClassReader(byteArrayOutputStream.toByteArray());
			ClassNode classNode = new ClassNode();
			classReader.accept(classNode, 0);
			try {
				classNode = HypertaleUnpickFixes.fix(jarFile, classNode);
				HypertaleUnpickData.patchForDev(hypertaleUnpickConstants, classNode);
			} catch (Exception e) {
				throw new RuntimeException("Failed to patch " + classNode.name.replace('/', '.'), e);
			}
			zipOutputStream.putNextEntry(new ZipEntry(path));
			SafeClassWriter classWriter;
			if ((classNode.access & TransformerUtils.ACC_COMPUTE_FRAMES) != 0) {
				classNode.access &= ~TransformerUtils.ACC_COMPUTE_FRAMES;
				classWriter = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES);
			} else {
				classWriter = new SafeClassWriter(0);
			}
			try {
				classNode.accept(classWriter);
				byte[] compiled = classWriter.toByteArray();
				zipOutputStream.write(compiled);
				zipOutputStream.closeEntry();
			} catch (Exception e) {
				throw new RuntimeException("Failed to compile " + classNode.name.replace('/', '.'), e);
			}
		} else {
			zipOutputStream.putNextEntry(new ZipEntry(path));
			IOUtils.copy(inputStream, zipOutputStream);
			zipOutputStream.closeEntry();
		}
	}
}
