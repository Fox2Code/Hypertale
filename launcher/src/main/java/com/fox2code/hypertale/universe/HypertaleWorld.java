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
package com.fox2code.hypertale.universe;

import com.fox2code.hypertale.annotations.AsyncSafe;
import com.hypixel.fastutil.longs.Long2ObjectConcurrentHashMap;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class HypertaleWorld {
	private final World world;
	private final ChunkStore chunkStore;
	private final Object playerCacheLock;
	private volatile WeakReference<List<Player>> playersReference;
	private final Long2ObjectConcurrentHashMap<WeakReference<WorldChunk>> worldChunkCache;

	public HypertaleWorld(World world) {
		this.world = world;
		this.chunkStore = world.getChunkStore();
		this.playerCacheLock = new Object();
		this.worldChunkCache = new Long2ObjectConcurrentHashMap<>(true, ChunkUtil.NOT_FOUND);
	}

	@AsyncSafe
	public World getWorld() {
		return this.world;
	}

	@AsyncSafe
	public void invalidatePlayerCache() {
		synchronized (this.playerCacheLock) {
			this.playersReference = null;
		}
	}

	/**
	 * This method returns a read-only list of all players currently in the world.
	 * <br/>
	 * It is faster than the old {@code World#getPlayers()} method!
	 * <br/>
	 * It is also thread-safe and can be used in any thread without causing issues.
	 *
	 * @return the current player list!
	 */
	@AsyncSafe
	public List<Player> getPlayers() {
		WeakReference<List<Player>> playersReference = this.playersReference;
		List<Player> playersCache;
		// Check cache async first
		if (playersReference != null && (playersCache = playersReference.get()) != null) {
			return playersCache;
		} else if (this.world.getPlayerRefs().isEmpty()) {
			return Collections.emptyList();
		}
		// Check cache sync if failed
		List<Player> players = null;
		final boolean inThread;
		synchronized (this.playerCacheLock) {
			playersReference = this.playersReference;
			if (playersReference != null && (playersCache = playersReference.get()) != null) {
				return playersCache;
			}
			inThread = this.world.isInThread();
			if (inThread) {
				final ObjectArrayList<Player> playersMut = new ObjectArrayList<>(32);
				this.world.getEntityStore().getStore().forEachChunk(Player.getComponentType(), (chunk, _) -> {
					playersMut.ensureCapacity(playersMut.size() + chunk.size());
					for (int index = 0; index < chunk.size(); ++index) {
						playersMut.add(chunk.getComponent(index, Player.getComponentType()));
					}
				});

				players = Collections.unmodifiableList(playersMut);
				this.playersReference = new WeakReference<>(players);
			}
		}
		if (inThread) {
			return players;
		} else {
			return CompletableFuture.supplyAsync(this::getPlayers, this.world).join();
		}
	}

	@AsyncSafe
	public void invalidateChunkCache(long index) {
		this.worldChunkCache.remove(index);
	}

	@AsyncSafe
	public @Nullable WorldChunk getChunkIfInMemory(long index) {
		WeakReference<WorldChunk> worldChunkWeakReference =
				this.worldChunkCache.getOrDefault(index, null);
		WorldChunk cachedWorldChunk;
		Ref<ChunkStore> cachedReference;
		if (worldChunkWeakReference != null &&
				(cachedWorldChunk = worldChunkWeakReference.get()) != null &&
				(cachedReference = cachedWorldChunk.getReference()) != null &&
				cachedReference.isValid()) {
			return cachedWorldChunk;
		} else if (worldChunkWeakReference != null) {
			this.worldChunkCache.remove(index);
		}
		Ref<ChunkStore> reference = this.world.getChunkStore().getChunkReference(index);
		if (reference == null) {
			return null;
		} else if (!this.world.isInThread()) {
			return CompletableFuture.supplyAsync(() ->
					this.getChunkIfInMemory(index), this.world).join();
		} else {
			WorldChunk worldChunk = this.chunkStore.getStore()
					.getComponent(reference, WorldChunk.getComponentType());
			if (worldChunk != null) {
				this.worldChunkCache.put(index, new WeakReference<>(worldChunk));
			}
			return worldChunk;
		}
	}

	@AsyncSafe
	public void onShutdown() {
		synchronized (this.playerCacheLock) {
			this.playersReference = null;
		}
		this.worldChunkCache.clear();
	}
}
