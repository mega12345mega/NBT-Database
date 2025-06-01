package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class GetEntriesByTagRequestPacket extends Packet {
	
	private final String tag;
	
	public GetEntriesByTagRequestPacket(String tag) {
		this.tag = tag;
	}
	public GetEntriesByTagRequestPacket(DataInputStream in) throws IOException {
		this.tag = in.readUTF();
	}
	
	public String getTag() {
		return tag;
	}
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(tag);
	}
	
}
