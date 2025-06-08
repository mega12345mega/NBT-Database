package com.luneruniverse.minecraft.nbtdatabase.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.luneruniverse.minecraft.nbtdatabase.DataVersion;
import com.luneruniverse.minecraft.nbtdatabase.EntryFilter;
import com.luneruniverse.minecraft.nbtdatabase.NBTDatabase;
import com.luneruniverse.minecraft.nbtdatabase.NBTEntry;
import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.TagFilter;
import com.luneruniverse.minecraft.nbtdatabase.connection.LocalNBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.NBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.NBTDatabaseAccessServer;
import com.luneruniverse.minecraft.nbtdatabase.connection.RemoteNBTDatabaseAccess;
import com.luneruniverse.simplecli.CommandParseException;
import com.luneruniverse.simplecli.CommandStream;
import com.luneruniverse.simplecli.CommandSyntaxException;
import com.luneruniverse.simplecli.commands.GroupCommand;
import com.luneruniverse.simplecli.commands.SingleCommand;
import com.luneruniverse.simplecli.inputs.IntegerInput;
import com.luneruniverse.simplecli.inputs.StringInput;

public class CLI extends Thread {
	
	private final GroupCommand root;
	private boolean exit;
	private NBTDatabase localDatabase;
	private NBTDatabaseAccess connection;
	private NBTDatabaseAccessServer server;
	private List<NBTEntry> result;
	
