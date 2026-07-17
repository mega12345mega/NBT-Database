package com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.permission;

import java.util.HashSet;
import java.util.Set;

import com.luneruniverse.minecraft.nbtdatabase.connection.user.User;

public class GlobalPermissionManager implements PermissionManager {
	
	public static GlobalPermissionManager fromPermissionsAndRoles(String... permsAndRoles) {
		Set<String> permissions = new HashSet<>();
		
		for (String permOrRole : permsAndRoles)
			permissions.addAll(Permissions.getMatchedPermissionsOrRole(permOrRole));
		
		return new GlobalPermissionManager(permissions);
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
