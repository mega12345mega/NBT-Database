package com.luneruniverse.minecraft.nbtdatabase;

import java.util.UUID;

public class EntryFilter {
	
	private String name;
	private Integer minDataVersion;
	private Integer maxDataVersion;
	private UUID authorUuid;
	private String authorName;
	
	public EntryFilter() {}
	public EntryFilter(String name, Integer minDataVersion, Integer maxDataVersion, UUID authorUuid, String authorName) {
		this.name = name;
		this.minDataVersion = minDataVersion;
		this.maxDataVersion = maxDataVersion;
		this.authorUuid = authorUuid;
		this.authorName = authorName;
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
	
}
