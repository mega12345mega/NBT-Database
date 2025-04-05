package com.luneruniverse.minecraft.nbtdatabase;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class NBTDatabase {
	
	private final Connection connection;
	private final NBTDatabaseConfig config;
	
	public NBTDatabase(File database) throws SQLException {
		boolean createNew = !database.exists();
		this.connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
		
		if (createNew) {
			try (Statement sql = connection.createStatement()) {
				sql.setQueryTimeout(5);
				
				sql.executeUpdate("CREATE TABLE `config` ("
						+ "`key` TEXT NOT NULL,"
						+ "`value` BLOB NOT NULL)");
				sql.executeUpdate("CREATE UNIQUE INDEX `config-key` ON `config` (`key`)");
				sql.executeUpdate("INSERT INTO `config` VALUES (\"max_nbt_size\", 1048576)");
				
				sql.executeUpdate("CREATE TABLE `entries` ("
						+ "`id` INTEGER PRIMARY KEY,"
						+ "`name` TEXT NOT NULL,"
						+ "`nbt` BLOB NOT NULL,"
						+ "`DataVersion` INTEGER NOT NULL,"
						+ "`author_uuid` TEXT NOT NULL,"
						+ "`author_username` TEXT NOT NULL,"
						+ "`created` INTEGER NOT NULL,"
						+ "`modified` INTEGER NOT NULL,"
						+ "`hash` TEXT NOT NULL,"
						+ "`verified` INTEGER NOT NULL)");
				sql.executeUpdate("CREATE INDEX `entries-author_uuid` ON `entries` (`author_uuid`)");
				
				sql.executeUpdate("CREATE TABLE `tags` ("
						+ "`name` TEXT NOT NULL,"
						+ "`color` INTEGER NOT NULL)");
				sql.executeUpdate("CREATE UNIQUE INDEX `tags-name` ON `tags` (`name`)");
				
				sql.executeUpdate("CREATE TABLE `entries_tags` ("
						+ "`entry_id` INTEGER NOT NULL,"
						+ "`tag` TEXT NOT NULL,"
						+ "FOREIGN KEY(`entry_id`) REFERENCES `entries`(`id`) ON DELETE CASCADE,"
						+ "FOREIGN KEY(`tag`) REFERENCES `tags`(`name`) ON DELETE CASCADE)");
				sql.executeUpdate("CREATE INDEX `entries_tags-entry_id` ON `entries_tags` (`entry_id`)");
				sql.executeUpdate("CREATE INDEX `entries_tags-tag` ON `entries_tags` (`tag`)");
			}
		}
		
		try (Statement sql = connection.createStatement()) {
			sql.setQueryTimeout(5);
			sql.executeUpdate("PRAGMA foreign_keys = ON");
		}
		
		this.config = new NBTDatabaseConfig(connection);
	}
	
	public NBTDatabaseConfig getConfig() {
		return config;
	}
	
	public NBTEntry addEntry(String name, byte[] nbt, int dataVersion, UUID authorUuid, String authorUsername, boolean verified) throws SQLException {
		if (name.length() > 64)
			throw new IllegalArgumentException("name must be <= 64 characters long");
		if (nbt.length > config.getMaxNbtSize())
			throw new IllegalArgumentException("nbt must be <= " + config.getMaxNbtSize() + " bytes long");
		if (authorUsername.length() > 16)
			throw new IllegalArgumentException("authorUsername must be <= 16 characters long");
		
		
		long id = genNewEntryId();
		long created = System.currentTimeMillis();
		long modified = created;
		String hash = NBTEntry.genHash(name, nbt, dataVersion, authorUuid, created, modified);
		
		try (PreparedStatement sql = connection.prepareStatement("INSERT INTO `entries` VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
			sql.setQueryTimeout(5);
			sql.setLong(1, id);
			sql.setString(2, name);
			sql.setBytes(3, nbt);
			sql.setInt(4, dataVersion);
			sql.setString(5, authorUuid.toString());
			sql.setString(6, authorUsername);
			sql.setLong(7, created);
			sql.setLong(8, modified);
			sql.setString(9, hash);
			sql.setInt(10, verified ? 1 : 0);
			sql.executeUpdate();
		}
		
		return new NBTEntry(id, name, nbt, dataVersion, authorUuid, authorUsername, created, modified, hash, verified);
	}
	private long genNewEntryId() throws SQLException {
		Random rand = new Random();
		long id;
		
		try (PreparedStatement sql = connection.prepareStatement("SELECT `id` FROM `entries` WHERE `id`=?")) {
			sql.setQueryTimeout(5);
			do {
				id = (rand.nextLong() & ~(1L << 63));
				sql.setLong(1, id);
			} while (sql.executeQuery().isBeforeFirst());
		}
		
		return id;
	}
	
	public void removeEntry(long id) throws SQLException {
		try (PreparedStatement sql = connection.prepareStatement("DELETE FROM `entries` WHERE `id`=?")) {
			sql.setQueryTimeout(5);
			sql.setLong(1, id);
			sql.executeUpdate();
		}
	}
	
	public NBTEntry getEntry(long id) throws SQLException {
		try (PreparedStatement sql = connection.prepareStatement("SELECT * FROM `entries` WHERE `id`=?")) {
			sql.setQueryTimeout(5);
			sql.setLong(1, id);
			ResultSet result = sql.executeQuery();
			
			if (!result.isBeforeFirst())
				return null;
			return NBTEntry.fromDatabase(result);
		}
	}
	
	public List<NBTEntry> getEntries() throws SQLException {
		try (PreparedStatement sql = connection.prepareStatement("SELECT * FROM `entries`")) {
			sql.setQueryTimeout(5);
			ResultSet result = sql.executeQuery();
			
			List<NBTEntry> output = new ArrayList<>();
			while (result.next())
				output.add(NBTEntry.fromDatabase(result));
			return output;
		}
	}
	
	public List<NBTEntry> getEntriesByName(String query) throws SQLException {
		try (PreparedStatement sql = connection.prepareStatement("SELECT * FROM `entries` WHERE `name` LIKE \"%?%\"")) {
			sql.setQueryTimeout(5);
			sql.setString(1, query);
			ResultSet result = sql.executeQuery();
			
			List<NBTEntry> output = new ArrayList<>();
			while (result.next())
				output.add(NBTEntry.fromDatabase(result));
			return output;
		}
	}
	
	public List<NBTEntry> getEntriesByAuthorUUID(UUID uuid) throws SQLException {
		try (PreparedStatement sql = connection.prepareStatement("SELECT * FROM `entries` WHERE `author_uuid`=?")) {
			sql.setQueryTimeout(5);
			sql.setString(1, uuid.toString());
			ResultSet result = sql.executeQuery();
			
			List<NBTEntry> output = new ArrayList<>();
			while (result.next())
				output.add(NBTEntry.fromDatabase(result));
			return output;
		}
	}
	
	public List<NBTEntry> getEntriesByAuthorName(String query) throws SQLException {
		try (PreparedStatement sql = connection.prepareStatement("SELECT * FROM `entries` WHERE `author_username` LIKE \"%?%\"")) {
			sql.setQueryTimeout(5);
			sql.setString(1, query);
			ResultSet result = sql.executeQuery();
			
			List<NBTEntry> output = new ArrayList<>();
			while (result.next())
				output.add(NBTEntry.fromDatabase(result));
			return output;
		}
	}
	
	public void addTag(String name, int color) throws SQLException {
		try (PreparedStatement sql = connection.prepareStatement("INSERT INTO `tags` VALUES (?, ?)")) {
			sql.setQueryTimeout(5);
			sql.setString(1, name);
			sql.setInt(2, color);
			sql.executeUpdate();
		}
	}
	
	public void removeTag(String name) throws SQLException {
		try (PreparedStatement sql = connection.prepareStatement("DELETE FROM `tags` WHERE `name`=?")) {
			sql.setQueryTimeout(5);
			sql.setString(1, name);
			sql.executeUpdate();
		}
	}
	
	public List<Tag> getTags() throws SQLException {
		try (Statement sql = connection.createStatement()) {
			sql.setQueryTimeout(5);
			ResultSet result = sql.executeQuery("SELECT * FROM `tags`");
			
			List<Tag> output = new ArrayList<>();
			while (result.next())
				output.add(Tag.fromDatabase(result));
			return output;
		}
	}
	
	public void addTagToEntry(long entry, String tag) throws SQLException {
		try (PreparedStatement sql = connection.prepareStatement("INSERT INTO `entries_tags` VALUES(?, ?)")) {
			sql.setQueryTimeout(5);
			sql.setLong(1, entry);
			sql.setString(2, tag);
			sql.executeUpdate();
		}
	}
	
	public void removeTagFromEntry(long entry, String tag) throws SQLException {
		try (PreparedStatement sql = connection.prepareStatement("DELETE FROM `entries_tags` WHERE entry_id=? AND `tag`=?")) {
			sql.setQueryTimeout(5);
			sql.setLong(1, entry);
			sql.setString(2, tag);
			sql.executeUpdate();
		}
	}
	
	public List<String> getTagsByEntry(long entry) throws SQLException {
		try (PreparedStatement sql = connection.prepareStatement("SELECT tag FROM `entries_tags` WHERE `entry_id`=?")) {
			sql.setQueryTimeout(5);
			sql.setLong(1, entry);
			ResultSet result = sql.executeQuery();
			
			List<String> output = new ArrayList<>();
			while (result.next())
				output.add(result.getString("tag"));
			return output;
		}
	}
	
	public List<Long> getEntriesByTag(String tag) throws SQLException {
		try (PreparedStatement sql = connection.prepareStatement("SELECT entry_id FROM `entries_tags` WHERE `tag`=?")) {
			sql.setQueryTimeout(5);
			sql.setString(1, tag);
			ResultSet result = sql.executeQuery();
			
			List<Long> output = new ArrayList<>();
			while (result.next())
				output.add(result.getLong("entry_id"));
			return output;
		}
	}
	
}
