package com.luneruniverse.minecraft.nbtdatabase;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.esotericsoftware.kryo.kryo5.serializers.CollectionSerializer.BindCollection;

public final class EntryFilter {
	
	private String name;
	private Integer minNbtLength;
	private Integer maxNbtLength;
	private Integer minDataVersion;
	private Integer maxDataVersion;
	private UUID authorUuid;
	private String authorUsername;
	private @BindCollection(elementsCanBeNull = false) Set<String> tags;
	
	public EntryFilter() {}
	public EntryFilter(String name, Integer minNbtLength, Integer maxNbtLength, Integer minDataVersion, Integer maxDataVersion,
			UUID authorUuid, String authorUsername, Set<String> tags) {
		this.name = name;
		this.minNbtLength = minNbtLength;
		this.maxNbtLength = maxNbtLength;
		this.minDataVersion = minDataVersion;
		this.maxDataVersion = maxDataVersion;
		this.authorUuid = authorUuid;
		this.authorUsername = authorUsername;
		this.tags = (tags == null || tags.isEmpty() ? null : tags);
	}
	
	public EntryFilter filterByName(String query) {
		name = query;
		return this;
	}
	public EntryFilter filterByNbtLength(int query) {
		minNbtLength = query;
		maxNbtLength = query;
		return this;
	}
	public EntryFilter filterByMinNbtLength(int query) {
		minNbtLength = query;
		return this;
	}
	public EntryFilter filterByMaxNbtLength(int query) {
		maxNbtLength = query;
		return this;
	}
	public EntryFilter filterByDataVersion(int query) {
		minDataVersion = query;
		maxDataVersion = query;
		return this;
	}
	public EntryFilter filterByMinDataVersion(int query) {
		minDataVersion = query;
		return this;
	}
	public EntryFilter filterByMaxDataVersion(int query) {
		maxDataVersion = query;
		return this;
	}
	public EntryFilter filterByAuthorUuid(UUID query) {
		authorUuid = query;
		return this;
	}
	public EntryFilter filterByAuthorUsername(String query) {
		authorUsername = query;
		return this;
	}
	public EntryFilter filterByTags(Set<String> query) {
		if (query == null || query.isEmpty())
			return this;
		if (tags == null)
			tags = new HashSet<>();
		tags.addAll(query);
		return this;
	}
	public EntryFilter filterByTag(String query) {
		if (tags == null)
			tags = new HashSet<>();
		tags.add(query);
		return this;
	}
	
	public String getName() {
		return name;
	}
	public Integer getMinNbtLength() {
		return minNbtLength;
	}
	public Integer getMaxNbtLength() {
		return maxNbtLength;
	}
	public Integer getMinDataVersion() {
		return minDataVersion;
	}
	public Integer getMaxDataVersion() {
		return maxDataVersion;
	}
	public UUID getAuthorUuid() {
		return authorUuid;
	}
	public String getAuthorUsername() {
		return authorUsername;
	}
	public Set<String> getTags() {
		return tags;
	}
	
}
