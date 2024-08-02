package com.luneruniverse.minecraft.nbtdatabase;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Tag {
	
	static Tag fromDatabase(ResultSet result) throws SQLException {
		return new Tag(result.getString("name"), result.getInt("color"));
	}
	
	public final String name;
	public final int color;
	
	public Tag(String name, int color) {
		this.name = name;
		this.color = color;
	}
	
}
