package com.luneruniverse.minecraft.nbtdatabase.connection;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.luneruniverse.minecraft.nbtdatabase.NBTEntry;
import com.luneruniverse.minecraft.nbtdatabase.Tag;

public interface NBTDatabaseAccess {
	public CompletableFuture<NBTEntry> addEntry(String name, byte[] nbt, int dataVersion, UUID authorUuid, String authorUsername);
	public CompletableFuture<Void> removeEntry(long id);
	public CompletableFuture<NBTEntry> getEntry(long id);
	public CompletableFuture<List<NBTEntry>> getEntriesByName(String query);
	public CompletableFuture<List<NBTEntry>> getEntriesByAuthorUUID(UUID uuid);
	public CompletableFuture<List<NBTEntry>> getEntriesByAuthorName(String query);
	public CompletableFuture<Void> addTag(String name, int color);
	public CompletableFuture<Void> removeTag(String name);
	public CompletableFuture<List<Tag>> getTags();
	public CompletableFuture<Void> addTagToEntry(long entry, String tag);
	public CompletableFuture<Void> removeTagFromEntry(long entry, String tag);
	public CompletableFuture<List<String>> getTagsByEntry(long entry);
	public CompletableFuture<List<Long>> getEntriesByTag(String tag);
}
