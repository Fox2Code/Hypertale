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
package com.fox2code.hypertale.dashboard;

import com.fox2code.hypertale.launcher.BuildConfig;
import com.fox2code.hypertale.utils.HypertaleTextUtil;
import com.fox2code.hypertale.utils.HypertaleUptimeManager;
import com.fox2code.hypertale.utils.HypertaleUptimeReceiver;
import com.fox2code.hypertale.utils.HytaleVersion;
import com.fox2code.hypertale.utils.jvm.HypertaleJVMHelper;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPage;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.util.Objects;

public final class HypertaleDashboard extends CustomUIPage implements HypertaleUptimeReceiver {
	private static final Value<String> label = Value.ref("Pages/HypertaleTheme.ui", "StatLabelStyle");
	private static final Value<String> labelFine = Value.ref("Pages/HypertaleTheme.ui", "StatLabelFineStyle");
	private static final Value<String> labelWarn = Value.ref("Pages/HypertaleTheme.ui", "StatLabelWarnStyle");
	private static final Value<String> labelSerious = Value.ref("Pages/HypertaleTheme.ui", "StatLabelSeriousStyle");
	private static final Value<String> labelSevere = Value.ref("Pages/HypertaleTheme.ui", "StatLabelSevereStyle");

	private static final long MEM_1536MB = 1024 * 1024 * 1536;
	private static final long MEM_512MB = 1024 * 1024 * 512;
	private static final long MEM_128MB = 1024 * 1024 * 128;
	private static final long TIME_12_HOURS = 12 * 60 * 60;
	private static final long TIME_24_HOURS = 24 * 60 * 60;
	private static final long TIME_48_HOURS = 48 * 60 * 60;
	private static final long TIME_1_MINUTE = 60;
	private final PageManager cachedPageManager;

	public HypertaleDashboard(@NonNull PlayerRef playerRef) {
		super(playerRef, CustomPageLifetime.CanDismiss);
		this.cachedPageManager = Objects.requireNonNull(playerRef.hypertale().getPlayer()).getPageManager();
	}

	@Override
	public void build(@NonNull Ref<EntityStore> ref, @NonNull UICommandBuilder commandBuilder,
					  @NonNull UIEventBuilder eventBuilder, @NonNull Store<EntityStore> store) {
		commandBuilder.append("Pages/HypertaleDashboard.ui");
		commandBuilder.set("#Description.Text", "Hypertale " + BuildConfig.HYPERTALE_VERSION + "\n" +
				"Running Hytale " + HytaleVersion.HYTALE_VERSION);
		long uptime = Instant.now().getEpochSecond() - HytaleServer.get().getBoot().getEpochSecond();
		buildUpdate(commandBuilder, uptime, HypertaleTextUtil.makeUptimeString(uptime));
		HypertaleUptimeManager.addUptimeListener(this);
		eventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
				"#CloseButton", new EventData());
	}

	public static void buildUpdate(@NonNull UICommandBuilder commandBuilder, long uptime, @NonNull String uptimeText) {
		commandBuilder.set("#StatPlayers.Text", String.valueOf(Universe.get().getPlayerCount()));
		commandBuilder.set("#StatUptime.Text", uptimeText);
		commandBuilder.set("#StatUptime.Style", uptime >= TIME_48_HOURS ? labelSevere :
				uptime >= TIME_24_HOURS ? labelSerious :
						uptime >= TIME_12_HOURS ? labelWarn :
								uptime >= TIME_1_MINUTE ? labelFine : label);
		double cpuLoad = HypertaleJVMHelper.get().getCpuLoad();
		commandBuilder.set("#StatCPU.Text", HypertaleTextUtil.makeCpuString(cpuLoad));
		commandBuilder.set("#StatCPU.Style", cpuLoad >= 0.9D ? labelSevere :
				cpuLoad >= 0.75D ? labelSerious : cpuLoad >= 0.5D ? labelWarn : labelFine);
		long maxMemory = HypertaleJVMHelper.get().getMaxMemory();
		long usedMemory = HypertaleJVMHelper.get().getUsedMemory();
		long freeMemory = HypertaleJVMHelper.get().getFreeMemory();
		double ramUsagePercent = (usedMemory * 100.0) / maxMemory;
		commandBuilder.set("#StatMemory.Text", String.format("%s / %s (%.2f%%)",
				HypertaleTextUtil.makeMemoryString(usedMemory),
				HypertaleTextUtil.makeMemoryString(maxMemory),
				ramUsagePercent));
		commandBuilder.set("#StatMemory.Style",
				(ramUsagePercent > 95.0D || freeMemory < MEM_128MB) ? labelSevere :
						(ramUsagePercent > 90.0 || freeMemory < MEM_512MB) ? labelSerious :
								(ramUsagePercent > 80.0 || freeMemory < MEM_1536MB) ? labelWarn : labelFine);
	}

	@Override
	public void handleDataEvent(@NonNull Ref<EntityStore> ref, @NonNull Store<EntityStore> store, String rawData) {
		Player player = this.playerRef.hypertale().getPlayer();
		if (player != null) player.getPageManager().setPage(ref, store, Page.None);
	}

	@Override
	public void onReceiveUptime(long uptime, String uptimeText) {
		UICommandBuilder commandBuilder = new UICommandBuilder();
		buildUpdate(commandBuilder, uptime, uptimeText);
		// This works exactly like sendUpdate except it supports async calling
		this.cachedPageManager.updateCustomPage(new CustomPage(
				this.getClass().getName(), false, false,
				this.lifetime, commandBuilder.getCommands(),
				UIEventBuilder.EMPTY_EVENT_BINDING_ARRAY));
	}

	@Override
	public void onDismiss(@NonNull Ref<EntityStore> ref, @NonNull Store<EntityStore> store) {
		HypertaleUptimeManager.removeUptimeListener(this);
	}
}
