package com.luneruniverse.minecraft.nbtdatabase.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
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
		try (InputStream in = UIUtil.class.getClassLoader().getResourceAsStream("ui/logo_transparent.png")) {
			frame.setIconImage(ImageIO.read(in));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String filterInvalidFileNameChars(String name) throws InvalidPathException {
		StringBuilder builder = new StringBuilder(name);
		for (int i = 0; i < builder.length(); i++) {
			char c = builder.charAt(i);
			if (c == '/' || c == '\\' || c == '\0')
				builder.setCharAt(i, '_');
		}
		
		FileSystem system = FileSystems.getDefault();
		while (true) {
			name = builder.toString();
			try {
				system.getPath(name);
				return name;
			} catch (InvalidPathException e) {
				if (e.getIndex() == -1)
					throw e;
				builder.setCharAt(e.getIndex(), '_');
			}
		}
	}
	
}
