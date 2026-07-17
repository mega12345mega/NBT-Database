package com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.permission;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.luneruniverse.minecraft.nbtdatabase.Config;
import com.luneruniverse.minecraft.nbtdatabase.Entry;
import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.connection.access.NBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.AuthorizationServerException;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;
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
import com.luneruniverse.minecraft.nbtdatabase.connection.server.Lock;
import com.luneruniverse.minecraft.nbtdatabase.connection.server.LockCacheMap;
import com.luneruniverse.minecraft.nbtdatabase.connection.server.ServerLock;
import com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.AuthorizationCheck;
import com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.AuthorizationManager;
import com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.CachedAuthorizationManager;
import com.luneruniverse.minecraft.nbtdatabase.connection.user.LoggedInUser;
import com.luneruniverse.minecraft.nbtdatabase.connection.user.User;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.FutureUtil;

public class PermissionAuthorizationManager implements AuthorizationManager {
	
	public static AuthorizationManager create(PermissionManager perms) {
		return new CachedAuthorizationManager(new PermissionAuthorizationManager(perms));
	}
	
	private final PermissionManager perms;
	
	private PermissionAuthorizationManager(PermissionManager perms) {
		this.perms = perms;
	}
	
	private boolean hasPermission(User user, String permission) {
		if (perms.hasPermission(user, permission))
			return true;
		if (permission.contains("/self/") && perms.hasPermission(user, permission.replace("/self/", "/anyone/")))
			return true;
		return false;
	}
	
	private void hasPermissionOrThrow(User user, String permission) throws AuthorizationServerException {
		if (!hasPermission(user, permission))
			throw new AuthorizationServerException("Missing permission: " + permission);
	}
	
	private <I extends Packet, O> AuthorizationCheck<I, O> permissionCheck(String permission) {
		return new AuthorizationCheck<I, O>() {
			@Override
			public I checkRequest(User user, I request) throws AuthorizationServerException {
				hasPermissionOrThrow(user, permission);
				return request;
			}
		};
	}
	
	private <I extends Packet, O> AuthorizationCheck<I, O> entryAuthorCheck(String selfPermission, boolean verifyRequired, Function<I, Long> getId) {
		return new AuthorizationCheck<I, O>() {
			@Override
			public I checkRequest(User user, I request) throws AuthorizationServerException {
				hasPermissionOrThrow(user, selfPermission);
				return request;
			}
			@Override
			public ServerLock getLock(Lock configLock, LockCacheMap<Long> entryLocks, LockCacheMap<String> tagLocks, User user, I request) {
				return entryLocks.getServerLock(getId.apply(request));
			}
			@Override
			public CompletableFuture<I> checkRequestDuringLock(NBTDatabaseAccess access, User user, I request) {
				return FutureUtil.thenApply(access.getEntry(getId.apply(request)), entry -> {
					boolean isAuthor = user.hasUuid(entry.getAuthorUuid());
					if (!isAuthor)
						hasPermissionOrThrow(user, selfPermission.replace("/self/", "/anyone/"));
					if (verifyRequired && entry.isVerified()) {
						hasPermissionOrThrow(user, isAuthor
								? Permissions.ENTRY_VERIFIED_SELF_EDIT_VERIFY : Permissions.ENTRY_VERIFIED_ANYONE_EDIT_VERIFY);
					}
					return request;
				});
			}
		};
	}
	
	@Override
	public void connect(User user) throws AuthorizationServerException {
		hasPermissionOrThrow(user, Permissions.CONNECT);
	}
	
	@Override
	public AuthorizationCheck<LockConfigRequestPacket, Void> lockConfig() {
		return permissionCheck(Permissions.CONFIG_LOCK);
	}
	
	@Override
	public AuthorizationCheck<UnlockConfigRequestPacket, Void> unlockConfig() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<SetConfigRequestPacket, Void> setConfig() {
		return permissionCheck(Permissions.CONFIG_EDIT);
	}
	
	@Override
	public AuthorizationCheck<GetConfigRequestPacket, Config> getConfig() {
		return permissionCheck(Permissions.CONFIG_LIST);
	}
	
	@Override
	public AuthorizationCheck<LockEntryRequestPacket, Void> lockEntry() {
		return permissionCheck(Permissions.ENTRY_LOCK);
	}
	
