package com.luneruniverse.minecraft.nbtdatabase.connection.server.auth;

import java.util.List;
import java.util.Optional;

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

public class CachedAuthorizationManager implements AuthorizationManager {
	
	private final AuthorizationManager manager;
	private final AuthorizationCheck<LockConfigRequestPacket, Void> lockConfig;
	private final AuthorizationCheck<UnlockConfigRequestPacket, Void> unlockConfig;
	private final AuthorizationCheck<SetConfigRequestPacket, Void> setConfig;
	private final AuthorizationCheck<GetConfigRequestPacket, Config> getConfig;
	private final AuthorizationCheck<LockEntryRequestPacket, Void> lockEntry;
	private final AuthorizationCheck<UnlockEntryRequestPacket, Void> unlockEntry;
	private final AuthorizationCheck<AddEntryRequestPacket, Long> addEntry;
	private final AuthorizationCheck<EditEntryRequestPacket, Void> editEntry;
	private final AuthorizationCheck<RemoveEntryRequestPacket, Void> removeEntry;
	private final AuthorizationCheck<GetEntryRequestPacket, Optional<Entry>> getEntry;
	private final AuthorizationCheck<GetEntryNBTRequestPacket, Optional<byte[]>> getEntryNBT;
	private final AuthorizationCheck<GetEntriesRequestPacket, List<Entry>> getEntries;
	private final AuthorizationCheck<LockTagRequestPacket, Void> lockTag;
	private final AuthorizationCheck<UnlockTagRequestPacket, Void> unlockTag;
	private final AuthorizationCheck<AddTagRequestPacket, Void> addTag;
	private final AuthorizationCheck<EditTagRequestPacket, Void> editTag;
	private final AuthorizationCheck<RemoveTagRequestPacket, Void> removeTag;
	private final AuthorizationCheck<GetTagRequestPacket, Optional<Tag>> getTag;
	private final AuthorizationCheck<GetTagsRequestPacket, List<Tag>> getTags;
	private final AuthorizationCheck<AddTagToEntryRequestPacket, Void> addTagToEntry;
	private final AuthorizationCheck<RemoveTagFromEntryRequestPacket, Void> removeTagFromEntry;
	
	public CachedAuthorizationManager(AuthorizationManager manager) {
		this.manager = manager;
		this.lockConfig = manager.lockConfig();
		this.unlockConfig = manager.unlockConfig();
		this.setConfig = manager.setConfig();
		this.getConfig = manager.getConfig();
		this.lockEntry = manager.lockEntry();
		this.unlockEntry = manager.unlockEntry();
		this.addEntry = manager.addEntry();
		this.editEntry = manager.editEntry();
		this.removeEntry = manager.removeEntry();
		this.getEntry = manager.getEntry();
		this.getEntryNBT = manager.getEntryNBT();
		this.getEntries = manager.getEntries();
		this.lockTag = manager.lockTag();
		this.unlockTag = manager.unlockTag();
		this.addTag = manager.addTag();
		this.editTag = manager.editTag();
		this.removeTag = manager.removeTag();
		this.getTag = manager.getTag();
		this.getTags = manager.getTags();
		this.addTagToEntry = manager.addTagToEntry();
		this.removeTagFromEntry = manager.removeTagFromEntry();
	}
	
	@Override
	public void connect(User user) throws AuthorizationServerException {
		manager.connect(user);
	}
	
	@Override
	public AuthorizationCheck<LockConfigRequestPacket, Void> lockConfig() {
		return lockConfig;
	}
	
	@Override
	public AuthorizationCheck<UnlockConfigRequestPacket, Void> unlockConfig() {
		return unlockConfig;
	}
	
	@Override
	public AuthorizationCheck<SetConfigRequestPacket, Void> setConfig() {
		return setConfig;
	}
	
	@Override
	public AuthorizationCheck<GetConfigRequestPacket, Config> getConfig() {
		return getConfig;
	}
	
	@Override
	public AuthorizationCheck<LockEntryRequestPacket, Void> lockEntry() {
		return lockEntry;
	}
	
	@Override
	public AuthorizationCheck<UnlockEntryRequestPacket, Void> unlockEntry() {
		return unlockEntry;
	}
	
	@Override
	public AuthorizationCheck<AddEntryRequestPacket, Long> addEntry() {
		return addEntry;
	}
	
	@Override
	public AuthorizationCheck<EditEntryRequestPacket, Void> editEntry() {
		return editEntry;
	}
	
	@Override
	public AuthorizationCheck<RemoveEntryRequestPacket, Void> removeEntry() {
		return removeEntry;
	}
	
	@Override
	public AuthorizationCheck<GetEntryRequestPacket, Optional<Entry>> getEntry() {
		return getEntry;
	}
	
	@Override
	public AuthorizationCheck<GetEntryNBTRequestPacket, Optional<byte[]>> getEntryNBT() {
		return getEntryNBT;
	}
	
	@Override
	public AuthorizationCheck<GetEntriesRequestPacket, List<Entry>> getEntries() {
		return getEntries;
	}
	
	@Override
	public AuthorizationCheck<LockTagRequestPacket, Void> lockTag() {
		return lockTag;
	}
	
	@Override
	public AuthorizationCheck<UnlockTagRequestPacket, Void> unlockTag() {
		return unlockTag;
	}
	
	@Override
	public AuthorizationCheck<AddTagRequestPacket, Void> addTag() {
		return addTag;
	}
	
	@Override
	public AuthorizationCheck<EditTagRequestPacket, Void> editTag() {
		return editTag;
	}
	
	@Override
	public AuthorizationCheck<RemoveTagRequestPacket, Void> removeTag() {
		return removeTag;
	}
	
	@Override
	public AuthorizationCheck<GetTagRequestPacket, Optional<Tag>> getTag() {
		return getTag;
	}
	
	@Override
	public AuthorizationCheck<GetTagsRequestPacket, List<Tag>> getTags() {
		return getTags;
	}
	
	@Override
	public AuthorizationCheck<AddTagToEntryRequestPacket, Void> addTagToEntry() {
		return addTagToEntry;
	}
	
	@Override
	public AuthorizationCheck<RemoveTagFromEntryRequestPacket, Void> removeTagFromEntry() {
		return removeTagFromEntry;
	}
	
}
