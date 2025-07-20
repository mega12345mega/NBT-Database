package com.luneruniverse.minecraft.nbtdatabase;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;

public final class Tag {
	
	static Tag fromDatabase(ResultSet result) throws SQLException {
		return new Tag(result.getString("name"), result.getInt("color"));
	}
	
	private @NotNull String name;
	private int color;
	
	public Tag(String name, int color) {
		this.name = name;
		this.color = color;
	}
	Tag() {
		// Deserialization
	}
	
	public String getName() {
		return name;
	}
	public int getColor() {
		return color;
	}
	
	@Override
	public String toString() {
		return "Tag[name=" + name + ", color=" + color + "]";
	}
	
}
