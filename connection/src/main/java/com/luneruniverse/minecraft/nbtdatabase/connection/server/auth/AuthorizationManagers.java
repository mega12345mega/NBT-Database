package com.luneruniverse.minecraft.nbtdatabase.connection.server.auth;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.permission.PermissionAuthorizationManager;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.ConfigurateUtil;

public class AuthorizationManagers {
	
	public static interface AuthorizationManagerDeserializer {
		public AuthorizationManager deserialize(File serverRoot, ConfigurationNode node) throws SerializationException;
	}
	
	private static final Map<String, AuthorizationManagerDeserializer> MANAGERS = new HashMap<>();
	private static AuthorizationManagerDeserializer r(String name, AuthorizationManagerDeserializer manager) {
		MANAGERS.put(name, manager);
		return manager;
	}
	
	public static AuthorizationManager deserialize(File serverRoot, ConfigurationNode node) throws SerializationException {
		String name = ConfigurateUtil.requireStringFromList(node.node("manager"), MANAGERS.keySet());
		return MANAGERS.get(name).deserialize(serverRoot, node.node(name));
	}
	
	public static final AuthorizationManagerDeserializer ALLOW = r("allow", (serverRoot, node) -> AllowAuthorizationManager.create());
	public static final AuthorizationManagerDeserializer PERMISSION = r("permission", PermissionAuthorizationManager::deserialize);
	
}
