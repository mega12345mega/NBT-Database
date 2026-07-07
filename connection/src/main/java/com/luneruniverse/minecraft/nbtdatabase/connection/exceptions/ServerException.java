package com.luneruniverse.minecraft.nbtdatabase.connection.exceptions;

import com.luneruniverse.minecraft.nbtdatabase.connection.packets.exceptions.ServerExceptionPacket;
import com.luneruniverse.minecraft.nbtdatabase.request.IllegalRequestException;

@SuppressWarnings("serial")
public abstract class ServerException extends RequestFailedException {
	
	public static ServerException from(Throwable throwable, boolean printInternalExceptions) {
		if (throwable instanceof ServerException)
			return (ServerException) throwable;
		
		if (throwable instanceof IllegalRequestException)
			return new IllegalRequestServerException((IllegalRequestException) throwable);
		
		if (printInternalExceptions)
			throwable.printStackTrace();
		return new InternalServerException();
	}
	
	public ServerException(String message) {
		super(message);
	}
	
	public abstract ServerExceptionPacket toPacket();
	
}
