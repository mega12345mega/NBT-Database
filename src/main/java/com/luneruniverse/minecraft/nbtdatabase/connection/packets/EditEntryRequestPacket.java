package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class EditEntryRequestPacket extends Packet {
	
	private final long id;
	private final Optional<String> name;
	private final Optional<byte[]> nbt;
	private final Optional<Integer> dataVersion;
	private final Optional<UUID> authorUuid;
	private final Optional<String> authorUsername;
	private final Optional<Boolean> verified;
	
	public EditEntryRequestPacket(long id, Optional<String> name, Optional<byte[]> nbt, Optional<Integer> dataVersion,
			Optional<UUID> authorUuid, Optional<String> authorUsername, Optional<Boolean> verified) {
		this.id = id;
		this.name = name;
		this.nbt = nbt;
		this.dataVersion = dataVersion;
		this.authorUuid = authorUuid;
		this.authorUsername = authorUsername;
		this.verified = verified;
	}
	public EditEntryRequestPacket(DataInputStream in) throws IOException {
		this.id = in.readLong();
		this.name = in.readBoolean() ? Optional.of(in.readUTF()) : Optional.empty();
		this.nbt = in.readBoolean() ? Optional.of(new byte[in.readInt()]) : Optional.empty();
		if (nbt.isPresent())
			in.readFully(nbt.get());
		this.dataVersion = in.readBoolean() ? Optional.of(in.readInt()) : Optional.empty();
		this.authorUuid = in.readBoolean() ? Optional.of(new UUID(in.readLong(), in.readLong())) : Optional.empty();
		this.authorUsername = in.readBoolean() ? Optional.of(in.readUTF()) : Optional.empty();
		this.verified = in.readBoolean() ? Optional.of(in.readBoolean()) : Optional.empty();
	}
	
	public long getId() {
		return id;
	}
	public Optional<String> getName() {
		return name;
	}
	public Optional<byte[]> getNbt() {
		return nbt;
	}
	public Optional<Integer> getDataVersion() {
		return dataVersion;
	}
	public Optional<UUID> getAuthorUuid() {
		return authorUuid;
	}
	public Optional<String> getAuthorUsername() {
		return authorUsername;
	}
	public Optional<Boolean> isVerified() {
		return verified;
	}
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeLong(id);
		
		out.writeBoolean(name.isPresent());
		if (name.isPresent())
			out.writeUTF(name.get());
		
		out.writeBoolean(nbt.isPresent());
		if (nbt.isPresent()) {
			out.writeInt(nbt.get().length);
			out.write(nbt.get());
		}
		
		out.writeBoolean(dataVersion.isPresent());
		if (dataVersion.isPresent())
			out.writeInt(dataVersion.get());
		
		out.writeBoolean(authorUuid.isPresent());
		if (authorUuid.isPresent()) {
			out.writeLong(authorUuid.get().getMostSignificantBits());
			out.writeLong(authorUuid.get().getLeastSignificantBits());
		}
		
		out.writeBoolean(authorUsername.isPresent());
		if (authorUsername.isPresent())
			out.writeUTF(authorUsername.get());
		
		out.writeBoolean(verified.isPresent());
		if (verified.isPresent())
			out.writeBoolean(verified.get());
	}
	
}
