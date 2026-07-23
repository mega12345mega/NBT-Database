package com.luneruniverse.minecraft.nbtdatabase;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import java.util.zip.GZIPInputStream;

import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import com.luneruniverse.minecraft.nbtdatabase.request.EntryFilter;
import com.luneruniverse.minecraft.nbtdatabase.request.EntryView;
import com.luneruniverse.minecraft.nbtdatabase.request.IllegalRequestException;
import com.luneruniverse.minecraft.nbtdatabase.request.TagFilter;
import com.luneruniverse.minecraft.nbtdatabase.sqlbuilder.SQLSelectBuilder;
import com.luneruniverse.minecraft.nbtdatabase.sqlbuilder.SQLUpdateBuilder;

import net.querz.nbt.io.NBTDeserializer;
import net.querz.nbt.io.NBTSerializer;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.IntTag;
import net.querz.nbt.tag.StringTag;

public class NBTDatabase implements AutoCloseable {
	
	public static final int DEFAULT_PORT = 28260;
	public static final int MAGIC;
	static {
		byte[] magicBytes = "nbt".getBytes(StandardCharsets.US_ASCII);
		MAGIC = (magicBytes[0] << 16) | (magicBytes[1] << 8) | magicBytes[2];
	}
	public static final int DATABASE_VERSION = 1;
	
	private final File file;
	private final Connection connection;
	private final ConfigManager config;
	
	public NBTDatabase(File file) throws IllegalRequestException, SQLException {
		this.file = file;
		
		boolean createNew = !file.exists();
		this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
		
		boolean success = false;
		try {
			if (createNew) {
				try (Statement sql = connection.createStatement()) {
					sql.setQueryTimeout(5);
					
					sql.executeUpdate("PRAGMA application_id = " + MAGIC);
					sql.executeUpdate("PRAGMA user_version = " + DATABASE_VERSION);
					
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
							+ "`type` INTEGER NOT NULL,"
							+ "`data_version` INTEGER NOT NULL,"
							+ "`author_uuid` TEXT NOT NULL,"
							+ "`author_username` TEXT NOT NULL,"
							+ "`created` INTEGER NOT NULL,"
							+ "`modified` INTEGER NOT NULL,"
							+ "`hash` TEXT NOT NULL,"
							+ "`verified` INTEGER NOT NULL)");
					sql.executeUpdate("CREATE INDEX `entries-nbt_length` ON `entries` (length(`nbt`))");
					sql.executeUpdate("CREATE INDEX `entries-type` ON `entries` (`type`)");
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
				
				int version = sql.executeQuery("PRAGMA user_version").getInt(1);
				if (version != DATABASE_VERSION)
					throw new IllegalRequestException("Database version " + version + " doesn't match " + DATABASE_VERSION);
			}
			
			this.config = new ConfigManager(connection);
			
			success = true;
		} finally {
			if (!success) {
				try {
					connection.close();
				} catch (SQLException e) {}
			}
		}
	}
	
	public File getFile() {
		return file;
	}
	
	public ConfigManager getConfigManager() {
		return config;
	}
	
