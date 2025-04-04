package com.luneruniverse.minecraft.nbtdatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class NBTDatabaseConfig {
	
	private final Connection connection;
	private int maxNbtSize;
	
	public NBTDatabaseConfig(Connection connection) throws SQLException {
		this.connection = connection;
		
		try (Statement sql = connection.createStatement()) {
			sql.setQueryTimeout(5);
			ResultSet result = sql.executeQuery("SELECT * FROM `config`");
			
			Integer maxNbtSize = null;
			
			while (result.next()) {
				switch (result.getString("key")) {
					case "max_nbt_size":
						maxNbtSize = result.getInt("value");
						break;
				}
			}
			
			if (maxNbtSize == null)
				throw new IllegalArgumentException("Invalid database config: missing max_nbt_size (int)");
			
			this.maxNbtSize = maxNbtSize;
		}
	}
	
	public int getMaxNbtSize() {
		return maxNbtSize;
	}
	public void setMaxNbtSize(int maxNbtSize) throws SQLException {
		this.maxNbtSize = maxNbtSize;
		
		try (PreparedStatement sql = connection.prepareStatement("UPDATE `config` SET `value`=? WHERE `key`=?")) {
			sql.setInt(1, maxNbtSize);
			sql.setString(2, "max_nbt_size");
			sql.executeUpdate();
		}
	}
	
}
