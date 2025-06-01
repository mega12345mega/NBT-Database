package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class MetadataRequestPacket extends Packet {
	
	public MetadataRequestPacket() {}
	public MetadataRequestPacket(DataInputStream in) {}
	
	@Override
	public void write(DataOutputStream out) throws IOException {}
	
}
