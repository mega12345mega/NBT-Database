package com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.permission;

import com.luneruniverse.minecraft.nbtdatabase.connection.user.User;

public interface PermissionManager {
	public boolean hasPermission(User user, String permission);
}
