package com.luneruniverse.minecraft.nbtdatabase.connection;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.luneruniverse.minecraft.nbtdatabase.NBTDatabase;
import com.luneruniverse.minecraft.nbtdatabase.NBTEntry;
import com.luneruniverse.minecraft.nbtdatabase.Tag;

public class LocalNBTDatabaseAccess implements NBTDatabaseAccess {
	
	private final NBTDatabase database;
	private final boolean verified;
	private final Executor executor;
	
	public LocalNBTDatabaseAccess(NBTDatabase database, boolean verified) {
		this.database = database;
		this.verified = verified;
		this.executor = Executors.newSingleThreadExecutor();
	}
	
	@Override
	public CompletableFuture<NBTEntry> addEntry(String name, byte[] nbt, int dataVersion, UUID authorUuid, String authorUsername) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return database.addEntry(name, nbt, dataVersion, authorUuid, authorUsername, verified);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to add entry", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<Void> removeEntry(long id) {
		return CompletableFuture.runAsync(() -> {
			try {
				database.removeEntry(id);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to remove entry", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<NBTEntry> getEntry(long id) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return database.getEntry(id);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to get entry", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<List<NBTEntry>> getEntries() {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return database.getEntries();
			} catch (SQLException e) {
				throw new RuntimeException("Failed to get entries", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<List<NBTEntry>> getEntriesByName(String query) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return database.getEntriesByName(query);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to get entries by name", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<List<NBTEntry>> getEntriesByAuthorUUID(UUID uuid) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return database.getEntriesByAuthorUUID(uuid);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to get entries by author uuid", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<List<NBTEntry>> getEntriesByAuthorName(String query) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return database.getEntriesByAuthorName(query);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to get entries by author name", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<Void> addTag(String name, int color) {
		return CompletableFuture.runAsync(() -> {
			try {
				database.addTag(name, color);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to add a tag", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<Void> removeTag(String name) {
		return CompletableFuture.runAsync(() -> {
			try {
				database.removeTag(name);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to remove a tag", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<List<Tag>> getTags() {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return database.getTags();
			} catch (SQLException e) {
				throw new RuntimeException("Failed to get tags", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<Void> addTagToEntry(long entry, String tag) {
		return CompletableFuture.runAsync(() -> {
			try {
				database.addTagToEntry(entry, tag);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to add a tag to an entry", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<Void> removeTagFromEntry(long entry, String tag) {
		return CompletableFuture.runAsync(() -> {
			try {
				database.removeTagFromEntry(entry, tag);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to remove a tag from an entry", e);
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<List<String>> getTagsByEntry(long entry) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return database.getTagsByEntry(entry);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to get tags by entry");
			}
		}, executor);
	}
	
	@Override
	public CompletableFuture<List<Long>> getEntriesByTag(String tag) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return database.getEntriesByTag(tag);
			} catch (SQLException e) {
				throw new RuntimeException("Failed to get entries by tag");
			}
		}, executor);
	}
	
}
