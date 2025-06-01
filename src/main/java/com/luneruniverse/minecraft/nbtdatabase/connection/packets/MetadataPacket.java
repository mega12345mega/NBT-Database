package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.luneruniverse.minecraft.nbtdatabase.connection.NBTDatabaseMetadata;
import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class MetadataPacket extends Packet {
	
	private final NBTDatabaseMetadata metadata;
	
	public MetadataPacket(NBTDatabaseMetadata metadata) {
		this.metadata = metadata;
	}
	public MetadataPacket(DataInputStream in) throws IOException {
		this.metadata = new NBTDatabaseMetadata(in.readInt(), in.readInt());
	}
	
	public NBTDatabaseMetadata getMetadata() {
		return metadata;
	}
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeInt(metadata.getMaxNbtSize());
		out.writeInt(metadata.getMaxNumResults());
	}
	
}
