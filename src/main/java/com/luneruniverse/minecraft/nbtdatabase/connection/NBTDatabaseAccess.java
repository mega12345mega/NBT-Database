package com.luneruniverse.minecraft.nbtdatabase.connection;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.luneruniverse.minecraft.nbtdatabase.NBTEntry;
import com.luneruniverse.minecraft.nbtdatabase.Tag;

public interface NBTDatabaseAccess extends AutoCloseable {
	public CompletableFuture<NBTDatabaseMetadata> getMetadata();
	public CompletableFuture<Long> addEntry(String name, byte[] nbt, int dataVersion, UUID authorUuid, String authorUsername, boolean verified);
	public CompletableFuture<Void> removeEntry(long id);
	public CompletableFuture<NBTEntry> getEntry(long id);
	public CompletableFuture<List<NBTEntry>> getEntries();
	public CompletableFuture<List<NBTEntry>> getEntriesByName(String query);
	public CompletableFuture<List<NBTEntry>> getEntriesByAuthorUUID(UUID query);
	public CompletableFuture<List<NBTEntry>> getEntriesByAuthorName(String query);
	public CompletableFuture<Void> addTag(String name, int color);
	public CompletableFuture<Void> removeTag(String name);
	public CompletableFuture<List<Tag>> getTags();
	public CompletableFuture<Void> addTagToEntry(long entry, String tag);
	public CompletableFuture<Void> removeTagFromEntry(long entry, String tag);
	public CompletableFuture<List<Tag>> getTagsByEntry(long entry);
	public CompletableFuture<List<NBTEntry>> getEntriesByTag(String tag);
	public CompletableFuture<Void> closeAsync();
}
