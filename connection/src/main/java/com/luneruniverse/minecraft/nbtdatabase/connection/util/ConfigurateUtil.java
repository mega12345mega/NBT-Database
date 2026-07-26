package com.luneruniverse.minecraft.nbtdatabase.connection.util;

import java.io.File;
import java.util.Collection;
import java.util.function.Predicate;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class ConfigurateUtil {
	
	public static <T> T require(ConfigurationNode node, Class<T> clazz, Predicate<T> requirement, String expected) throws SerializationException {
		T value = node.get(clazz);
		if (value == null || !requirement.test(value))
			throw new SerializationException(node, clazz, expected);
		return value;
	}
	
	public static <T> T require(ConfigurationNode node, Class<T> clazz, String expected) throws SerializationException {
		return require(node, clazz, value -> true, expected);
	}
	
	public static boolean requireBoolean(ConfigurationNode node) throws SerializationException {
		return require(node, Boolean.class, value -> true, "Expected 'false' or 'true'");
	}
	
	public static String requireStringFromList(ConfigurationNode node, Collection<String> options) throws SerializationException {
		if (options.isEmpty())
			throw new IllegalArgumentException("options cannot be empty");
		
		StringBuilder expected = new StringBuilder("Expected ");
		int i = 0;
		for (String option : options) {
			if (i > 0) {
				if (options.size() == 2)
					expected.append(" or ");
				else if (i == options.size() - 1)
					expected.append(", or ");
				else
					expected.append(", ");
			}
			
			expected.append('\'');
			expected.append(option);
			expected.append('\'');
			
			i++;
		}
		
		return require(node, String.class, options::contains, expected.toString());
	}
	
	public static File requireExistingFile(File parent, ConfigurationNode node) throws SerializationException {
		File file = new File(parent, require(node, String.class, "Expected file path"));
		if (!file.exists())
			throw new SerializationException(node, String.class, "File doesn't exist");
		return file;
	}
	
}
