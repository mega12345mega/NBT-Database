package com.luneruniverse.minecraft.nbtdatabase;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class EntryFilter {
	
	private String name;
	private Integer minDataVersion;
	private Integer maxDataVersion;
	private UUID authorUuid;
	private String authorName;
	private Set<String> tags;
	
	public EntryFilter() {}
	public EntryFilter(String name, Integer minDataVersion, Integer maxDataVersion, UUID authorUuid, String authorName, Set<String> tags) {
		this.name = name;
		this.minDataVersion = minDataVersion;
		this.maxDataVersion = maxDataVersion;
		this.authorUuid = authorUuid;
		this.authorName = authorName;
		this.tags = (tags == null || tags.isEmpty() ? null : tags);
	}
	
	public EntryFilter filterByName(String query) {
		name = query;
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
	public EntryFilter filterByAuthorName(String query) {
		authorName = query;
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
	public Integer getMinDataVersion() {
		return minDataVersion;
	}
	public Integer getMaxDataVersion() {
		return maxDataVersion;
	}
	public UUID getAuthorUuid() {
		return authorUuid;
	}
	public String getAuthorName() {
		return authorName;
	}
	public Set<String> getTags() {
		return tags;
	}
	
}
