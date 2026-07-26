package com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.permission;

@SuppressWarnings("serial")
public class NoPermissionMatchedException extends IllegalArgumentException {
	
	private final String matcher;
	
	public NoPermissionMatchedException(String matcher) {
		super("No permissions matched by: " + matcher);
		this.matcher = matcher;
	}
	
	public String getMatcher() {
		return matcher;
	}
	
}
