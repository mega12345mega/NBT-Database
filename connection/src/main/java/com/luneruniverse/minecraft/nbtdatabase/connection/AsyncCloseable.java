package com.luneruniverse.minecraft.nbtdatabase.connection;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface AsyncCloseable extends AutoCloseable {
	public default boolean isOpen() {
		return !getCloseFuture().isDone();
	}
	
	public CompletableFuture<Void> getCloseFuture();
	public CompletableFuture<Void> closeAsync();
	
	@Override
	public void close() throws IOException, InterruptedException;
}
