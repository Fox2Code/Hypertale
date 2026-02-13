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
package com.fox2code.hypertale.plugin;

import com.fox2code.hypertale.loader.HypertaleModLoader;
import com.fox2code.hypertale.patcher.mixin.MixinLoader;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.plugin.PluginBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public final class HypertaleBasePlugin {
	private final boolean preLoaded;
	private final boolean hasMixin;
	private final HytaleLogger originalLogger;
	private HytaleLogger logger;

	public HypertaleBasePlugin(PluginBase pluginBase, HytaleLogger originalLogger) {
		this.preLoaded = HypertaleModLoader.isPreloadedPlugin(pluginBase.getIdentifier());
		this.hasMixin = MixinLoader.modHasMixins(pluginBase.getIdentifier().toString());
		this.originalLogger = originalLogger;
		this.logger = this.originalLogger;
		// Fix preload plugins logger propagating to parent!
		if (!pluginBase.getIdentifier().getGroup().equals("Hytale")) {
			this.originalLogger.setPropagatesSentryToParent(false);
		}
	}

	public boolean isPreLoaded() {
		return this.preLoaded;
	}

	public boolean hasMixin() {
		return this.hasMixin;
	}

	@Nonnull
	public HytaleLogger getLogger() {
		return this.logger;
	}

	@Nonnull
	public HytaleLogger getOriginalLogger() {
		return this.originalLogger;
	}

	public void setLogger(@Nullable HytaleLogger logger) {
		if (logger != null && logger != this.originalLogger) {
			logger.setPropagatesSentryToParent(false);
			this.logger = logger;
		} else {
			this.logger = this.originalLogger;
		}
	}

	@Nonnull
	public String getLoggerName() {
		return this.logger.getName();
	}

	public void setLoggerName(@Nonnull String loggerName) {
		Objects.requireNonNull(loggerName, "Invalid null logger name!");
		this.setLogger(HytaleLogger.get(loggerName));
	}

	public void setLoggerName(@Nonnull Message loggerName) {
		this.setLoggerName(loggerName.getAnsiMessage());
	}
}
