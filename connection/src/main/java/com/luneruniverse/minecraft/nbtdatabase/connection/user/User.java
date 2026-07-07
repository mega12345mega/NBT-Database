package com.luneruniverse.minecraft.nbtdatabase.connection.user;

import java.util.Optional;

import com.luneruniverse.minecraft.nbtdatabase.connection.packets.login.LoginPacket;

import io.netty.channel.socket.SocketChannel;

public abstract class User {
	
	public static User fromConnect(SocketChannel channel, Optional<LoginPacket.User> user, ClientType clientType) {
		String ip = channel.remoteAddress().getAddress().getHostAddress();
		if (user.isPresent())
			return new LoggedInUser(clientType, ip, user.get().getUuid(), user.get().getUsername());
		return new GuestUser(clientType, ip);
	}
	
	private final ClientType clientType;
	private final String ip;
	
	public User(ClientType clientType, String ip) {
		this.clientType = clientType;
		this.ip = ip;
	}
	
	public ClientType getClientType() {
		return clientType;
	}
	
	public String getIp() {
		return ip;
	}
	
	public abstract boolean isLoggedIn();
	public abstract Optional<LoggedInUser> asLoggedInUser();
	@Override
	public abstract String toString();
	
}
