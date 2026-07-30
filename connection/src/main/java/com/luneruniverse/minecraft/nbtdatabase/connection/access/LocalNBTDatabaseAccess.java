package com.luneruniverse.minecraft.nbtdatabase.connection.access;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.luneruniverse.minecraft.nbtdatabase.Config;
import com.luneruniverse.minecraft.nbtdatabase.Entry;
import com.luneruniverse.minecraft.nbtdatabase.NBTDatabase;
import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.FutureUtil;
import com.luneruniverse.minecraft.nbtdatabase.request.EntryFilter;
import com.luneruniverse.minecraft.nbtdatabase.request.EntryView;
import com.luneruniverse.minecraft.nbtdatabase.request.TagFilter;

public class LocalNBTDatabaseAccess implements NBTDatabaseAccess {
	
	private final NBTDatabase database;
	private final CompletableFuture<Void> closeFuture;
	private final ExecutorService executor;
	
	public LocalNBTDatabaseAccess(NBTDatabase database) {
		this.database = database;
		this.closeFuture = new CompletableFuture<>();
		this.executor = Executors.newSingleThreadExecutor();
	}
	
	public NBTDatabase getDatabase() {
		return database;
	}
	
	@Override
	public String getName() {
		return "[Local] " + database.getFile().getAbsolutePath();
	}
	
	@Override
	public CompletableFuture<Void> lockConfig() {
		return CompletableFuture.completedFuture(null);
	}
	
	@Override
	public CompletableFuture<Void> unlockConfig() {
		return CompletableFuture.completedFuture(null);
	}
	
	@Override
	public CompletableFuture<Void> setConfig(Config config) {
		return FutureUtil.runAsync(() -> database.getConfigManager().setConfig(config), executor);
	}
	
	@Override
	public CompletableFuture<Config> getConfig() {
		return FutureUtil.supplyAsync(() -> database.getConfigManager().getConfig(), executor);
	}
	
	@Override
	public CompletableFuture<Void> lockEntry(long id) {
		return CompletableFuture.completedFuture(null);
	}
	
	@Override
	public CompletableFuture<Void> unlockEntry(long id) {
		return CompletableFuture.completedFuture(null);
	}
	
	@Override
	public CompletableFuture<Long> addEntry(String name, byte[] nbt, Entry.Type type, int dataVersion, UUID authorUuid, String authorUsername, boolean verified) {
		return FutureUtil.supplyAsync(() -> database.addEntry(name, nbt, type, dataVersion, authorUuid, authorUsername, verified), executor);
	}
	
	@Override
	public CompletableFuture<Void> editEntry(long id, Optional<String> name, Optional<byte[]> nbt, Optional<Entry.Type> type,
			Optional<Integer> dataVersion, Optional<UUID> authorUuid, Optional<String> authorUsername, Optional<Boolean> verified) {
		return FutureUtil.runAsync(() -> database.editEntry(id, name, nbt, type, dataVersion, authorUuid, authorUsername, verified), executor);
	}
	
	@Override
	public CompletableFuture<Void> removeEntry(long id) {
		return FutureUtil.runAsync(() -> database.removeEntry(id), executor);
	}
	
	@Override
	public CompletableFuture<Optional<Entry>> getEntry(long id) {
		return FutureUtil.supplyAsync(() -> database.getEntry(id), executor);
	}
	
	@Override
	public CompletableFuture<Optional<byte[]>> getEntryNBT(long id) {
		return FutureUtil.supplyAsync(() -> database.getEntryNBT(id), executor);
	}
	
	@Override
	public CompletableFuture<List<Entry>> getEntries(EntryFilter filter, EntryView view) {
		return FutureUtil.supplyAsync(() -> database.getEntries(filter, view), executor);
	}
	
	@Override
	public CompletableFuture<Void> lockTag(String name) {
		return CompletableFuture.completedFuture(null);
	}
	
	@Override
	public CompletableFuture<Void> unlockTag(String name) {
		return CompletableFuture.completedFuture(null);
	}
	
	@Override
	public CompletableFuture<Void> addTag(String name, int color) {
		return FutureUtil.runAsync(() -> database.addTag(name, color), executor);
	}
	
	@Override
	public CompletableFuture<Void> editTag(String currentName, Optional<String> name, Optional<Integer> color) {
		return FutureUtil.runAsync(() -> database.editTag(currentName, name, color), executor);
	}
	
	@Override
	public CompletableFuture<Void> removeTag(String name) {
		return FutureUtil.runAsync(() -> database.removeTag(name), executor);
	}
	
	@Override
	public CompletableFuture<Optional<Tag>> getTag(String name) {
		return FutureUtil.supplyAsync(() -> database.getTag(name), executor);
	}
	
	@Override
	public CompletableFuture<List<Tag>> getTags(TagFilter filter) {
		return FutureUtil.supplyAsync(() -> database.getTags(filter), executor);
	}
	
	@Override
	public CompletableFuture<Void> addTagToEntry(long entry, String tag) {
		return FutureUtil.runAsync(() -> database.addTagToEntry(entry, tag), executor);
	}
	
	@Override
	public CompletableFuture<Void> removeTagFromEntry(long entry, String tag) {
		return FutureUtil.runAsync(() -> database.removeTagFromEntry(entry, tag), executor);
	}
	
	@Override
	public CompletableFuture<Void> getCloseFuture() {
		return closeFuture;
	}
	
	@Override
	public CompletableFuture<Void> closeAsync() {
		closeFuture.complete(null);
		return FutureUtil.shutdown(executor);
	}
	
	@Override
	public void close() throws InterruptedException {
		closeFuture.complete(null);
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
	}
	
}