	@Override
	public AuthorizationCheck<UnlockEntryRequestPacket, Void> unlockEntry() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<AddEntryRequestPacket, Long> addEntry() {
		return new AuthorizationCheck<AddEntryRequestPacket, Long>() {
			@Override
			public AddEntryRequestPacket checkRequest(User user, AddEntryRequestPacket request) throws AuthorizationServerException {
				if (user.hasUuid(request.getAuthorUuid())) {
					hasPermissionOrThrow(user, Permissions.ENTRY_SELF_ADD);
					hasPermissionOrThrow(user, request.isVerified()
							? Permissions.ENTRY_VERIFIED_SELF_ADD_VERIFY : Permissions.ENTRY_VERIFIED_SELF_ADD_UNVERIFY);
				} else {
					hasPermissionOrThrow(user, Permissions.ENTRY_ANYONE_ADD);
					hasPermissionOrThrow(user, request.isVerified()
							? Permissions.ENTRY_VERIFIED_ANYONE_ADD_VERIFY : Permissions.ENTRY_VERIFIED_ANYONE_ADD_UNVERIFY);
				}
				return request;
			}
		};
	}
	
	@Override
	public AuthorizationCheck<EditEntryRequestPacket, Void> editEntry() {
		return new AuthorizationCheck<EditEntryRequestPacket, Void>() {
			@Override
			public EditEntryRequestPacket checkRequest(User user, EditEntryRequestPacket request) throws AuthorizationServerException {
				if (request.getName().isPresent())
					hasPermissionOrThrow(user, Permissions.ENTRY_SELF_EDIT_NAME);
				if (request.getNbt().isPresent())
					hasPermissionOrThrow(user, Permissions.ENTRY_SELF_EDIT_NBT);
				if (request.getType().isPresent())
					hasPermissionOrThrow(user, Permissions.ENTRY_SELF_EDIT_TYPE);
				if (request.getDataVersion().isPresent())
					hasPermissionOrThrow(user, Permissions.ENTRY_SELF_EDIT_DATA_VERSION);
				if (request.getAuthorUuid().isPresent())
					hasPermissionOrThrow(user, Permissions.ENTRY_AUTHOR_SELF_UUID);
				if (request.getAuthorUsername().isPresent())
					hasPermissionOrThrow(user, Permissions.ENTRY_AUTHOR_SELF_USERNAME);
				if (request.isVerified().isPresent()) {
					hasPermissionOrThrow(user, request.isVerified().get()
							? Permissions.ENTRY_VERIFIED_SELF_EDIT_VERIFY : Permissions.ENTRY_VERIFIED_SELF_EDIT_UNVERIFY);
				}
				return request;
			}
			@Override
			public ServerLock getLock(Lock configLock, LockCacheMap<Long> entryLocks, LockCacheMap<String> tagLocks, User user, EditEntryRequestPacket request) {
				return entryLocks.getServerLock(request.getId());
			}
			@Override
			public CompletableFuture<EditEntryRequestPacket> checkRequestDuringLock(NBTDatabaseAccess access, User user, EditEntryRequestPacket request) {
				return FutureUtil.thenApply(access.getEntry(request.getId()), entry -> {
					boolean fromSelf = user.hasUuid(entry.getAuthorUuid());
					boolean toSelf = user.hasUuid(request.getAuthorUuid().orElse(entry.getAuthorUuid()));
					
					if (request.getAuthorUuid().isPresent()) {
						boolean toVerified = request.isVerified().orElse(entry.isVerified());
						
						if (fromSelf) {
							if (toVerified) {
								hasPermissionOrThrow(user, toSelf
										? Permissions.ENTRY_VERIFIED_SELF_EDIT_VERIFY : Permissions.ENTRY_VERIFIED_ANYONE_EDIT_VERIFY);
							}
						} else {
							hasPermissionOrThrow(user, Permissions.ENTRY_AUTHOR_ANYONE_UUID);
							
							if (toVerified) {
								if (toSelf) {
									if (entry.isVerified()) {
										if (!(hasPermission(user, Permissions.ENTRY_VERIFIED_ANYONE_EDIT_VERIFY) ||
												hasPermission(user, Permissions.ENTRY_VERIFIED_ANYONE_EDIT_UNVERIFY) &&
												hasPermission(user, Permissions.ENTRY_VERIFIED_SELF_EDIT_VERIFY))) {
											throw new AuthorizationServerException("Missing permission(s): " +
													Permissions.ENTRY_VERIFIED_ANYONE_EDIT_VERIFY + " OR (" +
													Permissions.ENTRY_VERIFIED_ANYONE_EDIT_UNVERIFY + " AND " +
													Permissions.ENTRY_VERIFIED_SELF_EDIT_VERIFY + ")");
										}
									}
								} else {
									hasPermissionOrThrow(user, Permissions.ENTRY_VERIFIED_ANYONE_EDIT_VERIFY);
								}
							} else if (entry.isVerified()) {
								if (toSelf) {
									if (!(hasPermission(user, Permissions.ENTRY_VERIFIED_ANYONE_EDIT_UNVERIFY) ||
											hasPermission(user, Permissions.ENTRY_VERIFIED_ANYONE_EDIT_VERIFY) &&
											hasPermission(user, Permissions.ENTRY_VERIFIED_SELF_EDIT_UNVERIFY))) {
										throw new AuthorizationServerException("Missing permission(s): " +
												Permissions.ENTRY_VERIFIED_ANYONE_EDIT_UNVERIFY + " OR (" +
												Permissions.ENTRY_VERIFIED_ANYONE_EDIT_VERIFY + " AND " +
												Permissions.ENTRY_VERIFIED_SELF_EDIT_UNVERIFY + ")");
									}
								} else {
									hasPermissionOrThrow(user, Permissions.ENTRY_VERIFIED_ANYONE_EDIT_UNVERIFY);
								}
							} else if (request.isVerified().isPresent()) {
								// For consistency with other useless edits
								if (!toSelf)
									hasPermissionOrThrow(user, Permissions.ENTRY_VERIFIED_ANYONE_EDIT_UNVERIFY);
							}
						}
					} else {
						if (!fromSelf) {
							if (request.isVerified().isPresent()) {
								hasPermissionOrThrow(user, request.isVerified().get()
										? Permissions.ENTRY_VERIFIED_ANYONE_EDIT_VERIFY : Permissions.ENTRY_VERIFIED_ANYONE_EDIT_UNVERIFY);
							}
						}
					}
					
					if (!(fromSelf || toSelf)) {
						if (request.getName().isPresent())
							hasPermissionOrThrow(user, Permissions.ENTRY_ANYONE_EDIT_NAME);
						if (request.getNbt().isPresent())
							hasPermissionOrThrow(user, Permissions.ENTRY_ANYONE_EDIT_NBT);
						if (request.getType().isPresent())
							hasPermissionOrThrow(user, Permissions.ENTRY_ANYONE_EDIT_TYPE);
						if (request.getDataVersion().isPresent())
							hasPermissionOrThrow(user, Permissions.ENTRY_ANYONE_EDIT_DATA_VERSION);
						if (request.getAuthorUsername().isPresent())
							hasPermissionOrThrow(user, Permissions.ENTRY_AUTHOR_ANYONE_USERNAME);
					}
					
					return request;
				});
			}
		};
	}
	
