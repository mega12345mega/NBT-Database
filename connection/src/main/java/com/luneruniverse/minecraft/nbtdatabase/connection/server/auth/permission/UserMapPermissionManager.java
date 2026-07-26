package com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.permission;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import com.luneruniverse.minecraft.nbtdatabase.connection.user.LoggedInUser;
import com.luneruniverse.minecraft.nbtdatabase.connection.user.User;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.ConfigurateUtil;

public class UserMapPermissionManager implements PermissionManager {
	
	public static UserMapPermissionManager fromInputStream(InputStream in) throws IOException, NoPermissionMatchedException {
		Set<String> globalPermissions = new HashSet<>();
		Map<UUID, Map<String, Boolean>> userPermissions = new HashMap<>();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty() || line.startsWith("#"))
				continue;
			
			String[] parts = line.split("\\s+");
			if (parts.length != 3)
				throw new IOException("Expected: (anyone|guest|<uuid>) (<permission matcher>) (false|true)");
			
			boolean anyone;
			UUID uuid;
			try {
				if (parts[0].equals("anyone")) {
					anyone = true;
					uuid = null;
				} else {
					anyone = false;
					if (parts[0].equals("guest"))
						uuid = null;
					else
						uuid = UUID.fromString(parts[0]);
				}
			} catch (IllegalArgumentException e) {
				throw new IOException("Not a uuid or 'guest' or 'anyone': " + parts[0]);
			}
			
			String matcher = parts[1];
			
			boolean value = parts[2].equals("true");
			if (!value && !parts[2].equals("false"))
				throw new IOException("Not 'false' or 'true': " + parts[2]);
			
			if (anyone) {
				for (String permission : Permissions.getMatched(matcher)) {
					if (value)
						globalPermissions.add(permission);
					else
						globalPermissions.remove(permission);
					
					userPermissions.values().forEach(permMap -> permMap.remove(permission));
				}
			} else {
				Map<String, Boolean> permMap = userPermissions.computeIfAbsent(uuid, key -> new HashMap<>());
				for (String permission : Permissions.getMatched(matcher))
					permMap.put(permission, value);
			}
		}
		
		return new UserMapPermissionManager(globalPermissions, userPermissions);
	}
	public static UserMapPermissionManager fromString(String string) throws IOException, NoPermissionMatchedException {
		return fromInputStream(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));
	}
	public static UserMapPermissionManager fromFile(File file) throws IOException, NoPermissionMatchedException {
		try (FileInputStream in = new FileInputStream(file)) {
			return fromInputStream(in);
		}
	}
	
	public static UserMapPermissionManager deserialize(File parent, ConfigurationNode node) throws SerializationException {
		File permissionsFile = ConfigurateUtil.requireExistingFile(parent, node.node("permissions_file"));
		
		try {
			return fromFile(permissionsFile);
		} catch (NoPermissionMatchedException e) {
			throw new SerializationException(node.node("permissions_file"), String.class, e.getMessage());
		} catch (IOException e) {
			throw new SerializationException(node.node("permissions_file"), String.class, "Failed to read file", e);
		}
	}
	
	private final Set<String> globalPermissions;
	private final Map<UUID, Map<String, Boolean>> userPermissions;
	
	public UserMapPermissionManager(Set<String> globalPermissions, Map<UUID, Map<String, Boolean>> userPermissions) {
		this.globalPermissions = globalPermissions;
		this.userPermissions = userPermissions;
	}
	
	@Override
	public boolean hasPermission(User user, String permission) {
		Map<String, Boolean> permMap = userPermissions.get(user.isLoggedIn() ? ((LoggedInUser) user).getUuid() : null);
		if (permMap != null && permMap.containsKey(permission))
			return permMap.get(permission);
		
		return globalPermissions.contains(permission);
	}
	
}
