package com.luneruniverse.minecraft.nbtdatabase.connection.util;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ParsingException;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

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
	
	public static void mergeOverriding(ConfigurationNode base, ConfigurationNode modifiers) {
		if (!base.isMap() || !modifiers.isMap()) {
			if (!modifiers.isNull())
				base.from(modifiers);
			return;
		}
		
		for (Map.Entry<Object, ? extends ConfigurationNode> modifier : modifiers.childrenMap().entrySet())
			mergeOverriding(base.node(modifier.getKey()), modifier.getValue());
	}
	
	public static ConfigurationNode parseYamlFile(File file) throws ConfigurateException {
		return YamlConfigurationLoader.builder().file(file).build().load();
	}
	
	public static ConfigurationNode parseYamlString(String str) throws ConfigurateException {
		return YamlConfigurationLoader.builder().buildAndLoadString(str);
	}
	
	public static ConfigurationNode parseInlineYamlString(String str) throws ConfigurateException {
		ConfigurationNode node = YamlConfigurationLoader.builder().build().createNode();
		
		int charIndex = 0;
		int keyIndex = 0;
		StringBuilder keyBuilder = new StringBuilder();
		StringBuilder valueBuilder = new StringBuilder();
		StringBuilder currentBuilder = keyBuilder;
		boolean escaped = false;
		
		for (char c : str.toCharArray()) {
			charIndex++;
			if (escaped) {
				currentBuilder.append(c);
			} else if (c == '\\') {
				escaped = true;
			} else if (c == ':') {
				if (currentBuilder == valueBuilder)
					throw new ParsingException(1, charIndex, str, "Unexpected ':' in value for key '" + keyBuilder + "'", null);
				currentBuilder = valueBuilder;
			} else if (c == ';') {
				if (currentBuilder == keyBuilder)
					throw new ParsingException(1, charIndex, str, "Unexpected ';' in key '" + keyBuilder + "'", null);
				addKeyValueYamlPair(node, keyBuilder.toString(), valueBuilder.toString(), keyIndex, str);
				keyIndex = charIndex + 1;
				keyBuilder.setLength(0);
				valueBuilder.setLength(0);
				currentBuilder = keyBuilder;
			} else {
				currentBuilder.append(c);
			}
		}
		
		if (escaped)
			throw new ParsingException(1, charIndex, str, "Incomplete '\\'", null);
		if (currentBuilder == keyBuilder && keyBuilder.length() > 0)
			throw new ParsingException(1, charIndex, str, "Incomplete key '" + keyBuilder + "'", null);
		if (currentBuilder == valueBuilder)
			addKeyValueYamlPair(node, keyBuilder.toString(), valueBuilder.toString(), keyIndex, str);
		
		return node;
	}
	private static void addKeyValueYamlPair(ConfigurationNode node, String key, String value, int keyIndex, String str) throws ConfigurateException {
		StringBuilder keyParts = new StringBuilder();
		
		for (String keyPart : key.split("\\.")) {
			node = node.node(keyPart);
			
			if (keyParts.length() > 0)
				keyParts.append('.');
			keyParts.append(keyPart);
			
			if (!node.virtual() && !node.isMap())
				throw new ParsingException(1, keyIndex, str, "Duplicate key '" + keyParts + "'", null);
		}
		
		if (!node.virtual())
			throw new ParsingException(1, keyIndex, str, "Duplicate key '" + key + "'", null);
		
		node.from(parseYamlString(value));
	}
	
}