	public CLI() {
		DataVersion.loadVersions();
		
		root = new GroupCommand(null);
		
		root.addCommand(new SingleCommand("exit", () -> exit = true));
		
		root.addCommand(new SingleCommand("create", inputs -> createCmd(
				new File(inputs.getArgument("file", String.class)), inputs.hasFlag("overwrite")))
				.addArgument("file", new StringInput()).addFlag("overwrite", "o"));
		
		root.addCommand(new GroupCommand("open")
				.addCommand(new SingleCommand("local", inputs -> openLocalCmd(
						new File(inputs.getArgument("file", String.class))))
						.addArgument("file", new StringInput()))
				.addCommand(new SingleCommand("remote", inputs -> openRemoteCmd(
						inputs.getArgument("ip", String.class), inputs.getArgument("port", Integer.class)))
						.addArgument("ip", new StringInput()).addArgument("port", new IntegerInput())));
		
		root.addCommand(new SingleCommand("close", this::closeCmd));
		
		root.addCommand(new GroupCommand("server")
				.addCommand(new SingleCommand("start", inputs -> serverStartCmd(
						inputs.getArgument("port", Integer.class)))
						.addArgument("port", new IntegerInput()))
				.addCommand(new SingleCommand("stop", this::serverStopCmd)));
		
		root.addCommand(new SingleCommand("metadata", this::metadataCmd));
		
		root.addCommand(new GroupCommand("entry")
				.addCommand(new SingleCommand("add", inputs -> entryAddCmd(
						inputs.getArgument("name", String.class), new File(inputs.getArgument("file", String.class)), inputs.getArgument("data_version", Integer.class), inputs.getArgument("author_uuid", UUID.class), inputs.getArgument("author_username", String.class), !inputs.hasFlag("unverified")))
						.addArgument("name", new StringInput()).addArgument("file", new StringInput()).addArgument("data_version", new DataVersionInput()).addArgument("author_uuid", new UUIDInput()).addArgument("author_username", new StringInput()).addFlag("unverified", "uv"))
				.addCommand(new SingleCommand("remove", inputs -> entryRemoveCmd(
						inputs.getArgument("id", Long.class)))
						.addArgument("id", new LongInput()))
				.addCommand(new SingleCommand("get", inputs -> entryGetCmd(
						inputs.getArgument("id", Long.class), inputs.hasFlag("verbose")))
						.addArgument("id", new LongInput()).addFlag("verbose", "v"))
				.addCommand(new SingleCommand("list",
						inputs -> {
							EntryFilter filter = new EntryFilter();
							if (inputs.hasFlag("name"))
								filter.filterByName(inputs.getFlag("name", String.class));
							if (inputs.hasFlag("data_version"))
								filter.filterByDataVersion(inputs.getFlag("data_version", Integer.class));
							else {
								if (inputs.hasFlag("data_version_min"))
									filter.filterByMinDataVersion(inputs.getFlag("data_version_min", Integer.class));
								if (inputs.hasFlag("data_version_max"))
									filter.filterByMaxDataVersion(inputs.getFlag("data_version_max", Integer.class));
							}
							if (inputs.hasFlag("author_uuid"))
								filter.filterByAuthorUuid(inputs.getFlag("author_uuid", UUID.class));
							if (inputs.hasFlag("author_name"))
								filter.filterByAuthorName(inputs.getFlag("author_name", String.class));
							if (inputs.hasFlag("tags"))
								filter.filterByTags(new HashSet<>(Arrays.asList(inputs.getFlag("tags", String.class).split(","))));
							entryListCmd(filter, inputs.hasFlag("verbose"));
						})
						.addFlag("name", "n", new StringInput()).addFlag("data_version", "d", new DataVersionInput()).addFlag("data_version_min", "dmin", new DataVersionInput()).addFlag("data_version_max", "dmax", new DataVersionInput()).addFlag("author_uuid", "au", new UUIDInput()).addFlag("author_name", "an", new StringInput()).addFlag("tags", "t", new StringInput()).addFlag("verbose", "v")));
		
		root.addCommand(new GroupCommand("tag")
				.addCommand(new SingleCommand("add", inputs -> tagAddCmd(
						inputs.getArgument("name", String.class), inputs.getArgument("color", Integer.class)))
						.addArgument("name", new StringInput()).addArgument("color", new ColorInput()))
				.addCommand(new SingleCommand("remove", inputs -> tagRemoveCmd(
						inputs.getArgument("name", String.class)))
						.addArgument("name", new StringInput()))
				.addCommand(new SingleCommand("list",
						inputs -> {
							TagFilter filter = new TagFilter();
							if (inputs.hasFlag("name"))
								filter.filterByName(inputs.getFlag("name", String.class));
							if (inputs.hasFlag("entry_id"))
								filter.filterByEntryId(inputs.getFlag("entry_id", Long.class));
							tagListCmd(filter);
						})
						.addFlag("name", "n", new StringInput()).addFlag("entry_id", "e", new LongInput()))
				.addCommand(new SingleCommand("attach", inputs -> tagAttachCmd(
						inputs.getArgument("entry", Long.class), inputs.getArgument("tag", String.class)))
						.addArgument("entry", new LongInput()).addArgument("tag", new StringInput()))
				.addCommand(new SingleCommand("detach", inputs -> tagDetachCmd(
						inputs.getArgument("entry", Long.class), inputs.getArgument("tag", String.class)))
						.addArgument("entry", new LongInput()).addArgument("tag", new StringInput())));
		
		root.addCommand(new GroupCommand("result")
				.addCommand(new SingleCommand("export", inputs -> resultExportCmd(
						inputs.getArgument("index", Integer.class), new File(inputs.getArgument("file", String.class)), inputs.hasFlag("overwrite")))
						.addArgument("index", new IntegerInput().min(0)).addArgument("file", new StringInput()).addFlag("overwrite", "o"))
				.addCommand(new SingleCommand("remove", inputs -> resultRemoveCmd(
						inputs.getArgument("index", Integer.class)))
						.addArgument("index", new IntegerInput().min(0)))
				.addCommand(new SingleCommand("list", inputs -> resultListCmd(
						inputs.hasFlag("verbose")))
						.addFlag("verbose", "v"))
				.addCommand(new SingleCommand("tags",
						inputs -> {
							TagFilter filter = new TagFilter();
							if (inputs.hasFlag("name"))
								filter.filterByName(inputs.getFlag("name", String.class));
							resultTagsCmd(inputs.getArgument("index", Integer.class), filter);
						})
						.addArgument("index", new IntegerInput().min(0)).addFlag("name", "n", new StringInput()))
				.addCommand(new SingleCommand("attach", inputs -> resultAttachCmd(
						inputs.getArgument("index", Integer.class), inputs.getArgument("tag", String.class)))
						.addArgument("index", new IntegerInput().min(0)).addArgument("tag", new StringInput()))
				.addCommand(new SingleCommand("detach", inputs -> resultDetachCmd(
						inputs.getArgument("index", Integer.class), inputs.getArgument("tag", String.class)))
						.addArgument("index", new IntegerInput().min(0)).addArgument("tag", new StringInput())));
	}
	
