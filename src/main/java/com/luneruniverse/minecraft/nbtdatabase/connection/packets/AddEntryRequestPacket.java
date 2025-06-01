package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class AddEntryRequestPacket extends Packet {
	
	private final String name;
	private final byte[] nbt;
	private final int dataVersion;
	private final UUID authorUuid;
	private final String authorUsername;
	private final boolean verified;
	
	public AddEntryRequestPacket(String name, byte[] nbt, int dataVersion, UUID authorUuid, String authorUsername, boolean verified) {
		this.name = name;
		this.nbt = nbt;
		this.dataVersion = dataVersion;
		this.authorUuid = authorUuid;
		this.authorUsername = authorUsername;
		this.verified = verified;
	}
	public AddEntryRequestPacket(DataInputStream in) throws IOException {
		this.name = in.readUTF();
		this.nbt = new byte[in.readInt()];
		in.readFully(nbt);
		this.dataVersion = in.readInt();
		this.authorUuid = new UUID(in.readLong(), in.readLong());
		this.authorUsername = in.readUTF();
		this.verified = in.readBoolean();
	}
	
	public String getName() {
		return name;
	}
	public byte[] getNbt() {
		return nbt;
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
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(name);
		out.writeInt(nbt.length);
		out.write(nbt);
		out.writeInt(dataVersion);
		out.writeLong(authorUuid.getMostSignificantBits());
		out.writeLong(authorUuid.getLeastSignificantBits());
		out.writeUTF(authorUsername);
		out.writeBoolean(verified);
	}
	
}
