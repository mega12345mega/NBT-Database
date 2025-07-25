package com.luneruniverse.minecraft.nbtdatabase.ui.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.luneruniverse.minecraft.nbtdatabase.Config;
import com.luneruniverse.minecraft.nbtdatabase.DataVersion;
import com.luneruniverse.minecraft.nbtdatabase.Entry;
import com.luneruniverse.minecraft.nbtdatabase.NBTDatabase;
import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.connection.NBTDatabaseAccessServer;
import com.luneruniverse.minecraft.nbtdatabase.connection.RequestFailedException;
import com.luneruniverse.minecraft.nbtdatabase.connection.ServerException;
import com.luneruniverse.minecraft.nbtdatabase.connection.access.LocalNBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.access.NBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.access.RemoteNBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.request.EntryFilter;
import com.luneruniverse.minecraft.nbtdatabase.request.EntryView;
import com.luneruniverse.minecraft.nbtdatabase.request.IllegalRequestException;
import com.luneruniverse.minecraft.nbtdatabase.request.TagFilter;
import com.luneruniverse.minecraft.nbtdatabase.ui.ColorInput;
import com.luneruniverse.minecraft.nbtdatabase.ui.DataVersionInput;
import com.luneruniverse.minecraft.nbtdatabase.ui.UIUtil;
import com.luneruniverse.minecraft.nbtdatabase.ui.UUIDInput;
import com.luneruniverse.simplecli.CommandParseException;
import com.luneruniverse.simplecli.CommandStream;
import com.luneruniverse.simplecli.CommandSyntaxException;
import com.luneruniverse.simplecli.commands.GroupCommand;
import com.luneruniverse.simplecli.commands.SingleCommand;
import com.luneruniverse.simplecli.inputs.BooleanInput;
import com.luneruniverse.simplecli.inputs.IntegerInput;
import com.luneruniverse.simplecli.inputs.StringInput;
import com.luneruniverse.simplecli.inputs.StringKeyInput;

public class CLI extends Thread {
	
	public static void main(String[] args) {
		CLI cli = new CLI();
		for (String arg : args)
			cli.exec(arg);
		cli.start();
	}
	
	private final GroupCommand root;
	private boolean exit;
	private NBTDatabase localDatabase;
	private NBTDatabaseAccess connection;
	private NBTDatabaseAccessServer server;
	private List<Entry> results;
	
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
		
		root.addCommand(new GroupCommand("config")
				.addCommand(new SingleCommand("list", this::configListCmd))
				.addCommand(new SingleCommand("max_nbt_size", inputs -> configEditCmd(
						config -> config.setMaxNbtSize(inputs.getArgument("value", Integer.class))))
						.addArgument("value", new IntegerInput().min(0)))
				.addCommand(new SingleCommand("max_num_results", inputs -> configEditCmd(
						config -> config.setMaxNumResults(inputs.getArgument("value", Integer.class))))
						.addArgument("value", new IntegerInput().min(0))));
		
