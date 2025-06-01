package com.luneruniverse.minecraft.nbtdatabase.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;

import com.luneruniverse.minecraft.nbtdatabase.NBTDatabase;
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
	
	public CLI() {
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
