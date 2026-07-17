package com.luneruniverse.minecraft.nbtdatabase.connection.user;

import java.util.UUID;

public class GuestUser extends User {
	
	public GuestUser(ClientType clientType, String ip) {
		super(clientType, ip);
	}
	
	@Override
	public boolean isLoggedIn() {
		return false;
	}
	
	@Override
	public boolean hasUuid(UUID uuid) {
		return false;
	}
	
	@Override
	public String toString() {
		return "<guest>";
	}
	
}
