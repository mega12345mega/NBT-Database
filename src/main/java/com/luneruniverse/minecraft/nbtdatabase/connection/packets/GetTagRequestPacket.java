package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class GetTagRequestPacket extends Packet {
	
	private final String name;
	
	public GetTagRequestPacket(String name) {
		this.name = name;
	}
	public GetTagRequestPacket(DataInputStream in) throws IOException {
		this.name = in.readUTF();
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(name);
	}
	
}
