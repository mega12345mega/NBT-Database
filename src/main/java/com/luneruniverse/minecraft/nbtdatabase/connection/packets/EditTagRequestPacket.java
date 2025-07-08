package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class EditTagRequestPacket extends Packet {
	
	private final String currentName;
	private final Optional<String> name;
	private final Optional<Integer> color;
	
	public EditTagRequestPacket(String currentName, Optional<String> name, Optional<Integer> color) {
		this.currentName = currentName;
		this.name = name;
		this.color = color;
	}
	public EditTagRequestPacket(DataInputStream in) throws IOException {
		this.currentName = in.readUTF();
		this.name = in.readBoolean() ? Optional.of(in.readUTF()) : Optional.empty();
		this.color = in.readBoolean() ? Optional.of(in.readInt()) : Optional.empty();
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
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(currentName);
		
		out.writeBoolean(name.isPresent());
		if (name.isPresent())
			out.writeUTF(name.get());
		
		out.writeBoolean(color.isPresent());
		if (color.isPresent())
			out.writeInt(color.get());
	}
	
}
