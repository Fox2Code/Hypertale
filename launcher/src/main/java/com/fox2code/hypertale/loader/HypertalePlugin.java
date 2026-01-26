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
package com.fox2code.hypertale.loader;

import com.fox2code.hypertale.commands.HypertaleCommand;
import com.fox2code.hypertale.launcher.BuildConfig;
import com.fox2code.hypertale.launcher.EarlyLogger;
import com.fox2code.hypertale.utils.HypertalePaths;
import com.fox2code.hypertale.utils.HypertalePlatform;
import com.fox2code.hypertale.utils.HypertaleSystemInfo;
import com.fox2code.hypertale.utils.SourceUtil;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ShutdownReason;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.Objects;

public final class HypertalePlugin extends JavaPlugin {
	private static final boolean INVALID_INSTALLATION =
			HypertalePlugin.class.getClassLoader() != JavaPlugin.class.getClassLoader();
	private static final String HYPERTALE_INIT = "init-" + BuildConfig.HYPERTALE_VERSION + ".jar";
	public static final Message HYPERTALE = Message.join(Message.raw("Hypertale").color(Color.MAGENTA));
	// Use 16, 256, and true color ANSI codes for color, if it is not supported, it us usually ignored!
	private static final String ANSI_MAGENTA = "\u001B[35;95m\u001B[38;5;201m\u001B[38;2;255;0;255m";
	private static final String ANSI_RESET = "\u001B[0m";

	public HypertalePlugin(@NonNull JavaPluginInit init) {
		super(init);
		if (!INVALID_INSTALLATION) {
			this.hypertale().setLoggerName(ANSI_MAGENTA + "Hypertale" + ANSI_RESET);
		}
		this.getLogger().atInfo().log("System information: " + HypertaleSystemInfo.SYSTEM);
		EarlyLogger.installLoggerFunction(this.getLogger().atInfo()::log);
		if (INVALID_INSTALLATION) {
			try {
				this.tryInstallHypertaleFromModFolder();
			} catch (Exception e) {
				this.getLogger().atSevere().withCause(e).log("Failed to setup Hypertale properly!");
			}
			HytaleServer.get().shutdownServer(ShutdownReason.UPDATE);
		}
	}

	@Override
	protected void setup() {
		if (INVALID_INSTALLATION) return;
		this.getCommandRegistry().registerCommand(new HypertaleCommand());
	}

	@Override
	protected void start() {
		if (INVALID_INSTALLATION) return;
		HypertaleModLoader.loadModsLate();
		this.getLogger().atInfo().log("Successfully loaded!");
	}

	private void tryInstallHypertaleFromModFolder() throws IOException {
		if (!HypertalePaths.hypertaleCache.isDirectory() && !HypertalePaths.hypertaleCache.mkdirs()) {
			this.getLogger().atSevere().log("Failed to create the \".hypertale\" directory!");
		}
		if (HypertalePlatform.getPlatform() == HypertalePlatform.WINDOWS) {
			// Windows file locking prevent in place upgrading, let's assume
			// the user has Hytale installed if Windows is detected!
			this.getLogger().atSevere().log(HypertalePaths.hypertaleJar.getName() +
					" need to be ran directly and should not be put in the mods folder!");
			try (PrintStream printStream = new PrintStream(new File("hypertale.bat").getAbsoluteFile())) {
				printStream.println("@echo off");
				printStream.println("java -jar \"" + HypertalePaths.hypertaleJar.getAbsolutePath() + "\"");
			}
		} else {
			// Linux & macOS allows us to edit files that are currently in use
			HypertaleConfig.load();
			this.getLogger().atWarning().log(HypertalePaths.hypertaleJar.getName() +
					" was installed incorrectly! Installing Hypertale properly...");
			File hytaleServer = SourceUtil.getSourceFile(JavaPlugin.class);
			File hypertaleInit = HypertalePaths.getHypertaleInitJar();
			File hypertaleFile = HypertalePaths.hypertaleJar;
			if (hypertaleInit == null) {
				File hytaleServerDestination = new File(
						hytaleServer.getParentFile(), HypertaleConfig.secondaryJarName);
				if (hytaleServer.getName().equals(hytaleServerDestination.getName())) {
					if (HypertalePaths.hypertaleCache.isDirectory() || HypertalePaths.hypertaleCache.mkdirs()) {
						HypertaleConfig.save();
					}
					throw new IOException("Secondary server name is the current server name," +
							"please set secondaryJarName to another jar name in .hypertale/hypertale.properties");
				}

				if (hytaleServerDestination.exists() && !hytaleServerDestination.delete()) {
					hytaleServerDestination.deleteOnExit();
					throw new IOException("Failed to delete " + hytaleServerDestination.getName());
				}
				Files.copy(hytaleServer.toPath(), hytaleServerDestination.toPath());
				if (hytaleServer.exists() && !hytaleServer.delete()) {
					hytaleServerDestination.deleteOnExit();
					throw new IOException("Failed to delete " + hytaleServer.getName());
				}
			} else {
				// Set HypertaleInit as the target of the copy!
				hytaleServer = hypertaleInit;
			}
			if (HypertalePlugin.class.getClassLoader().getResource(HYPERTALE_INIT) == null) {
				// Pretend the lack of init.jar isn't an issue!
				Files.copy(hypertaleFile.toPath(), hytaleServer.toPath());
			} else try (InputStream inputStream = new BufferedInputStream(
					Objects.requireNonNull(HypertalePlugin.class.getClassLoader()
									.getResourceAsStream(HYPERTALE_INIT),
							"Failed to load " + HYPERTALE_INIT + " from " +
									HypertalePaths.hypertaleJar.getName()))) {
				Files.copy(inputStream, hytaleServer.toPath());
			}
			this.getLogger().atInfo().log("Hypertale installed successfully!");
			this.getLogger().atInfo().log("The server will be stopped and Hypertale will be active on restart!");
		}
	}
}
