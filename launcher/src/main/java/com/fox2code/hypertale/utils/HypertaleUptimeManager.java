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
package com.fox2code.hypertale.utils;

import com.fox2code.hypertale.loader.HypertalePlugin;
import com.hypixel.hytale.server.core.HytaleServer;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages the uptime reporting and notification for subscribers of the system.
 * This class is designed to track the uptime of the server and notify registered
 * listeners periodically about the updated uptime.
 * <p>
 * The updates are delivered to listeners in a concurrent-safe manner, using a 
 * scheduled task to compute and distribute the uptime at fixed intervals.
 * <p>
 * This class cannot be instantiated and provides only static methods for
 * managing uptime listeners.
 * <p>
 * Thread-safety is ensured using synchronization and thread-safe data structures.
 * <p>
 * Key responsibilities of this class include:
 * - Registering and deregistering listeners for uptime notifications.
 * - Maintaining a cache of listener references to avoid direct interaction
 *   with active listeners during ticking.
 * - Calculating and presenting uptime in a human-readable format.
 * <p>
 * Listeners registering with this manager receive two key values:
 * - The uptime in seconds.
 * - The formatted string representation of the uptime.
 */
public final class HypertaleUptimeManager {
	private static final Object stateLock = new Object();
	private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private static final HashSet<HypertaleUptimeReceiver> uptimeReceivers = new HashSet<>();
	private static final ArrayList<WeakReference<HypertaleUptimeReceiver>>
			uptimeReceiversCache = new ArrayList<>();
	private static final long serverStartEpoch = HytaleServer.get().getBoot().getEpochSecond();
	private static volatile ScheduledFuture<?> scheduledFuture;
	private static volatile boolean dirty = true;
	private static long lastUptime;

	private HypertaleUptimeManager() {}

	public static void addUptimeListener(HypertaleUptimeReceiver uptimeReceiver) {
		Objects.requireNonNull(uptimeReceiver);
		synchronized (stateLock) {
			uptimeReceivers.add(uptimeReceiver);
			if (scheduledFuture == null) {
				scheduledFuture = executor.scheduleAtFixedRate(
						HypertaleUptimeManager::onTicking,
						0, 100, TimeUnit.MILLISECONDS);
			}
			dirty = true;
		}
	}

	public static void removeUptimeListener(HypertaleUptimeReceiver uptimeReceiver) {
		Objects.requireNonNull(uptimeReceiver);
		synchronized (stateLock) {
			uptimeReceivers.remove(uptimeReceiver);
			if (scheduledFuture != null && uptimeReceivers.isEmpty()) {
				scheduledFuture.cancel(true);
				scheduledFuture = null;
			}
			dirty = true;
		}
	}

	private static void onTicking() {
		if (dirty) {
			synchronized (stateLock) {
				uptimeReceiversCache.clear();
				uptimeReceivers.stream().map(WeakReference::new)
						.forEach(uptimeReceiversCache::add);
				dirty = false;
			}
		}
		if (uptimeReceiversCache.isEmpty()) {
			return;
		}
		long uptime = Instant.now().getEpochSecond() - serverStartEpoch;
		if (uptime != lastUptime) {
			Thread currentThread = Thread.currentThread();
			String uptimeText = HypertaleTextUtil.makeUptimeString(uptime);
			Iterator<WeakReference<HypertaleUptimeReceiver>> iterator = uptimeReceiversCache.iterator();
			while (iterator.hasNext()) {
				HypertaleUptimeReceiver uptimeReceiver = iterator.next().get();
				if (uptimeReceiver == null) {
					iterator.remove();
					continue;
				}
				try {
					uptimeReceiver.onReceiveUptime(uptime, uptimeText);
				} catch (Exception e) {
					HypertalePlugin.get().getLogger().atSevere().withCause(e).log("Error in uptime ticking");
					iterator.remove();
				}
				if (currentThread.isInterrupted() && iterator.hasNext()) {
					return;
				}
			}
			lastUptime = uptime;
		}
	}
}
