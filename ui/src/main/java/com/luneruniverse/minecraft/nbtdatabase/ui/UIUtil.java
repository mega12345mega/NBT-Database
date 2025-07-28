package com.luneruniverse.minecraft.nbtdatabase.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class UIUtil {
	
	public static <T> Optional<T> edit(T originalValue, T newValue) {
		if (newValue == null || originalValue.equals(newValue))
			return Optional.empty();
		return Optional.of(newValue);
	}
	
	public static void setJFrameLogo(JFrame frame) {
		try (InputStream in = LoginUtil.class.getClassLoader().getResourceAsStream("logo_transparent.png")) {
			frame.setIconImage(ImageIO.read(in));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