	@Override
	public AuthorizationCheck<RemoveEntryRequestPacket, Void> removeEntry() {
		return entryAuthorCheck(Permissions.ENTRY_SELF_REMOVE, true, RemoveEntryRequestPacket::getId);
	}
	
	@Override
	public AuthorizationCheck<GetEntryRequestPacket, Entry> getEntry() {
		return new AuthorizationCheck<GetEntryRequestPacket, Entry>() {
			@Override
			public GetEntryRequestPacket checkRequest(User user, GetEntryRequestPacket request) throws AuthorizationServerException {
				hasPermissionOrThrow(user, Permissions.ENTRY_SELF_GET);
				return request;
			}
			@Override
			public Entry checkResponse(User user, Entry response) throws AuthorizationServerException {
				if (!user.hasUuid(response.getAuthorUuid()))
					hasPermissionOrThrow(user, Permissions.ENTRY_ANYONE_GET);
				return response;
			}
		};
	}
	
	@Override
	public AuthorizationCheck<GetEntryNBTRequestPacket, byte[]> getEntryNBT() {
		return entryAuthorCheck(Permissions.ENTRY_SELF_EXPORT, false, GetEntryNBTRequestPacket::getId);
	}
	
	@Override
	public AuthorizationCheck<GetEntriesRequestPacket, List<Entry>> getEntries() {
		return new AuthorizationCheck<GetEntriesRequestPacket, List<Entry>>() {
			@Override
			public GetEntriesRequestPacket checkRequest(User user, GetEntriesRequestPacket request) throws AuthorizationServerException {
				if (!user.isLoggedIn()) {
					hasPermissionOrThrow(user, Permissions.ENTRY_ANYONE_LIST);
					return request;
				}
				
				UUID userUuid = ((LoggedInUser) user).getUuid();
				UUID requestAuthorUuid = request.getFilter().getAuthorUuid();
				
				if (requestAuthorUuid == null) {
					hasPermissionOrThrow(user, Permissions.ENTRY_SELF_LIST);
					if (hasPermission(user, Permissions.ENTRY_ANYONE_LIST))
						return request;
					return new GetEntriesRequestPacket(request.getFilter().filterByAuthorUuid(userUuid), request.getView());
				}
				
				hasPermissionOrThrow(user,
						requestAuthorUuid.equals(userUuid) ? Permissions.ENTRY_SELF_LIST : Permissions.ENTRY_ANYONE_LIST);
				return request;
			}
		};
	}
	
