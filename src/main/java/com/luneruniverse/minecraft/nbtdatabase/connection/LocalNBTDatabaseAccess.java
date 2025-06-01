package com.luneruniverse.minecraft.nbtdatabase.connection;

import java.sql.SQLException;
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
		return Util.supplyAsync(() -> {
			try {
				return database.addEntry(name, nbt, dataVersion, authorUuid, authorUsername, verified);
			} catch (IllegalArgumentException | SQLException e) {
				throw new RuntimeException("Failed to add entry", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<Void> removeEntry(long id) {
		return Util.runAsync(() -> {
			try {
				database.removeEntry(id);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to remove entry", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<NBTEntry> getEntry(long id) {
		return Util.supplyAsync(() -> {
			try {
				return database.getEntry(id);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to get entry", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<List<NBTEntry>> getEntries() {
		return Util.supplyAsync(() -> {
			try {
				return database.getEntries();
			} catch (SQLException e) {
				throw new RuntimeException("Failed to get entries", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<List<NBTEntry>> getEntriesByName(String query) {
		return Util.supplyAsync(() -> {
			try {
				return database.getEntriesByName(query);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to get entries by name", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<List<NBTEntry>> getEntriesByAuthorUUID(UUID query) {
		return Util.supplyAsync(() -> {
			try {
				return database.getEntriesByAuthorUUID(query);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to get entries by author uuid", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<List<NBTEntry>> getEntriesByAuthorName(String query) {
		return Util.supplyAsync(() -> {
			try {
				return database.getEntriesByAuthorName(query);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to get entries by author name", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<Void> addTag(String name, int color) {
		return Util.runAsync(() -> {
			try {
				database.addTag(name, color);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to add a tag", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<Void> removeTag(String name) {
		return Util.runAsync(() -> {
			try {
				database.removeTag(name);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to remove a tag", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<List<Tag>> getTags() {
		return Util.supplyAsync(() -> {
			try {
				return database.getTags();
			} catch (SQLException e) {
				throw new RuntimeException("Failed to get tags", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<Void> addTagToEntry(long entry, String tag) {
		return Util.runAsync(() -> {
			try {
				database.addTagToEntry(entry, tag);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to add a tag to an entry", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<Void> removeTagFromEntry(long entry, String tag) {
		return Util.runAsync(() -> {
			try {
				database.removeTagFromEntry(entry, tag);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to remove a tag from an entry", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<List<Tag>> getTagsByEntry(long entry) {
		return Util.supplyAsync(() -> {
			try {
				return database.getTagsByEntry(entry);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to get tags by entry");
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<List<NBTEntry>> getEntriesByTag(String tag) {
		return Util.supplyAsync(() -> {
			try {
				return database.getEntriesByTag(tag);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to get entries by tag");
			}
		}, executor);
	}
	
	@Override
	public void close() throws InterruptedException {
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
	}
	
}
