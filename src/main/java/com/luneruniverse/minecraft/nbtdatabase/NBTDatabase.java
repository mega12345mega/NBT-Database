package com.luneruniverse.minecraft.nbtdatabase;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import com.luneruniverse.minecraft.nbtdatabase.connection.NBTDatabaseMetadata;
import com.luneruniverse.minecraft.nbtdatabase.sqlbuilder.SQLSelectBuilder;
import com.luneruniverse.minecraft.nbtdatabase.sqlbuilder.SQLUpdateBuilder;

public class NBTDatabase implements AutoCloseable {
	
	private final File file;
	private final Connection connection;
	private final NBTDatabaseConfig config;
	
	public NBTDatabase(File file) throws SQLException {
		this.file = file;
		
		boolean createNew = !file.exists();
		this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
		
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
						+ "FOREIGN KEY (`entry_id`) REFERENCES `entries` (`id`) ON UPDATE CASCADE ON DELETE CASCADE,"
						+ "FOREIGN KEY (`tag`) REFERENCES `tags` (`name`) ON UPDATE CASCADE ON DELETE CASCADE,"
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
	
	public File getFile() {
		return file;
	}
	
	public NBTDatabaseConfig getConfig() {
		return config;
	}
	
	public NBTDatabaseMetadata getMetadata() {
		return new NBTDatabaseMetadata(config.getMaxNbtSize(), config.getMaxNumResults());
	}
	
	public long addEntry(String name, byte[] nbt, int dataVersion, UUID authorUuid, String authorUsername, boolean verified) throws IllegalRequestException, SQLException {
		if (name.length() > 256)
			throw new IllegalRequestException("name must be <= 256 characters long");
		if (nbt.length > config.getMaxNbtSize())
			throw new IllegalRequestException("nbt must be <= " + config.getMaxNbtSize() + " bytes long");
		if (authorUsername.length() > 16)
			throw new IllegalRequestException("authorUsername must be <= 16 characters long");
		
		long id = genNewEntryId();
		long created = Instant.now().toEpochMilli();
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
	
	public void editEntry(long id, Optional<String> name, Optional<byte[]> nbt, Optional<Integer> dataVersion,
			Optional<UUID> authorUuid, Optional<String> authorUsername, Optional<Boolean> verified) throws IllegalRequestException, SQLException {
		SQLUpdateBuilder update = new SQLUpdateBuilder("`entries`");
		if (name.isPresent()) {
			if (name.get().length() > 256)
				throw new IllegalRequestException("name must be <= 256 characters long");
			update.addColumn("`name`=?", PreparedStatement::setString, name.get());
		}
		if (nbt.isPresent()) {
			if (nbt.get().length > config.getMaxNbtSize())
				throw new IllegalRequestException("nbt must be <= " + config.getMaxNbtSize() + " bytes long");
			update.addColumn("`nbt`=?", PreparedStatement::setBytes, nbt.get());
		}
		if (dataVersion.isPresent())
			update.addColumn("`data_version`=?", PreparedStatement::setInt, dataVersion.get());
		if (authorUuid.isPresent())
			update.addColumn("`author_uuid`=?", PreparedStatement::setString, authorUuid.get().toString());
		if (authorUsername.isPresent()) {
			if (authorUsername.get().length() > 16)
				throw new IllegalRequestException("authorUsername must be <= 16 characters long");
			update.addColumn("`author_username`=?", PreparedStatement::setString, authorUsername.get());
		}
		if (verified.isPresent())
			update.addColumn("`verified`=?", PreparedStatement::setBoolean, verified.get());
		if (!update.isValid())
			throw new IllegalRequestException("Nothing requested to be updated for entry with id " + id);
		long modified = Instant.now().toEpochMilli();
		update.addColumn("`modified`=?", PreparedStatement::setLong, modified);
		NBTEntry oldEntry = getEntry(id);
		if (oldEntry == null)
			throw new IllegalRequestException("Entry doesn't exist: " + id);
		update.addColumn("`hash`=?", PreparedStatement::setString,
				NBTEntry.genHash(name.orElse(oldEntry.name), nbt.orElse(oldEntry.nbt), dataVersion.orElse(oldEntry.dataVersion),
						authorUuid.orElse(oldEntry.authorUuid), oldEntry.created, modified));
		update.addFilter("`id`=?", PreparedStatement::setLong, id);
		
		try (PreparedStatement sql = connection.prepareStatement(update.toSQL())) {
			sql.setQueryTimeout(5);
			update.setParams(sql);
			if (sql.executeUpdate() == 0)
				throw new IllegalRequestException("Entry doesn't exist: " + id);
		}
	}
	
	public void removeEntry(long id) throws IllegalRequestException, SQLException {
		try (PreparedStatement sql = connection.prepareStatement("DELETE FROM `entries` WHERE `id`=?")) {
			sql.setQueryTimeout(5);
			sql.setLong(1, id);
			if (sql.executeUpdate() == 0)
				throw new IllegalRequestException("Entry doesn't exist: " + id);
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
	
	public void addTag(String name, int color) throws IllegalRequestException, SQLException {
		if (name.length() > 256)
			throw new IllegalRequestException("name must be <= 256 characters long");
		
		try (PreparedStatement sql = connection.prepareStatement("INSERT INTO `tags` VALUES (?, ?)")) {
			sql.setQueryTimeout(5);
			sql.setString(1, name);
			sql.setInt(2, color & 0xFFFFFF);
			try {
				sql.executeUpdate();
			} catch (SQLiteException e) {
				if (e.getResultCode() == SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE)
					throw new IllegalRequestException("Tag already exists: " + name);
				else
					throw e;
			}
		}
	}
	
	public void editTag(String currentName, Optional<String> name, Optional<Integer> color) throws IllegalRequestException, SQLException {
		SQLUpdateBuilder update = new SQLUpdateBuilder("`tags`");
		if (name.isPresent()) {
			if (name.get().length() > 256)
				throw new IllegalRequestException("name must be <= 256 characters long");
			update.addColumn("`name`=?", PreparedStatement::setString, name.get());
		}
		if (color.isPresent())
			update.addColumn("`color`=?", PreparedStatement::setInt, color.get());
		if (!update.isValid())
			throw new IllegalRequestException("Nothing requested to be updated for tag: " + currentName);
		update.addFilter("`name`=?", PreparedStatement::setString, currentName);
		
		try (PreparedStatement sql = connection.prepareStatement(update.toSQL())) {
			sql.setQueryTimeout(5);
			update.setParams(sql);
			try {
				if (sql.executeUpdate() == 0)
					throw new IllegalRequestException("Tag doesn't exist: " + currentName);
			} catch (SQLiteException e) {
				if (e.getResultCode() == SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE)
					throw new IllegalRequestException("Tag already exists: " + name.get());
				else
					throw e;
			}
		}
	}
	
	public void removeTag(String name) throws IllegalRequestException, SQLException {
		try (PreparedStatement sql = connection.prepareStatement("DELETE FROM `tags` WHERE `name`=?")) {
			sql.setQueryTimeout(5);
			sql.setString(1, name);
			if (sql.executeUpdate() == 0)
				throw new IllegalRequestException("Tag doesn't exist: " + name);
		}
	}
	
	public Tag getTag(String name) throws SQLException {
		try (PreparedStatement sql = connection.prepareStatement("SELECT * FROM `tags` WHERE `name`=?")) {
			sql.setQueryTimeout(5);
			sql.setString(1, name);
			ResultSet result = sql.executeQuery();
			
			if (!result.isBeforeFirst())
				return null;
			return Tag.fromDatabase(result);
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
		select.addOrder("`tags`.`name` ASC");
		
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
	
	public void addTagToEntry(long entry, String tag) throws IllegalRequestException, SQLException {
		try (PreparedStatement sql = connection.prepareStatement("INSERT INTO `entries_tags` VALUES(?, ?)")) {
			sql.setQueryTimeout(5);
			sql.setLong(1, entry);
			sql.setString(2, tag);
			try {
				sql.executeUpdate();
			} catch (SQLiteException e) {
				if (e.getResultCode() == SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE)
					throw new IllegalRequestException("Entry with id " + entry + " already has tag '" + tag + "'");
				else if (e.getResultCode() == SQLiteErrorCode.SQLITE_CONSTRAINT_FOREIGNKEY)
					throw new IllegalRequestException(getEntry(entry) == null ? "Entry doesn't exist: " + entry : "Tag doesn't exist: " + tag);
				else
					throw e;
			}
		}
	}
	
	public void removeTagFromEntry(long entry, String tag) throws IllegalRequestException, SQLException {
		try (PreparedStatement sql = connection.prepareStatement("DELETE FROM `entries_tags` WHERE entry_id=? AND `tag`=?")) {
			sql.setQueryTimeout(5);
			sql.setLong(1, entry);
			sql.setString(2, tag);
			if (sql.executeUpdate() == 0) {
				if (getEntry(entry) == null)
					throw new IllegalRequestException("Entry doesn't exist: " + entry);
				if (getTag(tag) == null)
					throw new IllegalRequestException("Tag doesn't exist: " + tag);
				throw new IllegalRequestException("Entry with id " + entry + " already doesn't have tag '" + tag + "'");
			}
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
