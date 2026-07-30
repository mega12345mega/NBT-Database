package com.luneruniverse.minecraft.nbtdatabase.connection.access;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.luneruniverse.minecraft.nbtdatabase.Config;
import com.luneruniverse.minecraft.nbtdatabase.Entry;
import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.connection.AsyncCloseable;
import com.luneruniverse.minecraft.nbtdatabase.request.EntryFilter;
import com.luneruniverse.minecraft.nbtdatabase.request.EntryView;
import com.luneruniverse.minecraft.nbtdatabase.request.TagFilter;

public interface NBTDatabaseAccess extends AsyncCloseable {
	public String getName();
	public CompletableFuture<Void> lockConfig();
	public CompletableFuture<Void> unlockConfig();
	public CompletableFuture<Void> setConfig(Config config);
	public CompletableFuture<Config> getConfig();
	public CompletableFuture<Void> lockEntry(long id);
	public CompletableFuture<Void> unlockEntry(long id);
	public CompletableFuture<Long> addEntry(String name, byte[] nbt, Entry.Type type, int dataVersion, UUID authorUuid, String authorUsername, boolean verified);
	public CompletableFuture<Void> editEntry(long id, Optional<String> name, Optional<byte[]> nbt, Optional<Entry.Type> type,
			Optional<Integer> dataVersion, Optional<UUID> authorUuid, Optional<String> authorUsername, Optional<Boolean> verified);
	public CompletableFuture<Void> removeEntry(long id);
	public CompletableFuture<Optional<Entry>> getEntry(long id);
	public CompletableFuture<Optional<byte[]>> getEntryNBT(long id);
	public CompletableFuture<List<Entry>> getEntries(EntryFilter filter, EntryView view);
	public CompletableFuture<Void> lockTag(String name);
	public CompletableFuture<Void> unlockTag(String name);
	public CompletableFuture<Void> addTag(String name, int color);
	public CompletableFuture<Void> editTag(String currentName, Optional<String> name, Optional<Integer> color);
	public CompletableFuture<Void> removeTag(String name);
	public CompletableFuture<Optional<Tag>> getTag(String name);
	public CompletableFuture<List<Tag>> getTags(TagFilter filter);
	public CompletableFuture<Void> addTagToEntry(long entry, String tag);
	public CompletableFuture<Void> removeTagFromEntry(long entry, String tag);
}
