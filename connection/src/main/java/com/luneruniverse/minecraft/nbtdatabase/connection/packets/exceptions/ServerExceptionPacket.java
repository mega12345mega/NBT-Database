package com.luneruniverse.minecraft.nbtdatabase.connection.packets.exceptions;

import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.ServerException;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;

public abstract class ServerExceptionPacket extends Packet {
	public abstract ServerException getException();
}
