package com.luneruniverse.minecraft.nbtdatabase.connection.server.auth;

import java.util.List;

import com.luneruniverse.minecraft.nbtdatabase.Config;
import com.luneruniverse.minecraft.nbtdatabase.Entry;
import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.AuthorizationServerException;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.config.GetConfigRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.config.LockConfigRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.config.SetConfigRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.config.UnlockConfigRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.AddEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.EditEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.GetEntriesRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.GetEntryNBTRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.GetEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.LockEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.RemoveEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.UnlockEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.AddTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.AddTagToEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.EditTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.GetTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.GetTagsRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.LockTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.RemoveTagFromEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.RemoveTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.UnlockTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.user.User;

public interface AuthorizationManager {
	public void connect(User user) throws AuthorizationServerException;
	public AuthorizationCheck<LockConfigRequestPacket, Void> lockConfig();
	public AuthorizationCheck<UnlockConfigRequestPacket, Void> unlockConfig();
	public AuthorizationCheck<SetConfigRequestPacket, Void> setConfig();
	public AuthorizationCheck<GetConfigRequestPacket, Config> getConfig();
	public AuthorizationCheck<LockEntryRequestPacket, Void> lockEntry();
	public AuthorizationCheck<UnlockEntryRequestPacket, Void> unlockEntry();
	public AuthorizationCheck<AddEntryRequestPacket, Long> addEntry();
	public AuthorizationCheck<EditEntryRequestPacket, Void> editEntry();
	public AuthorizationCheck<RemoveEntryRequestPacket, Void> removeEntry();
	public AuthorizationCheck<GetEntryRequestPacket, Entry> getEntry();
	public AuthorizationCheck<GetEntryNBTRequestPacket, byte[]> getEntryNBT();
	public AuthorizationCheck<GetEntriesRequestPacket, List<Entry>> getEntries();
	public AuthorizationCheck<LockTagRequestPacket, Void> lockTag();
	public AuthorizationCheck<UnlockTagRequestPacket, Void> unlockTag();
	public AuthorizationCheck<AddTagRequestPacket, Void> addTag();
	public AuthorizationCheck<EditTagRequestPacket, Void> editTag();
	public AuthorizationCheck<RemoveTagRequestPacket, Void> removeTag();
	public AuthorizationCheck<GetTagRequestPacket, Tag> getTag();
	public AuthorizationCheck<GetTagsRequestPacket, List<Tag>> getTags();
	public AuthorizationCheck<AddTagToEntryRequestPacket, Void> addTagToEntry();
	public AuthorizationCheck<RemoveTagFromEntryRequestPacket, Void> removeTagFromEntry();
}
