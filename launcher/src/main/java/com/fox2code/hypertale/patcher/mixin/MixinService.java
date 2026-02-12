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
package com.fox2code.hypertale.patcher.mixin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.LoggerAdapterAbstract;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.util.IConsumer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;

public final class MixinService extends MixinServiceAbstract implements IMixinService,
		IClassProvider, IClassBytecodeProvider, ITransformerProvider, IClassTracker {
	private static final ContainerHandleVirtual containerHandleVirtual = new ContainerHandleVirtual("Hypertale");
	private IConsumer<MixinEnvironment.Phase> phaseConsumer;

	public MixinService() {}

	@Override
	@SuppressWarnings("deprecation")
	public void wire(MixinEnvironment.Phase phase, IConsumer<MixinEnvironment.Phase> phaseConsumer) {
		super.wire(phase, phaseConsumer);
		this.phaseConsumer = phaseConsumer;
	}

	private ClassLoader getClassLoader() {
		return MixinService.class.getClassLoader();
	}

	@Override
	public String getName() {
		return "FoxMixinService";
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public IClassProvider getClassProvider() {
		return this;
	}

	@Override
	public IClassBytecodeProvider getBytecodeProvider() {
		return this;
	}

	@Override
	public ITransformerProvider getTransformerProvider() {
		return this;
	}

	@Override
	public IClassTracker getClassTracker() {
		return this;
	}

	@Override
	public IMixinAuditTrail getAuditTrail() {
		return null;
	}

	@Override
	public Collection<String> getPlatformAgents() {
		return Collections.emptyList();
	}

	@Override
	public IContainerHandle getPrimaryContainer() {
		return containerHandleVirtual;
	}

	@Override
	public Collection<IContainerHandle> getMixinContainers() {
		return Collections.singletonList(containerHandleVirtual);
	}

	@Override
	public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
		return MixinEnvironment.CompatibilityLevel.JAVA_8;
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		return this.getClassLoader().getResourceAsStream(name);
	}

	@Override
	public URL[] getClassPath() {
		return null;
	}

	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
		return this.getClassLoader().loadClass(name);
	}

	@Override
	public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
		return Class.forName(name, initialize, this.getClassLoader());
	}

	@Override
	public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
		return Class.forName(name, initialize, this.getClassLoader());
	}

	@Override
	public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
		return this.getClassNode(name, false, 0);
	}

	@Override
	public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
		return this.getClassNode(name, runTransformers, 0);
	}

	@Override
	public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags) throws ClassNotFoundException, IOException {
		URL url = this.getClassLoader().getResource(name.replace('.', '/') + ".class");
		if (url == null) {
			throw new ClassNotFoundException(name);
		}
		URLConnection urlConnection = url.openConnection();
		ClassNode classNode = new ClassNode();
		try (InputStream is = urlConnection.getInputStream()) {
			if (is == null) {
				System.err.println("Failed to read URL for class \"" + name + "\" for mixin");
				throw new ClassNotFoundException(name);
			}
			new ClassReader(is).accept(classNode, readerFlags);
		}
		return classNode;
	}

	@Override
	public Collection<ITransformer> getTransformers() {
		return null;
	}

	@Override
	public Collection<ITransformer> getDelegatedTransformers() {
		return null;
	}

	@Override
	public void addTransformerExclusion(String name) {

		// Exclusions are not implemented in this context
	}

	@Override
	public void registerInvalidClass(String className) {
		// Invalid classes are not implemented in this context
	}

	@Override
	public boolean isClassLoaded(String className) {
		// Class load status is not implemented in this context
		return false;
	}

	@Override
	public String getClassRestrictions(String className) {
		return className.startsWith("com.hypixel.hytale.") ? "" : "PACKAGE_TRANSFORMER_EXCLUSION";
	}

	@Override
	public MixinEnvironment.Phase getInitialPhase() {
		return MixinEnvironment.Phase.PREINIT;
	}

	@Override
	public void offer(IMixinInternal internal) {
		super.offer(internal);
		if (internal instanceof IMixinTransformerFactory mixinTransformerFactory) {
			MixinEnvironment.getCurrentEnvironment().setActiveTransformer(
					mixinTransformerFactory.createTransformer());
		}
	}

	@Override
	protected ILogger createLogger(final String name) {
		return new LoggerAdapterAbstract(name) {
			@Override
			public String getType() {
				return "Hytale";
			}

			@Override
			public void catching(Level level, Throwable t) {

			}

			@Override
			public void log(Level level, String message, Object... params) {

			}

			@Override
			public void log(Level level, String message, Throwable t) {

			}

			@Override
			public <T extends Throwable> T throwing(T t) {
				return null;
			}
		};
	}

	public void onStartup() {
		this.phaseConsumer.accept(MixinEnvironment.Phase.DEFAULT);
	}
}
