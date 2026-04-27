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
package com.fox2code.hypertale.launcher;

import com.fox2code.hypertale.utils.HypertalePlatform;
import com.fox2code.hypertale.utils.SourceUtil;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

final class InternalInstallerGUI {
	static boolean tryRunInstaller() {
		System.clearProperty("java.awt.headless");
		if (GraphicsEnvironment.isHeadless()) {
			return false;
		}
		new InternalInstallerGUI();
		return true;
	}

	private final JFrame frame;
	private final JButton installButton;
	private final JProgressBar progressBar;

	private InternalInstallerGUI() {
		Color transparent = new Color(0, 0, 0, 0);
		JFrame jFrame = new JFrame("Hypertale " + BuildConfig.HYPERTALE_VERSION + " Installer");
		jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jFrame.setLayout(new BorderLayout());
		jFrame.setLocationRelativeTo(null);
		InternalInstallerGUIPanel rootPanel = new InternalInstallerGUIPanel();
		Dimension rootDimension = new Dimension(640, 320);
		rootPanel.setMinimumSize(rootDimension);
		rootPanel.setPreferredSize(rootDimension);
		rootPanel.setSize(rootDimension);
		rootPanel.setLayout(new BorderLayout());
		jFrame.add(rootPanel, BorderLayout.CENTER);
		
		Dimension elementsDimension = new Dimension(640, 20);
		JButton button = new JButton("Install");
		
		button.addActionListener(_ -> this.installHypertale());
		button.setContentAreaFilled(false);
		button.setBackground(transparent);
		button.setForeground(Color.WHITE);
		button.setBorderPainted(false);
		button.setFocusPainted(false);
		button.setLocation(0, 300);
		button.setPreferredSize(elementsDimension);
		button.setSize(elementsDimension);
		rootPanel.add(button, BorderLayout.SOUTH);
		JProgressBar progressBar = new JProgressBar();
		progressBar.setPreferredSize(elementsDimension);
		progressBar.setSize(elementsDimension);
		progressBar.setStringPainted(true);
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		progressBar.setValue(0);
		progressBar.setVisible(false);
		progressBar.setString("Initializing...");
		progressBar.setIndeterminate(true);
		progressBar.setBorderPainted(false);
		progressBar.setFocusable(false);
		progressBar.setOpaque(false);
		progressBar.setForeground(Color.BLACK);
		rootPanel.add(progressBar, BorderLayout.SOUTH);
		jFrame.pack();
		jFrame.setVisible(true);
		jFrame.setResizable(false);
		this.frame = jFrame;
		this.installButton = button;
		this.progressBar = progressBar;
	}

	private void installHypertale() {
		if (this.frame == null || this.installButton == null || this.progressBar == null) {
			System.exit(-1);
			return;
		}
		this.installButton.setVisible(false);
		this.progressBar.setVisible(true);
		this.progressBar.setValue(0);
		this.progressBar.setString("Verifying jar integrity...");
		this.progressBar.setIndeterminate(false);
		if (InternalInstallerGUI.class.getResource("/manifest.json") == null ||
			InternalInstallerGUI.class.getResource("/init-" + BuildConfig.HYPERTALE_VERSION + ".jar") == null) {
			JOptionPane.showMessageDialog(this.frame, "Failed to check jar integrity! Cancelling installation!");
			System.exit(1);
			return;
		}
		this.progressBar.setValue(10);
		this.progressBar.setString("Verifying Hytale installation...");
		File hytale = HypertalePlatform.getPlatform().getHytaleHome();
		if (!hytale.isDirectory()) {
			JOptionPane.showMessageDialog(this.frame, "Hytale installation directory not found! Cancelling installation!");
			System.exit(1);
			return;
		}
		File mods = new File(hytale, "UserData" + File.separator + "Mods");
		File earlyPlugins = new File(hytale, "UserData" + File.separator + "EarlyPlugins");
		if (!mods.isDirectory() || !earlyPlugins.isDirectory()) {
			JOptionPane.showMessageDialog(this.frame, "Incomplete Hytale installation! Cancelling installation!");
			System.exit(1);
			return;
		}
		this.progressBar.setValue(20);
		this.progressBar.setString("Cleaning up previous Hytale installations...");
		for (File file : Objects.requireNonNull(earlyPlugins.listFiles())) {
			if (shouldCleanup(file)) {
				if (!file.delete()) {
					file.deleteOnExit(); // Just in case it works and magically solves the issue.
					JOptionPane.showMessageDialog(this.frame,
							"Failed to delete previous Hypertale version! Cancelling installation!");
					System.exit(1);
					return;
				}
			}
		}
		for (File file : Objects.requireNonNull(mods.listFiles())) {
			if (shouldCleanup(file)) {
				if (!file.delete()) {
					file.deleteOnExit(); // Just in case it works and magically solves the issue.
					JOptionPane.showMessageDialog(this.frame,
							"Failed to delete previous Hypertale version! Cancelling installation!");
					System.exit(1);
					return;
				}
			}
		}
		this.progressBar.setValue(30);
		this.progressBar.setString("Installing HypertaleInit...");
		File init = new File(earlyPlugins, "HypertaleInit.jar");
		try (InputStream inputStream = new BufferedInputStream(Objects.requireNonNull(
				InternalInstallerGUI.class.getResourceAsStream(
						"/init-" + BuildConfig.HYPERTALE_VERSION + ".jar")))) {
			Files.copy(inputStream, init.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(this.frame,
					"Failed to install HypertaleInit.jar! Cancelling installation!");
			System.exit(1);
			return;
		}
		this.progressBar.setValue(50);
		this.progressBar.setString("Installing Hypertale...");
		File hypertale = new File(mods, "Hypertale-" + BuildConfig.HYPERTALE_VERSION + ".jar");
		try (InputStream inputStream = new BufferedInputStream(new FileInputStream(
				SourceUtil.getSourceFile(InternalInstallerGUI.class)))) {
			Files.copy(inputStream, hypertale.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(this.frame,
					"Failed to install HypertaleInit.jar! Cancelling installation!");
			System.exit(1);
			return;
		}
		this.progressBar.setValue(100);
		this.progressBar.setString("Done!");
		JOptionPane.showMessageDialog(this.frame, "Hypertale " +
				BuildConfig.HYPERTALE_VERSION + " successfully installed!");
		this.frame.setVisible(false);
		System.exit(0);
	}

	private static boolean shouldCleanup(File file) {
		return file.isFile() && (file.getName().equals("HypertaleInit.jar") ||
				(file.getName().startsWith("Hypertale-") && file.getName().endsWith(".jar")));
	}
}