	private void closeConnection(boolean silent) {
		closeServer(silent);
		
		if (connection != null) {
			connection.closeAsync().whenComplete((v, e) -> {
				if (e == null) {
					if (!silent)
						System.out.println("Closed connection");
				} else
					e.printStackTrace();
			});
			connection = null;
		}
		
		if (localDatabase != null) {
			try {
				localDatabase.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			localDatabase = null;
			if (!silent)
				System.out.println("Closed database");
		}
	}
	
	private void closeServer(boolean silent) {
		if (server != null) {
			server.closeAsync().whenComplete((v, e) -> {
				if (e == null) {
					if (!silent)
						System.out.println("Closed server");
				} else
					e.printStackTrace();
			});
			server = null;
		}
	}
	
	private void createCmd(File file, boolean overwrite) {
		if (file.exists()) {
			if (!overwrite) {
				System.err.println("File already exists: " + file.getAbsolutePath());
				return;
			}
			
			if (!file.isFile()) {
				System.err.println("Can only overwrite a file: " + file.getAbsolutePath());
				return;
			}
			
			file.delete();
		}
		file.getAbsoluteFile().getParentFile().mkdirs();
		
		closeConnection(true);
		
		try {
			localDatabase = new NBTDatabase(file);
			connection = new LocalNBTDatabaseAccess(localDatabase);
			System.out.println("Created and opened: " + file.getAbsolutePath());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void openLocalCmd(File file) {
		if (!file.exists()) {
			System.err.println("File doesn't exist: " + file.getAbsolutePath());
			return;
		}
		
		if (!file.isFile()) {
			System.err.println("Not a file: " + file.getAbsolutePath());
			return;
		}
		
		closeConnection(true);
		
		try {
			localDatabase = new NBTDatabase(file);
			connection = new LocalNBTDatabaseAccess(localDatabase);
			System.out.println("Opened: " + file.getAbsolutePath());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void openRemoteCmd(String ip, int port) {
		closeConnection(true);
		
		try {
			connection = new RemoteNBTDatabaseAccess(ip, port);
			System.out.println("Opened: " + ip + ":" + port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void closeCmd() {
		if (connection == null) {
			System.err.println("There is not an open connection");
			return;
		}
		
		closeConnection(false);
	}
	
	private void serverStartCmd(int port) {
		if (connection == null) {
			System.err.println("There is not an open connection");
			return;
		}
		
		closeServer(true);
		
		try {
			server = new NBTDatabaseAccessServer(connection, port);
			System.out.println("Started server on port " + port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void serverStopCmd() {
		if (server == null) {
			System.err.println("There is not a running server");
			return;
		}
		
		closeServer(false);
	}
	
	private void metadataCmd() {
		if (connection == null) {
			System.err.println("There is not an open connection");
			return;
		}
		
		connection.getMetadata().whenComplete((metadata, e) -> {
			if (e != null)
				e.printStackTrace();
			else {
				System.out.println("max_nbt_size: " + metadata.getMaxNbtSize());
				System.out.println("max_num_results: " + metadata.getMaxNumResults());
			}
		});
	}
	
	private void entryAddCmd(String name, File file, int dataVersion, UUID authorUuid, String authorUsername, boolean verified) {
		if (connection == null) {
			System.err.println("There is not an open connection");
			return;
		}
		
		if (!file.exists()) {
			System.err.println("File doesn't exist: " + file.getAbsolutePath());
			return;
		}
		
		if (!file.isFile()) {
			System.err.println("Not a file: " + file.getAbsolutePath());
			return;
		}
		
		try {
			connection.addEntry(name, Files.readAllBytes(file.toPath()), dataVersion, authorUuid, authorUsername, verified).whenComplete((id, e) -> {
				if (e != null)
					e.printStackTrace();
				else
					System.out.println("Added entry '" + name + "' with id " + id);
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void entryRemoveCmd(long id) {
		if (connection == null) {
			System.err.println("There is not an open connection");
			return;
		}
		
		connection.removeEntry(id).whenComplete((v, e) -> {
			if (e != null)
				e.printStackTrace();
			else
				System.out.println("Removed entry with id " + id);
		});
	}
	
	private void entryGetCmd(long id, boolean verbose) {
		if (connection == null) {
			System.err.println("There is not an open connection");
			return;
		}
		
		connection.getEntry(id).whenComplete((entry, e) -> {
			if (e != null)
				e.printStackTrace();
			else if (entry == null)
				System.err.println("Entry doesn't exist: " + id);
			else {
				result = Arrays.asList(entry);
				resultListCmd(verbose);
			}
		});
	}
	
	private void entryListCmd(EntryFilter filter, boolean verbose) {
		if (connection == null) {
			System.err.println("There is not an open connection");
			return;
		}
		
		connection.getEntries(filter).whenComplete((entries, e) -> {
			if (e != null)
				e.printStackTrace();
			else {
				result = entries;
				resultListCmd(verbose);
			}
		});
	}
	
	private void tagAddCmd(String name, int color) {
		if (connection == null) {
			System.err.println("There is not an open connection");
			return;
		}
		
		connection.addTag(name, color).whenComplete((v, e) -> {
			if (e != null)
				e.printStackTrace();
			else
				System.out.println("Added tag: " + name + " (#" + ColorInput.toString(color) + ")");
		});
	}
	
	private void tagRemoveCmd(String name) {
		if (connection == null) {
			System.err.println("There is not an open connection");
			return;
		}
		
		connection.removeTag(name).whenComplete((v, e) -> {
			if (e != null)
				e.printStackTrace();
			else
				System.out.println("Removed tag: " + name);
		});
	}
	
	private void tagListCmd(TagFilter filter) {
		if (connection == null) {
			System.err.println("There is not an open connection");
			return;
		}
		
		connection.getTags(filter).whenComplete((tags, e) -> {
			if (e != null)
				e.printStackTrace();
			else if (tags.isEmpty())
				System.out.println("There are no tags");
			else {
				for (Tag tag : tags)
					System.out.println(tag.name + " (#" + ColorInput.toString(tag.color) + ")");
			}
		});
	}
	
	private void tagAttachCmd(long entry, String tag) {
		if (connection == null) {
			System.err.println("There is not an open connection");
			return;
		}
		
		connection.addTagToEntry(entry, tag).whenComplete((v, e) -> {
			if (e != null)
				e.printStackTrace();
			else
				System.out.println("Attached tag '" + tag + "' to entry with id " + entry);
		});
	}
	
	private void tagDetachCmd(long entry, String tag) {
		if (connection == null) {
			System.err.println("There is not an open connection");
			return;
		}
		
		connection.removeTagFromEntry(entry, tag).whenComplete((v, e) -> {
			if (e != null)
				e.printStackTrace();
			else
				System.out.println("Detached tag '" + tag + "' from entry with id " + entry);
		});
	}
	
	private void resultExportCmd(int index, File file, boolean overwrite) {
		if (result == null) {
			System.err.println("There are no saved results");
			return;
		}
		
		if (index >= result.size()) {
			System.err.println("Invalid index: " + index);
			return;
		}
		
		if (file.exists()) {
			if (!overwrite) {
				System.err.println("File already exists: " + file.getAbsolutePath());
				return;
			}
			
			if (!file.isFile()) {
				System.err.println("Can only overwrite a file: " + file.getAbsolutePath());
				return;
			}
			
			file.delete();
		}
		file.getAbsoluteFile().getParentFile().mkdirs();
		
		NBTEntry entry = result.get(index);
		try {
			Files.write(file.toPath(), entry.nbt);
			System.out.println("Exported " + entry.id + " to: " + file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void resultRemoveCmd(int index) {
		if (connection == null) {
			System.err.println("There is not an open connection");
			return;
		}
		
		if (result == null) {
			System.err.println("There are no saved results");
			return;
		}
		
		if (index >= result.size()) {
			System.err.println("Invalid index: " + index);
			return;
		}
		
		long id = result.get(index).id;
		connection.removeEntry(id).whenComplete((v, e) -> {
			if (e != null)
				e.printStackTrace();
			else
				System.out.println("Removed entry with id " + id);
		});
	}
	
	private void resultListCmd(boolean verbose) {
		if (result == null) {
			System.err.println("There are no saved results");
			return;
		}
		
		if (result.isEmpty()) {
			System.out.println("There are no entries");
			return;
		}
		
		for (int i = 0; i < result.size(); i++) {
			NBTEntry entry = result.get(i);
			
			if (i != 0)
				System.out.println();
			
			System.out.println("#" + i + ": " + entry.id + ": " + entry.name);
			System.out.println("  Author: " + entry.authorUsername + " (" + entry.authorUuid + ")");
			System.out.println("  Data Version: " + DataVersion.toString(entry.dataVersion));
			if (verbose) {
				System.out.println("  Bytes: " + entry.nbt.length);
				System.out.println("  Created: " + entry.created); // TODO format
				System.out.println("  Modified: " + entry.modified);
				System.out.println("  Hash: " + entry.hash);
				System.out.println("  Verified: " + entry.verified);
			}
		}
	}
	
	private void resultTagsCmd(int index, TagFilter filter) {
		if (connection == null) {
			System.err.println("There is not an open connection");
			return;
		}
		
		if (result == null) {
			System.err.println("There are no saved results");
			return;
		}
		
		if (index >= result.size()) {
			System.err.println("Invalid index: " + index);
			return;
		}
		
		filter.filterByEntryId(result.get(index).id);
		connection.getTags(filter).whenComplete((tags, e) -> {
			if (e != null)
				e.printStackTrace();
			else if (tags.isEmpty())
				System.out.println("There are no tags");
			else {
				for (Tag tag : tags)
					System.out.println(tag.name + " (#" + ColorInput.toString(tag.color) + ")");
			}
		});
	}
	
	private void resultAttachCmd(int index, String tag) {
		if (connection == null) {
			System.err.println("There is not an open connection");
			return;
		}
		
		if (result == null) {
			System.err.println("There are no saved results");
			return;
		}
		
		if (index >= result.size()) {
			System.err.println("Invalid index: " + index);
			return;
		}
		
		long id = result.get(index).id;
		connection.addTagToEntry(id, tag).whenComplete((v, e) -> {
			if (e != null)
				e.printStackTrace();
			else
				System.out.println("Attached tag '" + tag + "' to entry with id " + id);
		});
	}
	
	private void resultDetachCmd(int index, String tag) {
		if (connection == null) {
			System.err.println("There is not an open connection");
			return;
		}
		
		if (result == null) {
			System.err.println("There are no saved results");
			return;
		}
		
		if (index >= result.size()) {
			System.err.println("Invalid index: " + index);
			return;
		}
		
		long id = result.get(index).id;
		connection.removeTagFromEntry(id, tag).whenComplete((v, e) -> {
			if (e != null)
				e.printStackTrace();
			else
				System.out.println("Detached tag '" + tag + "' from entry with id " + id);
		});
	}
	
	@Override
	public void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String line;
			while (!exit && (line = in.readLine()) != null) {
				try {
					root.parse(CommandStream.parse(line));
				} catch (CommandParseException | CommandSyntaxException e) {
					System.err.println(e.getMessage());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			closeConnection(false);
		}
	}
	
}
