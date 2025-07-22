package com.luneruniverse.minecraft.nbtdatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConfigManager {
	
	private final Connection connection;
	private int maxNbtSize;
	private int maxNumResults;
	
	public ConfigManager(Connection connection) throws SQLException {
		this.connection = connection;
		
		try (Statement sql = connection.createStatement()) {
			sql.setQueryTimeout(5);
			ResultSet result = sql.executeQuery("SELECT * FROM `config`");
			
			Integer maxNbtSize = null;
			Integer maxNumResults = null;
			
			while (result.next()) {
				switch (result.getString("key")) {
					case "max_nbt_size":
						maxNbtSize = result.getInt("value");
						break;
					case "max_num_results":
						maxNumResults = result.getInt("value");
						break;
				}
			}
			
			if (maxNbtSize == null)
				throw new IllegalArgumentException("Invalid database config: missing max_nbt_size (int)");
			if (maxNumResults == null)
				throw new IllegalArgumentException("Invalid database config: missing max_num_results (int)");
			
			this.maxNbtSize = maxNbtSize;
			this.maxNumResults = maxNumResults;
		}
	}
	
	private interface PreparedStatementSetter<T> {
		public void set(PreparedStatement sql, int parameterIndex, T value) throws SQLException;
	}
	private <T> void set(String name, PreparedStatementSetter<T> setter, T value) throws SQLException {
		try (PreparedStatement sql = connection.prepareStatement("UPDATE `config` SET `value`=? WHERE `key`=?")) {
			setter.set(sql, 1, value);
			sql.setString(2, name);
			sql.executeUpdate();
		}
	}
	
	public int getMaxNbtSize() {
		return maxNbtSize;
	}
	public void setMaxNbtSize(int maxNbtSize) throws IllegalRequestException, SQLException {
		if (maxNbtSize < 0)
			throw new IllegalRequestException("maxNbtSize must be >= 0");
		
		this.maxNbtSize = maxNbtSize;
		set("max_nbt_size", PreparedStatement::setInt, maxNbtSize);
	}
	
	public int getMaxNumResults() {
		return maxNumResults;
	}
	public void setMaxNumResults(int maxNumResults) throws IllegalRequestException, SQLException {
		if (maxNumResults < 0)
			throw new IllegalRequestException("maxNumResults must be >= 0");
		
		this.maxNumResults = maxNumResults;
		set("max_num_results", PreparedStatement::setInt, maxNumResults);
	}
	
	public Config getConfig() {
		return new Config(maxNbtSize, maxNumResults);
	}
	public void setConfig(Config config) throws IllegalRequestException, SQLException {
		setMaxNbtSize(config.getMaxNbtSize());
		setMaxNumResults(config.getMaxNumResults());
	}
	
}
