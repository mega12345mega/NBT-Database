package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class GetEntriesRequestPacket extends Packet {
	
	public GetEntriesRequestPacket() {}
	public GetEntriesRequestPacket(DataInputStream in) {}
	
	@Override
	public void write(DataOutputStream out) throws IOException {}
	
}
