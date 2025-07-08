package com.luneruniverse.minecraft.nbtdatabase.connection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.luneruniverse.minecraft.nbtdatabase.EntryFilter;
import com.luneruniverse.minecraft.nbtdatabase.NBTEntry;
import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.TagFilter;

public interface NBTDatabaseAccess extends AutoCloseable {
	public String getName();
	public CompletableFuture<NBTDatabaseMetadata> getMetadata();
	public CompletableFuture<Long> addEntry(String name, byte[] nbt, int dataVersion, UUID authorUuid, String authorUsername, boolean verified);
	public CompletableFuture<Void> editEntry(long id, Optional<String> name, Optional<byte[]> nbt, Optional<Integer> dataVersion,
			Optional<UUID> authorUuid, Optional<String> authorUsername, Optional<Boolean> verified);
	public CompletableFuture<Void> removeEntry(long id);
	public CompletableFuture<NBTEntry> getEntry(long id);
	public CompletableFuture<List<NBTEntry>> getEntries(EntryFilter filter);
	public CompletableFuture<Void> addTag(String name, int color);
	public CompletableFuture<Void> editTag(String currentName, Optional<String> name, Optional<Integer> color);
	public CompletableFuture<Void> removeTag(String name);
	public CompletableFuture<Tag> getTag(String name);
	public CompletableFuture<List<Tag>> getTags(TagFilter filter);
	public CompletableFuture<Void> addTagToEntry(long entry, String tag);
	public CompletableFuture<Void> removeTagFromEntry(long entry, String tag);
	public CompletableFuture<Void> closeAsync();
}
