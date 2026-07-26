package com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.permission;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Role {
	
	private final String name;
	private final Set<String> permissions;
	
	public Role(String name, String... matchers) throws NoPermissionMatchedException {
		this.name = name;
		this.permissions = Collections.unmodifiableSet(Stream.of(matchers)
				.flatMap(matcher -> Permissions.getMatched(matcher).stream()).collect(Collectors.toSet()));
	}
	
	public String getName() {
		return name;
	}
	
	public Set<String> getPermissions() {
		return permissions;
	}
	
}
