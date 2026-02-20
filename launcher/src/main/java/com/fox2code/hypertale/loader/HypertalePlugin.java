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
import com.fox2code.hypertale.event.AntiCheatEvent;
import com.fox2code.hypertale.launcher.BuildConfig;
import com.fox2code.hypertale.launcher.EarlyLogger;
import com.fox2code.hypertale.utils.HypertalePaths;
import com.fox2code.hypertale.utils.HypertalePlatform;
import com.fox2code.hypertale.utils.HypertaleSystemInfo;
import com.fox2code.hypertale.utils.SourceUtil;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ShutdownReason;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.Objects;

public final class HypertalePlugin extends JavaPlugin {
	public static final String EDITION = System.getProperty("hypertale.edition", "OSS");
	public static final boolean PREMIUM = Boolean.getBoolean("hypertale.premium");
	private static final boolean INVALID_INSTALLATION =
			HypertalePlugin.class.getClassLoader() != JavaPlugin.class.getClassLoader();
	private static final boolean USE_HYPERTALE_INIT = Boolean.getBoolean("hypertale.useInitWrapper");
	private static final String HYPERTALE_INIT = "init-" + BuildConfig.HYPERTALE_VERSION + ".jar";
	public static final Message HYPERTALE = Message.join(Message.raw("Hypertale").color(Color.MAGENTA));
	// Use 16, 256, and true color ANSI codes for color, if it is not supported, it is usually ignored!
	private static final String ANSI_MAGENTA = "\u001B[35;95m\u001B[38;5;201m\u001B[38;2;255;0;255m";
	private static HypertalePlugin instance;

	public static HypertalePlugin get() {
		return instance;
	}

	public HypertalePlugin(@NonNull JavaPluginInit init) {
		if (instance != null) throw new IllegalStateException("HypertalePlugin is already loaded!");
		super(init);
		instance = this;
		if (!INVALID_INSTALLATION) {
			if (inHypertalePatcherProcess) {
				throw new Error("HypertalePlugin should not be loaded inside the patcher process!");
			}
			this.hypertale().setLoggerName(ANSI_MAGENTA + "Hypertale");
		}
		this.getLogger().atInfo().log("System information: " + HypertaleSystemInfo.SYSTEM);
		EarlyLogger.installLoggerFunction(this.getLogger().atInfo()::log);
		if (INVALID_INSTALLATION) {
			try {
				this.tryInstallHypertaleFromModFolder();
			} catch (Exception e) {
				this.getLogger().atSevere().withCause(e).log("Failed to setup Hypertale properly!");
			}
			if (Constants.SINGLEPLAYER) {
				HytaleServer.get().shutdownServer(ShutdownReason.UPDATE
						.withMessage("Please reopen your world to join!"));
			} else {
				HytaleServer.get().shutdownServer(ShutdownReason.UPDATE);
			}
		}
	}

	@Override
	protected void setup() {
		if (INVALID_INSTALLATION) return;
		this.getCommandRegistry().registerCommand(new HypertaleCommand());
		this.getEventRegistry().registerGlobal(
				EventPriority.LAST, AntiCheatEvent.class,
				this::onUnhandledAntiCheatEvent);
	}

	@Override
	protected void start() {
		if (INVALID_INSTALLATION) return;
		HypertaleModLoader.loadModsLate();
		this.getLogger().atInfo().log("Successfully loaded!");
		if (PREMIUM) {
			this.getLogger().atInfo().log("Thank you for supporting Hypertale!");
		}
	}

	private void onUnhandledAntiCheatEvent(AntiCheatEvent event) {
		if (event.willDisconnect() && event.getPlayerRef().getPacketHandler().stillActive()) {
			this.getLogger().atSevere().log("Unhandled anti-cheat event for " + event.getPlayerRef().getUsername());
			this.getLogger().atSevere().log("Certainty: " + event.getCertainty() + ", action: " + event.getAction());
			this.getLogger().atSevere().log("Message: " + event.getMessage().getAnsiMessage());
			this.getLogger().atSevere().log("CheckID: " + event.getId());
			if (event.getAction() == AntiCheatEvent.AntiCheatAction.AWAITING_BAN) {
				// I should implement a proper ban provider in hytale for this.
				CommandManager.get().handleCommand(ConsoleSender.INSTANCE,
						"ban " + event.getPlayerRef().getUsername() + " Hypertale: Unhandled anti-cheat event!").join();
			} else {
				event.getPlayerRef().getPacketHandler().disconnect("Hypertale: Unhandled anti-cheat event!");
			}
		}
	}

	private void tryInstallHypertaleFromModFolder() throws IOException {
		if (!HypertalePaths.hypertaleCache.isDirectory() && !HypertalePaths.hypertaleCache.mkdirs()) {
			this.getLogger().atSevere().log("Failed to create the \".hypertale\" directory!");
		}
		if (!HypertalePaths.hytaleEarlyPlugins.isDirectory() && !HypertalePaths.hytaleEarlyPlugins.mkdirs()) {
			this.getLogger().atSevere().log("Failed to create the \"earlyplugins\" directory!");
		}
		if (USE_HYPERTALE_INIT) {
			this.getLogger().atInfo().log("Hypertale is already installed but not working?");
			this.getLogger().atWarning().withCause(new Throwable()).log();
			return;
		}
		if (HypertalePlatform.getPlatform() != HypertalePlatform.WINDOWS &&
				!HypertalePaths.hypertaleCacheJust.isFile() &&
				!Constants.SINGLEPLAYER) {
			if (!HypertalePaths.hypertaleCacheJust.createNewFile()) {
				this.getLogger().atWarning().log("Failed to make the install marker!");
			}
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
		} else {
			this.getLogger().atInfo().log("Previous Hypertale installation was not appropriate for your server host!");
			File earlyPluginFolder = HypertalePaths.hytaleEarlyPlugins;
			File userData = HypertalePaths.hypertaleJar.getParentFile().getParentFile();
			this.getLogger().atInfo().log("UserData folder: " + userData.getPath());
			if (Constants.SINGLEPLAYER && new File(userData, "Settings.json").exists()) {
				earlyPluginFolder = new File(userData, "EarlyPlugins");
				if (!earlyPluginFolder.isDirectory() && !earlyPluginFolder.mkdirs()) {
					throw new IOException("Failed to create early plugins folder!");
				}
			}
			File hypertaleInit = new File(earlyPluginFolder, "HypertaleInit.jar");
			try (InputStream inputStream = new BufferedInputStream(
					Objects.requireNonNull(HypertalePlugin.class.getClassLoader()
									.getResourceAsStream(HYPERTALE_INIT),
							"Failed to load " + HYPERTALE_INIT + " from " +
									HypertalePaths.hypertaleJar.getName()))) {
				Files.copy(inputStream, hypertaleInit.toPath());
			}
			this.getLogger().atInfo().log("Hypertale installed successfully using late entry point!");
			this.getLogger().atInfo().log("Please note that some hypertale features may not work properly in this mode!");
			this.getLogger().atInfo().log("The server will be stopped and Hypertale will be active on restart!");
		}
	}
}
