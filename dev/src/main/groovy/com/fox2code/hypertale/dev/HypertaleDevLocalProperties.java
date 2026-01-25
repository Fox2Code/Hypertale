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
package com.fox2code.hypertale.dev;

import com.fox2code.hypertale.utils.HypertalePlatform;
import org.gradle.api.Project;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class HypertaleDevLocalProperties {
	public static Properties loadProperties(Project project) throws IOException {
		final File localPropertiesFile = new File(project.getRootDir(), "local.properties");
		Properties localProperties = new Properties();
		if (localPropertiesFile.isFile() && localPropertiesFile.canRead()) {
			try (InputStreamReader inputStreamReader = new InputStreamReader(
					new BufferedInputStream(new FileInputStream(localPropertiesFile)),
					StandardCharsets.UTF_8)) {
				localProperties.load(inputStreamReader);
			}
		}
		if (localProperties.isEmpty() && project.hasProperty("hytale_home")) {
			File localHytaleHome =  new File(String.valueOf(
					project.findProperty("hytale_home"))).getAbsoluteFile();
			File platformHytaleHome = HypertalePlatform.getPlatform().getHytaleHome();
			if (!localHytaleHome.exists() && platformHytaleHome.exists() &&
					!localPropertiesFile.exists() && localPropertiesFile.createNewFile()) {
				localProperties.setProperty("hytale_home", platformHytaleHome.getPath());
				try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
						new BufferedOutputStream(new FileOutputStream(localPropertiesFile)),
						StandardCharsets.UTF_8)) {
					localProperties.store(outputStreamWriter, null);
				}
			} else if (localHytaleHome.exists()) {
				localProperties.setProperty("hytale_home", localHytaleHome.getAbsolutePath());
			}
		}
		return localProperties;
	}
}
