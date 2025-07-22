package com.luneruniverse.minecraft.nbtdatabase.connection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.luneruniverse.minecraft.nbtdatabase.Config;
import com.luneruniverse.minecraft.nbtdatabase.EntryFilter;
import com.luneruniverse.minecraft.nbtdatabase.EntryView;
import com.luneruniverse.minecraft.nbtdatabase.NBTDatabase;
import com.luneruniverse.minecraft.nbtdatabase.NBTEntry;
import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.TagFilter;
import com.luneruniverse.minecraft.nbtdatabase.Util;

public class LocalNBTDatabaseAccess implements NBTDatabaseAccess {
	
	private final NBTDatabase database;
	private final ExecutorService executor;
	
	public LocalNBTDatabaseAccess(NBTDatabase database) {
		this.database = database;
		this.executor = Executors.newSingleThreadExecutor();
	}
	
	@Override
	public String getName() {
		return "[Local] " + database.getFile().getAbsolutePath();
	}
	
	@Override
	public CompletableFuture<Void> setConfig(Config config) {
		return Util.runAsync(() -> database.getConfigManager().setConfig(config), executor);
	}
	
	@Override
	public CompletableFuture<Config> getConfig() {
		return CompletableFuture.completedFuture(database.getConfigManager().getConfig());
	}
	
	@Override
	public CompletableFuture<Long> addEntry(String name, byte[] nbt, int dataVersion, UUID authorUuid, String authorUsername, boolean verified) {
		return Util.supplyAsync(() -> database.addEntry(name, nbt, dataVersion, authorUuid, authorUsername, verified), executor);
	}
	
	@Override
	public CompletableFuture<Void> editEntry(long id, Optional<String> name, Optional<byte[]> nbt, Optional<Integer> dataVersion,
			Optional<UUID> authorUuid, Optional<String> authorUsername, Optional<Boolean> verified) {
		return Util.runAsync(() -> database.editEntry(id, name, nbt, dataVersion, authorUuid, authorUsername, verified), executor);
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
	public CompletableFuture<byte[]> getEntryNBT(long id) {
		return Util.supplyAsync(() -> database.getEntryNBT(id), executor);
	}
	
	@Override
	public CompletableFuture<List<NBTEntry>> getEntries(EntryFilter filter, EntryView view) {
		return Util.supplyAsync(() -> database.getEntries(filter, view), executor);
	}
	
	@Override
	public CompletableFuture<Void> addTag(String name, int color) {
		return Util.runAsync(() -> database.addTag(name, color), executor);
	}
	
	@Override
	public CompletableFuture<Void> editTag(String currentName, Optional<String> name, Optional<Integer> color) {
		return Util.runAsync(() -> database.editTag(currentName, name, color), executor);
	}
	
	@Override
	public CompletableFuture<Void> removeTag(String name) {
		return Util.runAsync(() -> database.removeTag(name), executor);
	}
	
	@Override
	public CompletableFuture<Tag> getTag(String name) {
		return Util.supplyAsync(() -> database.getTag(name), executor);
	}
	
	@Override
	public CompletableFuture<List<Tag>> getTags(TagFilter filter) {
		return Util.supplyAsync(() -> database.getTags(filter), executor);
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
	public CompletableFuture<Void> closeAsync() {
		return Util.shutdown(executor);
	}
	
	@Override
	public void close() throws InterruptedException {
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
	}
	
}
