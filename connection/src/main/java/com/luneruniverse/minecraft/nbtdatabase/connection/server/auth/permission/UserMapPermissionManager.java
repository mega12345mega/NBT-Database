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

import com.luneruniverse.minecraft.nbtdatabase.connection.user.LoggedInUser;
import com.luneruniverse.minecraft.nbtdatabase.connection.user.User;

public class UserMapPermissionManager implements PermissionManager {
	
	public static UserMapPermissionManager fromInputStream(InputStream in) throws IOException {
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
				throw new IOException("Expected: (anyone|guest|<uuid>) (<permission>|<role>) (false|true)");
			
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
			
			String permOrRole = parts[1];
			
			boolean value = parts[2].equals("true");
			if (!value && !parts[2].equals("false"))
				throw new IOException("Not 'false' or 'true': " + parts[2]);
			
			if (anyone) {
				for (String matchedPermission : Permissions.getMatchedPermissionsOrRole(permOrRole)) {
					if (value)
						globalPermissions.add(matchedPermission);
					else
						globalPermissions.remove(matchedPermission);
					
					userPermissions.values().forEach(permMap -> permMap.remove(matchedPermission));
				}
			} else {
				Map<String, Boolean> permMap = userPermissions.computeIfAbsent(uuid, key -> new HashMap<>());
				for (String matchedPermission : Permissions.getMatchedPermissionsOrRole(permOrRole))
					permMap.put(matchedPermission, value);
			}
		}
		
		return new UserMapPermissionManager(globalPermissions, userPermissions);
	}
	public static UserMapPermissionManager fromString(String string) throws IOException {
		return fromInputStream(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));
	}
	public static UserMapPermissionManager fromFile(File file) throws IOException {
		try (FileInputStream in = new FileInputStream(file)) {
			return fromInputStream(in);
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
