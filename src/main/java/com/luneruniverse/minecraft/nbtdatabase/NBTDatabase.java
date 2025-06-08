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

import com.luneruniverse.minecraft.nbtdatabase.connection.NBTDatabaseMetadata;

public class NBTDatabase implements AutoCloseable {
	
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
				sql.executeUpdate("INSERT INTO `config` VALUES (\"max_num_results\", 100)");
				
				sql.executeUpdate("CREATE TABLE `entries` ("
						+ "`id` INTEGER PRIMARY KEY,"
						+ "`name` TEXT NOT NULL,"
						+ "`nbt` BLOB NOT NULL,"
						+ "`data_version` INTEGER NOT NULL,"
						+ "`author_uuid` TEXT NOT NULL,"
						+ "`author_username` TEXT NOT NULL,"
						+ "`created` INTEGER NOT NULL,"
						+ "`modified` INTEGER NOT NULL,"
						+ "`hash` TEXT NOT NULL,"
						+ "`verified` INTEGER NOT NULL)");
				sql.executeUpdate("CREATE INDEX `entries-data_version` ON `entries` (`data_version`)");
				sql.executeUpdate("CREATE INDEX `entries-author_uuid` ON `entries` (`author_uuid`)");
				
				sql.executeUpdate("CREATE TABLE `tags` ("
						+ "`name` TEXT NOT NULL,"
						+ "`color` INTEGER NOT NULL)");
				sql.executeUpdate("CREATE UNIQUE INDEX `tags-name` ON `tags` (`name`)");
				
				sql.executeUpdate("CREATE TABLE `entries_tags` ("
						+ "`entry_id` INTEGER NOT NULL,"
						+ "`tag` TEXT NOT NULL,"
						+ "FOREIGN KEY (`entry_id`) REFERENCES `entries` (`id`) ON DELETE CASCADE,"
						+ "FOREIGN KEY (`tag`) REFERENCES `tags` (`name`) ON DELETE CASCADE,"
						+ "UNIQUE (`entry_id`, `tag`))");
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
	
	public NBTDatabaseMetadata getMetadata() {
		return new NBTDatabaseMetadata(config.getMaxNbtSize(), config.getMaxNumResults());
	}
	
	public long addEntry(String name, byte[] nbt, int dataVersion, UUID authorUuid, String authorUsername, boolean verified) throws IllegalArgumentException, SQLException {
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
		
		return id;
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
	
	public List<NBTEntry> getEntries(EntryFilter filter) throws SQLException {
		SQLSelectBuilder select = new SQLSelectBuilder("`entries`.* FROM `entries`");
		if (filter.getName() != null)
			select.addFilter("`entries`.`name` LIKE ? ESCAPE \"\\\"", PreparedStatement::setString, "%" + escapeQuery(filter.getName()) + "%");
		if (filter.getMinDataVersion() != null || filter.getMaxDataVersion() != null) {
			if (filter.getMaxDataVersion() == null)
				select.addFilter("`entries`.`data_version`>=?", PreparedStatement::setInt, filter.getMinDataVersion());
			else if (filter.getMinDataVersion() == null)
				select.addFilter("`entries`.`data_version`<=?", PreparedStatement::setInt, filter.getMaxDataVersion());
			else if (filter.getMinDataVersion() == filter.getMaxDataVersion())
				select.addFilter("`entries`.`data_version`=?", PreparedStatement::setInt, filter.getMinDataVersion());
			else {
				select.addFilter("`entries`.`data_version` BETWEEN ? AND ?");
				select.addParam(PreparedStatement::setInt, filter.getMinDataVersion());
				select.addParam(PreparedStatement::setInt, filter.getMaxDataVersion());
			}
		}
		if (filter.getAuthorUuid() != null)
			select.addFilter("`entries`.`author_uuid`=?", PreparedStatement::setString, filter.getAuthorUuid().toString());
		if (filter.getAuthorName() != null)
			select.addFilter("`entries`.`author_username` LIKE ? ESCAPE \"\\\"", PreparedStatement::setString, "%" + escapeQuery(filter.getAuthorName()) + "%");
		if (filter.getTags() != null) {
			select.addJoin("JOIN `entries_tags` ON `entries_tags`.`entry_id`=`entries`.`id`");
			select.addFilter("`entries_tags`.`tag` IN " + SQLSelectBuilder.genParamList(filter.getTags().size()));
			for (String tag : filter.getTags())
				select.addParam(PreparedStatement::setString, tag);
			select.addGroup("`entries`.`id`");
			select.addGroupFilter("COUNT(*)=?", PreparedStatement::setInt, filter.getTags().size());
		}
		select.setLimit(config.getMaxNumResults());
		
		try (PreparedStatement sql = connection.prepareStatement(select.toSQL())) {
			sql.setQueryTimeout(5);
			select.setParams(sql);
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
	
	public List<Tag> getTags(TagFilter filter) throws SQLException {
		SQLSelectBuilder select = new SQLSelectBuilder("`tags`.* FROM `tags`");
		if (filter.getName() != null)
			select.addFilter("`tags`.`name` LIKE ? ESCAPE \"\\\"", PreparedStatement::setString, "%" + escapeQuery(filter.getName()) + "%");
		if (filter.getEntryId() != null) {
			select.addJoin("JOIN `entries_tags` ON `entries_tags`.`tag`=`tags`.`name`");
			select.addFilter("`entries_tags`.`entry_id`=?", PreparedStatement::setLong, filter.getEntryId());
		}
		
		try (PreparedStatement sql = connection.prepareStatement(select.toSQL())) {
			sql.setQueryTimeout(5);
			select.setParams(sql);
			ResultSet result = sql.executeQuery();
			
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
	
	private String escapeQuery(String query) {
		return query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
	
	@Override
	public void close() throws SQLException {
		connection.close();
	}
	
}
