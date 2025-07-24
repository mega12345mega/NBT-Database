package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.util.UUID;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;
import com.luneruniverse.minecraft.nbtdatabase.Entry;

public class AddEntryRequestPacket extends Packet {
	
	private @NotNull String name;
	private @NotNull byte[] nbt;
	private @NotNull Entry.Type type;
	private int dataVersion;
	private @NotNull UUID authorUuid;
	private @NotNull String authorUsername;
	private boolean verified;
	
	public AddEntryRequestPacket(String name, byte[] nbt, Entry.Type type, int dataVersion, UUID authorUuid, String authorUsername, boolean verified) {
		this.name = name;
		this.nbt = nbt;
		this.type = type;
		this.dataVersion = dataVersion;
		this.authorUuid = authorUuid;
		this.authorUsername = authorUsername;
		this.verified = verified;
	}
	AddEntryRequestPacket() {
		// Deserialization
	}
	
	public String getName() {
		return name;
	}
	public byte[] getNbt() {
		return nbt;
	}
	public Entry.Type getType() {
		return type;
	}
	public int getDataVersion() {
		return dataVersion;
	}
	public UUID getAuthorUuid() {
		return authorUuid;
	}
	public String getAuthorUsername() {
		return authorUsername;
	}
	public boolean isVerified() {
		return verified;
	}
	
}
