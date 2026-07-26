package com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.permission;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import com.luneruniverse.minecraft.nbtdatabase.connection.util.ConfigurateUtil;

public class PermissionManagers {
	
	public static interface PermissionManagerDeserializer {
		public PermissionManager deserialize(File parent, ConfigurationNode node) throws SerializationException;
	}
	
	private static final Map<String, PermissionManagerDeserializer> MANAGERS = new HashMap<>();
	private static PermissionManagerDeserializer r(String name, PermissionManagerDeserializer manager) {
		MANAGERS.put(name, manager);
		return manager;
	}
	
	public static PermissionManager deserialize(File parent, ConfigurationNode node) throws SerializationException {
		String name = ConfigurateUtil.requireStringFromList(node.node("manager"), MANAGERS.keySet());
		return MANAGERS.get(name).deserialize(parent, node.node(name));
	}
	
	public static final PermissionManagerDeserializer GLOBAL = r("global", GlobalPermissionManager::deserialize);
	public static final PermissionManagerDeserializer USER_MAP = r("user_map", UserMapPermissionManager::deserialize);
	
}