		root.addCommand(new GroupCommand("entry")
				.addCommand(new SingleCommand("add", inputs -> entryAddCmd(
						inputs.getArgument("name", String.class),
						new File(inputs.getArgument("file", String.class)),
						inputs.getArgument("type", Entry.Type.class),
						inputs.getArgument("data_version", Integer.class),
						inputs.getArgument("author_uuid", UUID.class),
						inputs.getArgument("author_username", String.class),
						!inputs.hasFlag("unverified")))
						.addArgument("name", new StringInput())
						.addArgument("file", new StringInput())
						.addArgument("type", StringKeyInput.forEnum(Entry.Type.class, true))
						.addArgument("data_version", new DataVersionInput())
						.addArgument("author_uuid", new UUIDInput())
						.addArgument("author_username", new StringInput())
						.addFlag("unverified", "uv"))
				.addCommand(new SingleCommand("edit", inputs -> entryEditCmd(
						inputs.getArgument("id", Long.class),
						inputs.getFlagOptional("name", String.class),
						inputs.getFlagOptional("file", String.class).map(File::new),
						inputs.getFlagOptional("type", Entry.Type.class),
						inputs.getFlagOptional("data_version", Integer.class),
						inputs.getFlagOptional("author_uuid", UUID.class),
						inputs.getFlagOptional("author_username", String.class),
						inputs.getFlagOptional("verified", Boolean.class)))
						.addArgument("id", new EntryIdInput(this::getResults))
						.addFlag("name", "n", new StringInput())
						.addFlag("file", "f", new StringInput())
						.addFlag("type", "t", StringKeyInput.forEnum(Entry.Type.class, true))
						.addFlag("data_version", "d", new DataVersionInput())
						.addFlag("author_uuid", "au", new UUIDInput())
						.addFlag("author_username", "an", new StringInput())
						.addFlag("verified", "v", new BooleanInput()))
				.addCommand(new SingleCommand("remove", inputs -> entryRemoveCmd(
						inputs.getArgument("id", Long.class)))
						.addArgument("id", new EntryIdInput(this::getResults)))
				.addCommand(new SingleCommand("get", inputs -> entryGetCmd(
						inputs.getArgument("id", Long.class), inputs.hasFlag("verbose")))
						.addArgument("id", new EntryIdInput(this::getResults)).addFlag("verbose", "v"))
				.addCommand(new SingleCommand("export", inputs -> entryExportCmd(
						inputs.getArgument("id", Long.class),
						new File(inputs.getArgument("file", String.class)),
						inputs.hasFlag("overwrite")))
						.addArgument("id", new EntryIdInput(this::getResults))
						.addArgument("file", new StringInput())
						.addFlag("overwrite", "o"))
				.addCommand(new SingleCommand("list",
						inputs -> {
							EntryFilter filter = new EntryFilter();
							if (inputs.hasFlag("name"))
								filter.filterByName(inputs.getFlag("name", String.class));
							if (inputs.hasFlag("nbt_length"))
								filter.filterByNbtLength(inputs.getFlag("nbt_length", Integer.class));
							else {
								if (inputs.hasFlag("nbt_length_min"))
									filter.filterByMinNbtLength(inputs.getFlag("nbt_length_min", Integer.class));
								if (inputs.hasFlag("nbt_length_max"))
									filter.filterByMaxNbtLength(inputs.getFlag("nbt_length_max", Integer.class));
							}
							if (inputs.hasFlag("type"))
								filter.filterByType(inputs.getFlag("type", Entry.Type.class));
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
							if (inputs.hasFlag("author_username"))
								filter.filterByAuthorUsername(inputs.getFlag("author_username", String.class));
							if (inputs.hasFlag("tags"))
								filter.filterByTags(new HashSet<>(Arrays.asList(inputs.getFlag("tags", String.class).split(","))));
							EntryView view = new EntryView();
							if (inputs.hasFlag("order"))
								view.setOrder(inputs.getFlag("order", EntryView.Order.class));
							if (inputs.hasFlag("reversed_order"))
								view.setReversedOrder(true);
							if (inputs.hasFlag("offset"))
								view.setOffset(inputs.getFlag("offset", Integer.class));
							entryListCmd(filter, view, inputs.hasFlag("verbose"));
						})
						.addFlag("name", "n", new StringInput())
						.addFlag("nbt_length", "l", new IntegerInput().min(0))
						.addFlag("nbt_length_min", "lmin", new IntegerInput().min(0))
						.addFlag("nbt_length_max", "lmax", new IntegerInput().min(0))
						.addFlag("type", "t", StringKeyInput.forEnum(Entry.Type.class, true))
						.addFlag("data_version", "d", new DataVersionInput())
						.addFlag("data_version_min", "dmin", new DataVersionInput())
						.addFlag("data_version_max", "dmax", new DataVersionInput())
						.addFlag("author_uuid", "au", new UUIDInput())
						.addFlag("author_username", "an", new StringInput())
						.addFlag("tags", "g", new StringInput())
						.addFlag("order", "o", StringKeyInput.forEnum(EntryView.Order.class, true))
						.addFlag("reversed_order", "r")
						.addFlag("offset", "f", new IntegerInput().min(0))
						.addFlag("verbose", "v")));
		
