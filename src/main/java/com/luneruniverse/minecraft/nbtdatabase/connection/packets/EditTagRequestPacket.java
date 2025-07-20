package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.util.Optional;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;

public class EditTagRequestPacket extends Packet {
	
	private @NotNull String currentName;
	private @NotNull Optional<String> name;
	private @NotNull Optional<Integer> color;
	
	public EditTagRequestPacket(String currentName, Optional<String> name, Optional<Integer> color) {
		this.currentName = currentName;
		this.name = name;
		this.color = color;
	}
	EditTagRequestPacket() {
		// Deserialization
	}
	
	public String getCurrentName() {
		return currentName;
	}
	public Optional<String> getName() {
		return name;
	}
	public Optional<Integer> getColor() {
		return color;
	}
	
}