	public long addEntry(String name, byte[] nbt, Entry.Type type, int dataVersion, UUID authorUuid, String authorUsername,
			boolean verified) throws IllegalRequestException, SQLException {
		if (name.length() > 256)
			throw new IllegalRequestException("name must be <= 256 characters long");
		if (nbt.length > config.getMaxNbtSize())
			throw new IllegalRequestException("nbt must be <= " + config.getMaxNbtSize() + " bytes long");
		if (dataVersion < 0)
			throw new IllegalRequestException("dataVersion must be >= 0");
		if (authorUsername.length() > 16)
			throw new IllegalRequestException("authorUsername must be <= 16 characters long");
		
		nbt = checkNBT(nbt, type, dataVersion);
		
		long id = genNewEntryId();
		long created = Instant.now().toEpochMilli();
		long modified = created;
		String hash = Entry.genHash(name, nbt, type, dataVersion, authorUuid, created, modified);
		
		try (PreparedStatement sql = connection.prepareStatement("INSERT INTO `entries` VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
			sql.setQueryTimeout(5);
			sql.setLong(1, id);
			sql.setString(2, name);
			sql.setBytes(3, nbt);
			sql.setByte(4, (byte) type.ordinal());
			sql.setInt(5, dataVersion);
			sql.setString(6, authorUuid.toString());
			sql.setString(7, authorUsername);
			sql.setLong(8, created);
			sql.setLong(9, modified);
			sql.setString(10, hash);
			sql.setInt(11, verified ? 1 : 0);
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
	
	public void editEntry(long id, Optional<String> name, Optional<byte[]> nbt, Optional<Entry.Type> type, Optional<Integer> dataVersion,
			Optional<UUID> authorUuid, Optional<String> authorUsername, Optional<Boolean> verified) throws IllegalRequestException, SQLException {
		Entry oldEntry = getEntry(id);
		if (oldEntry == null)
			throw new IllegalRequestException("Entry doesn't exist: " + id);
		
		SQLUpdateBuilder update = new SQLUpdateBuilder("`entries`");
		if (name.isPresent()) {
			if (name.get().length() > 256)
				throw new IllegalRequestException("name must be <= 256 characters long");
			update.addColumn("`name`=?", PreparedStatement::setString, name.get());
		}
		if (nbt.isPresent()) {
			if (nbt.get().length > config.getMaxNbtSize())
				throw new IllegalRequestException("nbt must be <= " + config.getMaxNbtSize() + " bytes long");
			nbt = Optional.of(checkNBT(nbt.get(), type.orElse(oldEntry.getType()), dataVersion.orElse(oldEntry.getDataVersion())));
			update.addColumn("`nbt`=?", PreparedStatement::setBytes, nbt.get());
		}
		if (type.isPresent()) {
			if (!nbt.isPresent() && type.get() != oldEntry.getType())
				throw new IllegalRequestException("type must match the type tag in nbt");
			update.addColumn("`type`=?", PreparedStatement::setByte, (byte) type.get().ordinal());
		}
		if (dataVersion.isPresent()) {
			if (dataVersion.get() < 0)
				throw new IllegalRequestException("dataVersion must be >= 0");
			if (!nbt.isPresent() && dataVersion.get() != oldEntry.getDataVersion())
				throw new IllegalRequestException("dataVersion must match the DataVersion tag in nbt");
			update.addColumn("`data_version`=?", PreparedStatement::setInt, dataVersion.get());
		}
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
		byte[] oldEntryNBT = null;
		if (!nbt.isPresent()) {
			oldEntryNBT = getEntryNBT(id);
			if (oldEntryNBT == null)
				throw new IllegalRequestException("Entry doesn't exist: " + id);
		}
		update.addColumn("`hash`=?", PreparedStatement::setString,
				Entry.genHash(name.orElse(oldEntry.getName()), nbt.orElse(oldEntryNBT), type.orElse(oldEntry.getType()),
						dataVersion.orElse(oldEntry.getDataVersion()), authorUuid.orElse(oldEntry.getAuthorUuid()),
						oldEntry.getCreated(), modified));
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
	
	public Entry getEntry(long id) throws SQLException {
		try (PreparedStatement sql = connection.prepareStatement("SELECT " + Entry.DATABASE_COLUMNS + " FROM `entries` WHERE `id`=?")) {
			sql.setQueryTimeout(5);
			sql.setLong(1, id);
			ResultSet result = sql.executeQuery();
			
			if (!result.isBeforeFirst())
				return null;
			return Entry.fromDatabase(result);
		}
	}
	
	public byte[] getEntryNBT(long id) throws SQLException {
		try (PreparedStatement sql = connection.prepareStatement("SELECT `nbt` FROM `entries` WHERE `id`=?")) {
			sql.setQueryTimeout(5);
			sql.setLong(1, id);
			ResultSet result = sql.executeQuery();
			
			if (!result.isBeforeFirst())
				return null;
			return result.getBytes("nbt");
		}
	}
	
	public List<Entry> getEntries(EntryFilter filter, EntryView view) throws IllegalRequestException, SQLException {
		if (view.getOffset() < 0)
			throw new IllegalRequestException("offset must be >= 0");
		
		SQLSelectBuilder select = new SQLSelectBuilder(Entry.DATABASE_COLUMNS + " FROM `entries`");
		if (filter.getName() != null)
			select.addFilter("`entries`.`name` LIKE ? ESCAPE \"\\\"", PreparedStatement::setString, "%" + escapeQuery(filter.getName()) + "%");
		if (filter.getMinNbtLength() != null || filter.getMaxNbtLength() != null) {
			if (filter.getMaxNbtLength() == null)
				select.addFilter("`nbt_length`>=?", PreparedStatement::setInt, filter.getMinNbtLength());
			else if (filter.getMinNbtLength() == null)
				select.addFilter("`nbt_length`<=?", PreparedStatement::setInt, filter.getMaxNbtLength());
			else if (filter.getMinNbtLength() == filter.getMaxNbtLength())
				select.addFilter("`nbt_length`=?", PreparedStatement::setInt, filter.getMinNbtLength());
			else {
				select.addFilter("`nbt_length` BETWEEN ? AND ?");
				select.addParam(PreparedStatement::setInt, filter.getMinNbtLength());
				select.addParam(PreparedStatement::setInt, filter.getMaxNbtLength());
			}
		}
		if (filter.getType() != null)
			select.addFilter("`entries`.`type`=?", PreparedStatement::setByte, (byte) filter.getType().ordinal());
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
		if (filter.getAuthorUsername() != null)
			select.addFilter("`entries`.`author_username` LIKE ? ESCAPE \"\\\"", PreparedStatement::setString, "%" + escapeQuery(filter.getAuthorUsername()) + "%");
		if (filter.getTags() != null) {
			select.addJoin("JOIN `entries_tags` ON `entries_tags`.`entry_id`=`entries`.`id`");
			select.addFilter("`entries_tags`.`tag` IN " + SQLSelectBuilder.genParamList(filter.getTags().size()));
			for (String tag : filter.getTags())
				select.addParam(PreparedStatement::setString, tag);
			select.addGroup("`entries`.`id`");
			select.addGroupFilter("COUNT(*)=?", PreparedStatement::setInt, filter.getTags().size());
		}
		select.addOrder(view.getOrder().getColumn() + (view.getOrder().isDefaultDesc() == view.isReversedOrder() ? " ASC" : " DESC"));
		if (view.getOrder() != EntryView.Order.CREATED)
			select.addOrder("`created` DESC");
		select.setLimit(config.getMaxNumResults());
		select.setOffset(view.getOffset());
		
		try (PreparedStatement sql = connection.prepareStatement(select.toSQL())) {
			sql.setQueryTimeout(5);
			select.setParams(sql);
			ResultSet result = sql.executeQuery();
			
			List<Entry> output = new ArrayList<>();
			while (result.next())
				output.add(Entry.fromDatabase(result));
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
	
	private byte[] checkNBT(byte[] nbt, Entry.Type type, int dataVersion) throws IllegalRequestException {
		NamedTag rootTag;
		CompoundTag rootValue;
		try {
			rootTag = new NBTDeserializer((nbt[0] & 0xFF) + ((nbt[1] & 0xFF) << 8) == GZIPInputStream.GZIP_MAGIC).fromBytes(nbt);
			if (rootTag.getTag() instanceof CompoundTag)
				rootValue = (CompoundTag) rootTag.getTag();
			else
				throw new IOException();
		} catch (IOException e) {
			throw new IllegalRequestException("nbt must be a valid NBT file");
		}
		
		if (rootValue.containsKey("type") && rootValue.get("type") instanceof StringTag) {
			if (type != Entry.Type.fromNBT(rootValue.getString("type")))
				throw new IllegalRequestException("type must match the type tag in nbt");
		}
		
		if (rootValue.containsKey("DataVersion") && rootValue.get("DataVersion") instanceof IntTag) {
			if (dataVersion != rootValue.getInt("DataVersion"))
				throw new IllegalRequestException("dataVersion must match the DataVersion tag in nbt");
		}
		
		try {
			byte[] compressedBytes = new NBTSerializer(true).toBytes(rootTag);
			if (compressedBytes.length < nbt.length)
				nbt = compressedBytes;
		} catch (IOException e) {
			// Impossible
			throw new RuntimeException("Failed to serialize NBT", e);
		}
		
		return nbt;
	}
	
	private String escapeQuery(String query) {
		return query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
	
	@Override
	public void close() throws SQLException {
		connection.close();
	}
	
}
