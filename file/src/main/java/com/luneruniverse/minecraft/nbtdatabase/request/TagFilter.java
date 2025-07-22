package com.luneruniverse.minecraft.nbtdatabase.request;

public final class TagFilter {
	
	private String name;
	private Long entryId;
	
	public TagFilter() {}
	public TagFilter(String name, Long entryId) {
		this.name = name;
		this.entryId = entryId;
	}
	
	public TagFilter filterByName(String query) {
		name = query;
		return this;
	}
	public TagFilter filterByEntryId(long query) {
		entryId = query;
		return this;
	}
	
	public String getName() {
		return name;
	}
	public Long getEntryId() {
		return entryId;
	}
	
}