	@Override
	public AuthorizationCheck<LockTagRequestPacket, Void> lockTag() {
		return permissionCheck(Permissions.TAG_LOCK);
	}
	
	@Override
	public AuthorizationCheck<UnlockTagRequestPacket, Void> unlockTag() {
		return AuthorizationCheck.allow();
	}
	
	@Override
	public AuthorizationCheck<AddTagRequestPacket, Void> addTag() {
		return permissionCheck(Permissions.TAG_ADD);
	}
	
	@Override
	public AuthorizationCheck<EditTagRequestPacket, Void> editTag() {
		return new AuthorizationCheck<EditTagRequestPacket, Void>() {
			@Override
			public EditTagRequestPacket checkRequest(User user, EditTagRequestPacket request) throws AuthorizationServerException {
				if (request.getName().isPresent())
					hasPermissionOrThrow(user, Permissions.TAG_EDIT_NAME);
				if (request.getColor().isPresent())
					hasPermissionOrThrow(user, Permissions.TAG_EDIT_COLOR);
				return request;
			}
		};
	}
	
	@Override
	public AuthorizationCheck<RemoveTagRequestPacket, Void> removeTag() {
		return permissionCheck(Permissions.TAG_REMOVE);
	}
	
	@Override
	public AuthorizationCheck<GetTagRequestPacket, Tag> getTag() {
		return permissionCheck(Permissions.TAG_GET);
	}
	
	@Override
	public AuthorizationCheck<GetTagsRequestPacket, List<Tag>> getTags() {
		return new AuthorizationCheck<GetTagsRequestPacket, List<Tag>>() {
			@Override
			public GetTagsRequestPacket checkRequest(User user, GetTagsRequestPacket request) throws AuthorizationServerException {
				hasPermissionOrThrow(user, Permissions.TAG_LIST);
				if (request.getFilter().getEntryId() != null)
					hasPermissionOrThrow(user, Permissions.TAG_SELF_FILTER);
				return request;
			}
			@Override
			public ServerLock getLock(Lock configLock, LockCacheMap<Long> entryLocks, LockCacheMap<String> tagLocks, User user, GetTagsRequestPacket request) {
				Long entryId = request.getFilter().getEntryId();
				if (entryId == null)
					return null;
				return entryLocks.getServerLock(entryId);
			}
			@Override
			public CompletableFuture<GetTagsRequestPacket> checkRequestDuringLock(NBTDatabaseAccess access, User user, GetTagsRequestPacket request) {
				Long entryId = request.getFilter().getEntryId();
				if (entryId == null)
					return null;
				return FutureUtil.thenApply(access.getEntry(entryId), entry -> {
					if (!user.hasUuid(entry.getAuthorUuid()))
						hasPermissionOrThrow(user, Permissions.TAG_ANYONE_FILTER);
					return request;
				});
			}
		};
	}
	
	@Override
	public AuthorizationCheck<AddTagToEntryRequestPacket, Void> addTagToEntry() {
		return entryAuthorCheck(Permissions.TAG_SELF_ATTACH, true, AddTagToEntryRequestPacket::getEntry);
	}
	
	@Override
	public AuthorizationCheck<RemoveTagFromEntryRequestPacket, Void> removeTagFromEntry() {
		return entryAuthorCheck(Permissions.TAG_SELF_DETACH, true, RemoveTagFromEntryRequestPacket::getEntry);
	}
	
}
