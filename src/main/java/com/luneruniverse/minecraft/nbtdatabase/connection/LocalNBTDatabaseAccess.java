package com.luneruniverse.minecraft.nbtdatabase.connection;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.luneruniverse.minecraft.nbtdatabase.NBTDatabase;
import com.luneruniverse.minecraft.nbtdatabase.NBTEntry;
import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.Util;

public class LocalNBTDatabaseAccess implements NBTDatabaseAccess {
	
	private final NBTDatabase database;
	private final ExecutorService executor;
	
	public LocalNBTDatabaseAccess(NBTDatabase database) {
		this.database = database;
		this.executor = Executors.newSingleThreadExecutor();
	}
	
	@Override
	public CompletableFuture<NBTDatabaseMetadata> getMetadata() {
		return CompletableFuture.completedFuture(database.getMetadata());
	}
	
	@Override
	public CompletableFuture<Long> addEntry(String name, byte[] nbt, int dataVersion, UUID authorUuid, String authorUsername, boolean verified) {
		return Util.supplyAsync(() -> database.addEntry(name, nbt, dataVersion, authorUuid, authorUsername, verified), executor);
	}
	
	@Override
	public CompletableFuture<Void> removeEntry(long id) {
		return Util.runAsync(() -> database.removeEntry(id), executor);
	}
	
	@Override
	public CompletableFuture<NBTEntry> getEntry(long id) {
		return Util.supplyAsync(() -> database.getEntry(id), executor);
	}
	
	@Override
	public CompletableFuture<List<NBTEntry>> getEntries() {
		return Util.supplyAsync(() -> database.getEntries(), executor);
	}
	
	@Override
	public CompletableFuture<List<NBTEntry>> getEntriesByName(String query) {
		return Util.supplyAsync(() -> database.getEntriesByName(query), executor);
	}
	
	@Override
	public CompletableFuture<List<NBTEntry>> getEntriesByAuthorUUID(UUID query) {
		return Util.supplyAsync(() -> database.getEntriesByAuthorUUID(query), executor);
	}
	
	@Override
	public CompletableFuture<List<NBTEntry>> getEntriesByAuthorName(String query) {
		return Util.supplyAsync(() -> database.getEntriesByAuthorName(query), executor);
	}
	
	@Override
	public CompletableFuture<Void> addTag(String name, int color) {
		return Util.runAsync(() -> database.addTag(name, color), executor);
	}
	
	@Override
	public CompletableFuture<Void> removeTag(String name) {
		return Util.runAsync(() -> database.removeTag(name), executor);
	}
	
	@Override
	public CompletableFuture<List<Tag>> getTags() {
		return Util.supplyAsync(() -> database.getTags(), executor);
	}
	
	@Override
	public CompletableFuture<Void> addTagToEntry(long entry, String tag) {
		return Util.runAsync(() -> database.addTagToEntry(entry, tag), executor);
	}
	
	@Override
	public CompletableFuture<Void> removeTagFromEntry(long entry, String tag) {
		return Util.runAsync(() -> database.removeTagFromEntry(entry, tag), executor);
	}
	
	@Override
	public CompletableFuture<List<Tag>> getTagsByEntry(long entry) {
		return Util.supplyAsync(() -> database.getTagsByEntry(entry), executor);
	}
	
	@Override
	public CompletableFuture<List<NBTEntry>> getEntriesByTag(String tag) {
		return Util.supplyAsync(() -> database.getEntriesByTag(tag), executor);
	}
	
	@Override
	public CompletableFuture<Void> closeAsync() {
		return Util.shutdown(executor);
	}
	
	@Override
	public void close() throws InterruptedException {
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
	}
	
}
