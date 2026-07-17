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

public class AllowAuthorizationManager implements AuthorizationManager {
	
	public static AuthorizationManager create() {
		return new CachedAuthorizationManager(new AllowAuthorizationManager());
	}
	
	private AllowAuthorizationManager() {}
	
	@Override
	public void connect(User user) throws AuthorizationServerException {}
	
	@Override
	public AuthorizationCheck<LockConfigRequestPacket, Void> lockConfig() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<UnlockConfigRequestPacket, Void> unlockConfig() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<SetConfigRequestPacket, Void> setConfig() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<GetConfigRequestPacket, Config> getConfig() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<LockEntryRequestPacket, Void> lockEntry() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<UnlockEntryRequestPacket, Void> unlockEntry() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<AddEntryRequestPacket, Long> addEntry() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<EditEntryRequestPacket, Void> editEntry() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<RemoveEntryRequestPacket, Void> removeEntry() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<GetEntryRequestPacket, Entry> getEntry() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<GetEntryNBTRequestPacket, byte[]> getEntryNBT() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<GetEntriesRequestPacket, List<Entry>> getEntries() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<LockTagRequestPacket, Void> lockTag() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<UnlockTagRequestPacket, Void> unlockTag() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<AddTagRequestPacket, Void> addTag() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<EditTagRequestPacket, Void> editTag() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<RemoveTagRequestPacket, Void> removeTag() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<GetTagRequestPacket, Tag> getTag() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<GetTagsRequestPacket, List<Tag>> getTags() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<AddTagToEntryRequestPacket, Void> addTagToEntry() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<RemoveTagFromEntryRequestPacket, Void> removeTagFromEntry() {
		return AuthorizationCheck.allow();
	}
	
}
