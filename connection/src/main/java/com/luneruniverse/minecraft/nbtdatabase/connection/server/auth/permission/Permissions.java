package com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.permission;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Permissions {
	
	private static final Set<String> PERMISSIONS = new HashSet<>();
	private static String r(String permission) {
		PERMISSIONS.add(permission);
		return permission;
	}
	
	public static Set<String> getPermissions() {
		return Collections.unmodifiableSet(PERMISSIONS);
	}
	
	public static Set<String> getMatchedPermissionsOrRole(String groupOrRole) {
		Role role = Roles.getRole(groupOrRole);
		if (role != null)
			return role.getPermissions();
		return getMatchedPermissions(groupOrRole);
	}
	
	public static Set<String> getMatchedPermissions(String group) {
		return Collections.unmodifiableSet(PERMISSIONS.stream()
				.filter(permission -> matches(group, permission)).collect(Collectors.toSet()));
	}
	
	public static boolean matches(String group, String permission) {
		if (group.equals(permission))
			return true;
		
		if (!group.endsWith("/"))
			group += "/";
		
		return permission.startsWith(group);
	}
	
	public static final String CONNECT = r("/connect");
	
	public static final String CONFIG_LOCK = r("/config/lock");
	public static final String CONFIG_EDIT = r("/config/edit");
	public static final String CONFIG_LIST = r("/config/list");
	
	public static final String ENTRY_LOCK = r("/entry/lock");
	
	public static final String ENTRY_SELF_ADD = r("/entry/self/add");
	public static final String ENTRY_SELF_EDIT_NAME = r("/entry/self/edit/name");
	public static final String ENTRY_SELF_EDIT_NBT = r("/entry/self/edit/nbt");
	public static final String ENTRY_SELF_EDIT_TYPE = r("/entry/self/edit/type");
	public static final String ENTRY_SELF_EDIT_DATA_VERSION = r("/entry/self/edit/data_version");
	public static final String ENTRY_SELF_REMOVE = r("/entry/self/remove");
	public static final String ENTRY_SELF_GET = r("/entry/self/get");
	public static final String ENTRY_SELF_EXPORT = r("/entry/self/export");
	public static final String ENTRY_SELF_LIST = r("/entry/self/list");
	public static final String ENTRY_ANYONE_ADD = r("/entry/anyone/add");
	public static final String ENTRY_ANYONE_EDIT_NAME = r("/entry/anyone/edit/name");
	public static final String ENTRY_ANYONE_EDIT_NBT = r("/entry/anyone/edit/nbt");
	public static final String ENTRY_ANYONE_EDIT_TYPE = r("/entry/anyone/edit/type");
	public static final String ENTRY_ANYONE_EDIT_DATA_VERSION = r("/entry/anyone/edit/data_version");
	public static final String ENTRY_ANYONE_REMOVE = r("/entry/anyone/remove");
	public static final String ENTRY_ANYONE_GET = r("/entry/anyone/get");
	public static final String ENTRY_ANYONE_EXPORT = r("/entry/anyone/export");
	public static final String ENTRY_ANYONE_LIST = r("/entry/anyone/list");
	
	public static final String ENTRY_AUTHOR_SELF_UUID = r("/entry/author/self/uuid");
	public static final String ENTRY_AUTHOR_SELF_USERNAME = r("/entry/author/self/username");
	public static final String ENTRY_AUTHOR_ANYONE_UUID = r("/entry/author/anyone/uuid");
	public static final String ENTRY_AUTHOR_ANYONE_USERNAME = r("/entry/author/anyone/username");
	
	public static final String ENTRY_VERIFIED_SELF_ADD_VERIFY = r("/entry/verified/self/add/verify");
	public static final String ENTRY_VERIFIED_SELF_ADD_UNVERIFY = r("/entry/verified/self/add/unverify");
	public static final String ENTRY_VERIFIED_SELF_EDIT_VERIFY = r("/entry/verified/self/edit/verify");
	public static final String ENTRY_VERIFIED_SELF_EDIT_UNVERIFY = r("/entry/verified/self/edit/unverify");
	public static final String ENTRY_VERIFIED_ANYONE_ADD_VERIFY = r("/entry/verified/anyone/add/verify");
	public static final String ENTRY_VERIFIED_ANYONE_ADD_UNVERIFY = r("/entry/verified/anyone/add/unverify");
	public static final String ENTRY_VERIFIED_ANYONE_EDIT_VERIFY = r("/entry/verified/anyone/edit/verify");
	public static final String ENTRY_VERIFIED_ANYONE_EDIT_UNVERIFY = r("/entry/verified/anyone/edit/unverify");
	
	public static final String TAG_LOCK = r("/tag/lock");
	public static final String TAG_ADD = r("/tag/add");
	public static final String TAG_EDIT_NAME = r("/tag/edit/name");
	public static final String TAG_EDIT_COLOR = r("/tag/edit/color");
	public static final String TAG_REMOVE = r("/tag/remove");
	public static final String TAG_GET = r("/tag/get");
	public static final String TAG_LIST = r("/tag/list");
	public static final String TAG_SELF_ATTACH = r("/tag/self/attach");
	public static final String TAG_SELF_DETACH = r("/tag/self/detach");
	public static final String TAG_SELF_FILTER = r("/tag/self/filter");
	public static final String TAG_ANYONE_ATTACH = r("/tag/anyone/attach");
	public static final String TAG_ANYONE_DETACH = r("/tag/anyone/detach");
	public static final String TAG_ANYONE_FILTER = r("/tag/anyone/filter");
	
}
