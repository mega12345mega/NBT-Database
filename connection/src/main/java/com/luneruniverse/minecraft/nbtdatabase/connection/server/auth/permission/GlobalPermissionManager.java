package com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.permission;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import com.luneruniverse.minecraft.nbtdatabase.connection.user.User;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.ConfigurateUtil;

public class GlobalPermissionManager implements PermissionManager {
	
	public static GlobalPermissionManager fromMatchers(String... matchers) throws NoPermissionMatchedException {
		Set<String> permissions = new HashSet<>();
		
		for (String matcher : matchers)
			permissions.addAll(Permissions.getMatched(matcher));
		
		return new GlobalPermissionManager(permissions);
	}
	
	public static GlobalPermissionManager deserialize(File parent, ConfigurationNode node) throws SerializationException {
		try {
			return fromMatchers(ConfigurateUtil.require(node.node("permissions"), String[].class,
					"Expected list of permission matchers"));
		} catch (NoPermissionMatchedException e) {
			throw new SerializationException(node.node("permissions"), String[].class, e.getMessage());
		}
	}
	
	private final Set<String> permissions;
	
	public GlobalPermissionManager(Set<String> permissions) {
		this.permissions = permissions;
	}
	
	@Override
	public boolean hasPermission(User user, String permission) {
		return permissions.contains(permission);
	}
	
}
