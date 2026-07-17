package com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.permission;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Roles {
	
	private static final Map<String, Role> ROLES = new HashMap<>();
	private static Role r(Role role) {
		ROLES.put(role.getName(), role);
		return role;
	}
	
	public static Map<String, Role> getRoles() {
		return Collections.unmodifiableMap(ROLES);
	}
	
	public static Role getRole(String name) {
		return ROLES.get(name);
	}
	
	public static final Role ADMIN = r(new Role("admin",
			"/"));
	
	public static final Role CONTRIBUTOR_ANYONE = r(new Role("contributor_anyone",
			"/connect",
			"/config/list",
			"/entry/anyone",
			"/entry/author",
			"/entry/verified/self/add/verify",
			"/entry/verified/self/edit/verify",
			"/entry/verified/anyone/add/unverify",
			"/entry/verified/anyone/edit/unverify",
			"/tag/get",
			"/tag/list",
			"/tag/anyone"));
	
	public static final Role CONTRIBUTOR_SELF = r(new Role("contributor_self",
			"/connect",
			"/config/list",
			"/entry/self",
			"/entry/author/self/username",
			"/entry/verified/self/add/verify",
			"/entry/verified/self/edit/verify",
			"/tag/get",
			"/tag/list",
			"/tag/self"));
	
	public static final Role VIEWER_ANYONE = r(new Role("viewer_anyone",
			"/connect",
			"/config/list",
			"/entry/anyone/get",
			"/entry/anyone/export",
			"/entry/anyone/list",
			"/tag/get",
			"/tag/list",
			"/tag/anyone/filter"));
	
	public static final Role VIEWER_SELF = r(new Role("viewer_self",
			"/connect",
			"/config/list",
			"/entry/self/get",
			"/entry/self/export",
			"/entry/self/list",
			"/tag/get",
			"/tag/list",
			"/tag/self/filter"));
	
}
