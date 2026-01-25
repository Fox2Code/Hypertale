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

import org.gradle.StartParameter;
import org.gradle.TaskExecutionRequest;
import org.gradle.api.Project;
import org.gradle.internal.DefaultTaskExecutionRequest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

final class HypertaleIntellijIDEASupport {
	private static final String[] ideaAddDictionary = new String[]{
			"hytale", "hypixel", "hypertale", "mixin", "mixins",
	};

	static void installIdeaDictionaryOnIDEASync(Project target, Iterable<String> words) throws IOException {
		if (isIdeaSync()) installIdeaDictionary(target, words);
	}

	static void scheduleTaskBeforeIDEASync(Project target, String taskName) {
		if (isIdeaSync()) modifyGradleStartParameters(target, taskName);
	}

	private static void installIdeaDictionary(Project target, Iterable<String> words) throws IOException {
		File dotIdea = new File(target.getRootDir(), ".idea");
		if (!dotIdea.isDirectory()) {
			return;
		}
		File dictionaries = new File(dotIdea, "dictionaries");
		if (!dictionaries.isDirectory() && !dictionaries.mkdirs()) {
			return;
		}
		File projectDictionary = new File(dotIdea, "project.xml");
		LinkedHashSet<String> wordsUnique = new LinkedHashSet<>();
		if (projectDictionary.exists()) {
			String originalDictionary = Files.readString(
					projectDictionary.toPath(), StandardCharsets.UTF_8);
			int nextIndex;
			int nextEnd = 0;
			while ((nextIndex = originalDictionary.indexOf("<w>", nextEnd)) != -1 &&
					(nextEnd = originalDictionary.indexOf("</w>", nextIndex)) != -1) {
				wordsUnique.add(originalDictionary.substring(nextIndex + 3, nextEnd));
			}
		}
		Collections.addAll(wordsUnique, ideaAddDictionary);
		for (String word : words) {
			wordsUnique.add(word);
		}
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("""
				<component name="ProjectDictionaryState">
				  <dictionary name="project">
				    <words>""");
		for (String word : wordsUnique) {
			stringBuilder.append("\n      <w>").append(word).append("</w>");
		}
		stringBuilder.append("""
				    </words>
				  </dictionary>
				</component>""");
		Files.writeString(projectDictionary.toPath(),
				stringBuilder.toString(), StandardCharsets.UTF_8);
	}

	private static void modifyGradleStartParameters(Project target, String taskName) {
		final StartParameter startParameter = target.getGradle().getStartParameter();
		System.out.println(startParameter);
		final ArrayList<TaskExecutionRequest> taskRequests = new ArrayList<>(startParameter.getTaskRequests());
		taskRequests.addFirst(new DefaultTaskExecutionRequest(List.of(taskName)));
		startParameter.setTaskRequests(taskRequests);
	}

	private static boolean isIdeaSync() {
		return Boolean.parseBoolean(System.getProperty("idea.sync.active", "false"));
	}
}
