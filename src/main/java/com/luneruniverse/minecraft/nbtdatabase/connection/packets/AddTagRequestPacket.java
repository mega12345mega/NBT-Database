package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class AddTagRequestPacket extends Packet {
	
	private final String name;
	private final int color;
	
	public AddTagRequestPacket(String name, int color) {
		this.name = name;
		this.color = color;
	}
	public AddTagRequestPacket(DataInputStream in) throws IOException {
		this.name = in.readUTF();
		this.color = in.readInt();
	}
	
	public String getName() {
		return name;
	}
	public int getColor() {
		return color;
	}
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(name);
		out.writeInt(color);
	}
	
}
