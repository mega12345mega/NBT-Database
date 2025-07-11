package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class EntryNBTPacket extends Packet {
	
	private final byte[] nbt;
	
	public EntryNBTPacket(byte[] nbt) {
		this.nbt = nbt;
	}
	public EntryNBTPacket(DataInputStream in) throws IOException {
		this.nbt = new byte[in.readInt()];
		in.readFully(nbt);
	}
	
	public byte[] getNBT() {
		return nbt;
	}
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeInt(nbt.length);
		out.write(nbt);
	}
	
}
