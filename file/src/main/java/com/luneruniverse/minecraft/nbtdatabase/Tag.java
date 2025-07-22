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
	
	public boolean isTextColorWhite() {
		float[] channels = new float[3];
		for (int i = 0; i < 3; i++) {
			float channel = ((color >> ((2 - i) * 8)) & 0xFF) / 255.0f;
			if (channel <= 0.04045)
				channel /= 12.92;
			else
				channel = (float) Math.pow((channel + 0.055) / 1.055, 2.4);
			channels[i] = channel;
		}
		return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722 <= 0.179;
	}
	
	@Override
	public String toString() {
		return "Tag[name=" + name + ", color=" + color + "]";
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
}