		root.addCommand(new GroupCommand("tag")
				.addCommand(new SingleCommand("add", inputs -> tagAddCmd(
						inputs.getArgument("name", String.class), inputs.getArgument("color", Integer.class)))
						.addArgument("name", new StringInput()).addArgument("color", new ColorInput()))
				.addCommand(new SingleCommand("edit", inputs -> tagEditCmd(
						inputs.getArgument("currentName", String.class),
						inputs.getFlagOptional("name", String.class),
						inputs.getFlagOptional("color", Integer.class)))
						.addArgument("currentName", new StringInput())
						.addFlag("name", "n", new StringInput())
						.addFlag("color", "c", new ColorInput()))
				.addCommand(new SingleCommand("remove", inputs -> tagRemoveCmd(
						inputs.getArgument("name", String.class)))
						.addArgument("name", new StringInput()))
				.addCommand(new SingleCommand("get", inputs -> tagGetCmd(
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
						.addFlag("name", "n", new StringInput()).addFlag("entry_id", "e", new EntryIdInput(this::getResults)))
				.addCommand(new SingleCommand("attach", inputs -> tagAttachCmd(
						inputs.getArgument("entry", Long.class), inputs.getArgument("tag", String.class)))
						.addArgument("entry", new EntryIdInput(this::getResults)).addArgument("tag", new StringInput()))
				.addCommand(new SingleCommand("detach", inputs -> tagDetachCmd(
						inputs.getArgument("entry", Long.class), inputs.getArgument("tag", String.class)))
						.addArgument("entry", new EntryIdInput(this::getResults)).addArgument("tag", new StringInput())));
		
		root.addCommand(new SingleCommand("results", inputs -> resultsCmd(
				inputs.hasFlag("verbose")))
				.addFlag("verbose", "v"));
	}
	
	private <T> void whenComplete(CompletableFuture<T> future, Consumer<T> consumer) {
		future.whenComplete((value, e) -> {
			if (e == null)
				consumer.accept(value);
			else {
				if (e instanceof IllegalRequestException || e instanceof RequestFailedException) {
					if (e instanceof IllegalRequestException)
						System.err.println("[Database] " + e.getMessage());
					else if (e instanceof ServerException)
						System.err.println("[Server] " + e.getMessage());
					else
						System.err.println(e.getMessage());
					if (e.getCause() != null)
						e.getCause().printStackTrace();
				} else
					e.printStackTrace();
			}
		});
	}
	
	private boolean checkFileDoesntExist(File file, boolean overwrite, boolean deleteIfOverwriting) throws IOException {
		if (file.exists()) {
			if (!overwrite) {
				System.err.println("File already exists: " + file.getAbsolutePath());
				return true;
			}
			
			if (!file.isFile()) {
				System.err.println("Can only overwrite a file: " + file.getAbsolutePath());
				return true;
			}
			
			if (deleteIfOverwriting)
				Files.delete(file.toPath());
		}
		
		Files.createDirectories(file.getAbsoluteFile().getParentFile().toPath());
		return false;
	}
	
	private boolean checkFileExists(File file) {
		if (!file.exists()) {
			System.err.println("File doesn't exist: " + file.getAbsolutePath());
			return true;
		}
		
		if (!file.isFile()) {
			System.err.println("Not a file: " + file.getAbsolutePath());
			return true;
		}
		
		return false;
	}
	
	private boolean checkConnectionExists() {
		if (connection == null) {
			System.err.println("There is not an open connection");
			return true;
		}
		
		return false;
	}
	
	private boolean checkServerExists() {
		if (server == null) {
			System.err.println("There is not a running server");
			return true;
		}
		
		return false;
	}
	
	private boolean checkResultsExist() {
		if (results == null) {
			System.err.println("There are no saved results");
			return true;
		}
		
		return false;
	}
	
	private void closeConnection(boolean silent) {
		closeServer(silent);
		
		if (connection != null) {
			whenComplete(connection.closeAsync(), v -> {
				if (!silent)
					System.out.println("Closed connection");
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
			whenComplete(server.closeAsync(), v -> {
				if (!silent)
					System.out.println("Closed server");
			});
			server = null;
		}
	}
	
	private void createCmd(File file, boolean overwrite) {
		try {
			if (checkFileDoesntExist(file, overwrite, true))
				return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		closeConnection(true);
		
		try {
			localDatabase = new NBTDatabase(file);
			connection = new LocalNBTDatabaseAccess(localDatabase);
			System.out.println("Created and opened: " + file.getAbsolutePath());
		} catch (IllegalRequestException e) {
			System.err.println("[Database] " + e.getMessage());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void openLocalCmd(File file) {
		if (checkFileExists(file))
			return;
		
		closeConnection(true);
		
		try {
			localDatabase = new NBTDatabase(file);
			connection = new LocalNBTDatabaseAccess(localDatabase);
			System.out.println("Opened: " + file.getAbsolutePath());
		} catch (IllegalRequestException e) {
			System.err.println("[Database] " + e.getMessage());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void openRemoteCmd(String ip, int port) {
		closeConnection(true);
		
		try {
			connection = new RemoteNBTDatabaseAccess(ip, port);
			System.out.println("Opened: " + ip + ":" + port);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void closeCmd() {
		if (checkConnectionExists())
			return;
		
		closeConnection(false);
	}
	
	private void serverStartCmd(int port) {
		if (checkConnectionExists())
			return;
		
		closeServer(true);
		
		try {
			server = new NBTDatabaseAccessServer(connection, port);
			System.out.println("Started server on port " + port);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void serverStopCmd() {
		if (checkServerExists())
			return;
		
		closeServer(false);
	}
	
	private void configListCmd() {
		if (checkConnectionExists())
			return;
		
		whenComplete(connection.getConfig(), config -> {
			System.out.println("max_nbt_size: " + config.getMaxNbtSize());
			System.out.println("max_num_results: " + config.getMaxNumResults());
		});
	}
	
	private void configEditCmd(Consumer<Config> edit) {
		if (checkConnectionExists())
			return;
		
		whenComplete(connection.getConfig(), config -> {
			edit.accept(config);
			whenComplete(connection.setConfig(config), v -> System.out.println("Edited config"));
		});
	}
	
	private void entryAddCmd(String name, File file, Entry.Type type, int dataVersion, UUID authorUuid, String authorUsername, boolean verified) {
		if (checkConnectionExists() || checkFileExists(file))
			return;
		
		try {
			whenComplete(connection.addEntry(
					name, Files.readAllBytes(file.toPath()), type, dataVersion, authorUuid, authorUsername, verified),
					id -> System.out.println("Added entry '" + name + "' with id " + id));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void entryEditCmd(long id, Optional<String> name, Optional<File> file, Optional<Entry.Type> type,
			Optional<Integer> dataVersion, Optional<UUID> authorUuid, Optional<String> authorUsername, Optional<Boolean> verified) {
		if (checkConnectionExists() || file.isPresent() && checkFileExists(file.get()))
			return;
		
		Optional<byte[]> nbt;
		if (file.isPresent()) {
			try {
				nbt = Optional.of(Files.readAllBytes(file.get().toPath()));
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		} else
			nbt = Optional.empty();
		
		whenComplete(connection.editEntry(id, name, nbt, type, dataVersion, authorUuid, authorUsername, verified),
				v -> System.out.println("Edited entry with id " + id));
	}
	
	private void entryRemoveCmd(long id) {
		if (checkConnectionExists())
			return;
		
		whenComplete(connection.removeEntry(id), v -> System.out.println("Removed entry with id " + id));
	}
	
	private void entryGetCmd(long id, boolean verbose) {
		if (checkConnectionExists())
			return;
		
		whenComplete(connection.getEntry(id), entry -> {
			if (entry == null)
				System.err.println("Entry doesn't exist: " + id);
			else {
				results = Arrays.asList(entry);
				resultsCmd(verbose);
			}
		});
	}
	
	private void entryExportCmd(long id, File file, boolean overwrite) {
		try {
			if (checkFileDoesntExist(file, overwrite, false))
				return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		whenComplete(connection.getEntryNBT(id), nbt -> {
			if (nbt == null)
				System.err.println("Entry doesn't exist: " + id);
			else {
				try {
					Files.write(file.toPath(), nbt);
					System.out.println("Exported " + id + " to: " + file.getAbsolutePath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	private void entryListCmd(EntryFilter filter, EntryView view, boolean verbose) {
		if (checkConnectionExists())
			return;
		
		whenComplete(connection.getEntries(filter, view), entries -> {
			results = entries;
			resultsCmd(verbose);
		});
	}
	
	private void tagAddCmd(String name, int color) {
		if (checkConnectionExists())
			return;
		
		whenComplete(connection.addTag(name, color),
				v -> System.out.println("Added tag: " + name + " (#" + ColorInput.toString(color) + ")"));
	}
	
	private void tagEditCmd(String currentName, Optional<String> name, Optional<Integer> color) {
		if (checkConnectionExists())
			return;
		
		whenComplete(connection.editTag(currentName, name, color),
				v -> System.out.println("Edited tag: " + currentName + name.map(value -> " (now '" + value + "')").orElse("")));
	}
	
	private void tagRemoveCmd(String name) {
		if (checkConnectionExists())
			return;
		
		whenComplete(connection.removeTag(name), v -> System.out.println("Removed tag: " + name));
	}
	
	private void tagGetCmd(String name) {
		if (checkConnectionExists())
			return;
		
		whenComplete(connection.getTag(name), tag -> {
			if (tag == null)
				System.err.println("Tag doesn't exist: " + name);
			else
				System.out.println(tag.getName() + " (#" + ColorInput.toString(tag.getColor()) + ")");
		});
	}
	
	private void tagListCmd(TagFilter filter) {
		if (checkConnectionExists())
			return;
		
		whenComplete(connection.getTags(filter), tags -> {
			if (tags.isEmpty())
				System.out.println("No tags found");
			else {
				for (Tag tag : tags)
					System.out.println(tag.getName() + " (#" + ColorInput.toString(tag.getColor()) + ")");
			}
		});
	}
	
	private void tagAttachCmd(long entry, String tag) {
		if (checkConnectionExists())
			return;
		
		whenComplete(connection.addTagToEntry(entry, tag),
				v -> System.out.println("Attached tag '" + tag + "' to entry with id " + entry));
	}
	
	private void tagDetachCmd(long entry, String tag) {
		if (checkConnectionExists())
			return;
		
		whenComplete(connection.removeTagFromEntry(entry, tag),
				v -> System.out.println("Detached tag '" + tag + "' from entry with id " + entry));
	}
	
	private void resultsCmd(boolean verbose) {
		if (checkResultsExist())
			return;
		
		if (results.isEmpty()) {
			System.out.println("No entries found");
			return;
		}
		
		for (int i = 0; i < results.size(); i++) {
			Entry entry = results.get(i);
			
			if (i != 0)
				System.out.println();
			
			System.out.println("#" + i + ": " + entry.getId() + ": " + entry.getName());
			System.out.println("  Author: " + entry.getAuthorUsername() + " (" + entry.getAuthorUuid() + ")");
			System.out.println("  Type: " + entry.getType());
			System.out.println("  Data Version: " + DataVersion.toViewableString(entry.getDataVersion()));
			if (verbose) {
				System.out.println("  Bytes: " + entry.getNbtLength());
				System.out.println("  Created: " + UIUtil.formatTimestamp(entry.getCreated()));
				System.out.println("  Modified: " + UIUtil.formatTimestamp(entry.getModified()));
				System.out.println("  Hash: " + entry.getHash());
				System.out.println("  Verified: " + entry.isVerified());
			}
		}
	}
	
	public void exec(String cmd) {
		try {
			root.parse(new CommandStream(cmd));
		} catch (CommandSyntaxException | CommandParseException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public List<Entry> getResults() {
		return results;
	}
	
	@Override
	public void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String line;
			while (!exit && (line = in.readLine()) != null)
				exec(line);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			closeConnection(false);
		}
	}
	
}
