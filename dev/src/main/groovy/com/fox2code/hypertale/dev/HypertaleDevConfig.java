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


import java.util.*;

// Cannot be final due to being a Gradle extension
public class HypertaleDevConfig {
	private static final boolean DECOMPILE_SUPPORTED = true;
	private boolean configImmutable = false;
	private String hytaleBranch = "release";
	private boolean useUnmodifiedHytale = false; // To use unmodified HytaleServer.jar file
	private boolean decompileHypertale = // Only decompile sources if we have no CI to not waste server time
			System.getenv("CI") == null && System.getenv("JITPACK") == null;
	private boolean useSpotless = true;
	private boolean includeLicenseHeader = true;
	private boolean useBundling = true;
	private String buildConfigPackage = null;
	private final LinkedHashMap<String, String> buildConfig = new LinkedHashMap<>();
	final LinkedHashSet<String> customDictionary = new LinkedHashSet<>();
	Properties localProperties;

	public HypertaleDevConfig() {
		this.buildConfig.put("MOD_VERSION", null);
	}

	public String getHytaleBranch() {
		return this.hytaleBranch;
	}

	public void setHytaleBranch(String hytaleBranch) {
		this.checkConfigMutable();
		if ("prerelease".equals(hytaleBranch)) {
			this.hytaleBranch = "pre-release";
		} else if ("release".equals(hytaleBranch) || "pre-release".equals(hytaleBranch)) {
			this.hytaleBranch = hytaleBranch;
		} else {
			throw new IllegalArgumentException(
					"Invalid branch \"" + hytaleBranch +
							"\" expected \"release\" or \"pre-release\"");
		}
	}

	public boolean getUseUnmodifiedHytale() {
		return this.useUnmodifiedHytale;
	}

	public void setUseUnmodifiedHytale(boolean useUnmodifiedHytale) {
		this.checkConfigMutable();
		this.useUnmodifiedHytale = useUnmodifiedHytale;
	}

	public boolean getDecompileHytale() {
		return this.decompileHypertale && HypertaleDevConfig.DECOMPILE_SUPPORTED;
	}

	public void setDecompileHytale(boolean decompileHypertale) {
		this.checkConfigMutable();
		this.decompileHypertale = decompileHypertale;
	}

	public boolean getUseSpotless() {
		return this.useSpotless;
	}

	public void setUseSpotless(boolean useSpotless) {
		this.checkConfigMutable();
		this.useSpotless = useSpotless;
	}

	public boolean getIncludeLicenseHeader() {
		return this.includeLicenseHeader;
	}

	public void setIncludeLicenseHeader(boolean includeLicenseHeader) {
		this.checkConfigMutable();
		this.includeLicenseHeader = includeLicenseHeader;
	}

	public boolean getUseBundling() {
		return this.useBundling;
	}

	public void setUseBundling(boolean useBundling) {
		this.checkConfigMutable();
		this.useBundling = useBundling;
	}

	public String getBuildConfigPackage() {
		return this.buildConfigPackage;
	}

	public void setBuildConfigPackage(String buildConfigPackage) {
		this.checkConfigMutable();
		if (buildConfigPackage != null && (buildConfigPackage.isEmpty() ||
				buildConfigPackage.startsWith(".") || buildConfigPackage.endsWith(".") ||
				!buildConfigPackage.matches("[a-zA-Z0-9_.]+")) ||
				"com.fox2code.hypertale.launcher".equals(buildConfigPackage)) {
			throw new IllegalArgumentException(
					"Invalid package name \"" + buildConfigPackage + "\"");
		}
		this.buildConfigPackage = buildConfigPackage;
	}

	public Map<String, String> getBuildConfig() {
		return this.configImmutable ?
				Collections.unmodifiableMap(this.buildConfig) :
				this.buildConfig;
	}

	public void buildConfig(Object key, Object value) {
		this.checkConfigMutable();
		Objects.requireNonNull(key, "key must be non null");
		String keyString = String.valueOf(key);
		if (!keyString.matches("[a-zA-Z_][a-zA-Z0-9_]+")) {
			throw new IllegalArgumentException(
					"Invalid build config key \"" + keyString + "\"");
		}
		this.buildConfig.put(keyString,
				value == null ? null : String.valueOf(value));
	}

	public void addWord(String word) {
		this.checkConfigMutable();
		this.customDictionary.add(word);
	}

	public Properties getLocalProperties() {
		return localProperties;
	}

	void internalMakeImmutable() {
		this.configImmutable = true;
	}

	public void internalMakeImmutableForTesting() {
		if (this.localProperties != null) {
			throw new IllegalStateException("Not ran from testing!");
		}
		this.configImmutable = true;
	}

	private void checkConfigMutable() {
		if (this.configImmutable) {
			throw new IllegalStateException("Trying to modify config after it has been loaded");
		}
	}
}
