package com.luneruniverse.minecraft.nbtdatabase;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class NBTEntry {
	
	static final String DATABASE_COLUMNS = "`entries`.`id`, `entries`.`name`, length(`entries`.`nbt`) AS `nbt_length`, "
			+ "`entries`.`data_version`, `entries`.`author_uuid`, `entries`.`author_username`, "
			+ "`entries`.`created`, `entries`.`modified`, `entries`.`hash`, `entries`.`verified`";
	static NBTEntry fromDatabase(ResultSet row) throws SQLException {
		return new NBTEntry(
				row.getLong("id"),
				row.getString("name"),
				row.getInt("nbt_length"),
				row.getInt("data_version"),
				UUID.fromString(row.getString("author_uuid")),
				row.getString("author_username"),
				row.getLong("created"),
				row.getLong("modified"),
				row.getString("hash"),
				row.getInt("verified") != 0);
	}
	
	public static String genHash(String name, byte[] nbt, int dataVersion, UUID authorUuid, long created, long modified) {
		try (ByteArrayOutputStream buf = new ByteArrayOutputStream();
				DataOutputStream bufData = new DataOutputStream(buf);) {
			byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
			bufData.writeInt(nameBytes.length);
			bufData.write(nameBytes);
			
			bufData.writeInt(nbt.length);
			bufData.write(nbt);
			
			bufData.writeInt(dataVersion);
			
			byte[] authorUuidBytes = authorUuid.toString().getBytes(StandardCharsets.UTF_8);
			bufData.writeInt(authorUuidBytes.length);
			bufData.write(authorUuidBytes);
			
			bufData.writeLong(created);
			
			bufData.writeLong(modified);
			
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(buf.toByteArray());
			StringBuilder hashStr = new StringBuilder();
			for (byte b : hash)
				hashStr.append(String.format("%02x", b));
			return hashStr.toString();
		} catch (IOException | NoSuchAlgorithmException e) {
			throw new RuntimeException("Failed to generate hash", e);
		}
	}
	
	public final long id;
	public final String name;
	public final int nbtLength;
	public final int dataVersion;
	public final UUID authorUuid;
	public final String authorUsername;
	public final long created;
	public final long modified;
	public final String hash;
	public final boolean verified;
	
	public NBTEntry(long id, String name, int nbtLength, int dataVersion, UUID authorUuid, String authorUsername,
			long created, long modified, String hash, boolean verified) {
		this.id = id;
		this.name = name;
		this.nbtLength = nbtLength;
		this.dataVersion = dataVersion;
		this.authorUuid = authorUuid;
		this.authorUsername = authorUsername;
		this.created = created;
		this.modified = modified;
		this.hash = hash;
		this.verified = verified;
	}
	
	@Override
	public String toString() {
		return "NBTEntry[id=" + id + ", name=" + name + ", ...]";
	}
	
}
