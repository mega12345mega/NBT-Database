package com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries;

import java.util.Optional;
import java.util.UUID;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;
import com.luneruniverse.minecraft.nbtdatabase.Entry;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;

public class EditEntryRequestPacket extends Packet {
	
	private long id;
	private @NotNull Optional<String> name;
	private @NotNull Optional<byte[]> nbt;
	private @NotNull Optional<Entry.Type> type;
	private @NotNull Optional<Integer> dataVersion;
	private @NotNull Optional<UUID> authorUuid;
	private @NotNull Optional<String> authorUsername;
	private @NotNull Optional<Boolean> verified;
	
	public EditEntryRequestPacket(long id, Optional<String> name, Optional<byte[]> nbt, Optional<Entry.Type> type,
			Optional<Integer> dataVersion, Optional<UUID> authorUuid, Optional<String> authorUsername, Optional<Boolean> verified) {
		this.id = id;
		this.name = name;
		this.nbt = nbt;
		this.type = type;
		this.dataVersion = dataVersion;
		this.authorUuid = authorUuid;
		this.authorUsername = authorUsername;
		this.verified = verified;
	}
	EditEntryRequestPacket() {
		// Deserialization
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
	public Optional<Entry.Type> getType() {
		return type;
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
	
}
