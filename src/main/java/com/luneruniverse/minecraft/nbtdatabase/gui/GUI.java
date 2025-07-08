package com.luneruniverse.minecraft.nbtdatabase.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.luneruniverse.minecraft.nbtdatabase.DataVersion;
import com.luneruniverse.minecraft.nbtdatabase.IllegalRequestException;
import com.luneruniverse.minecraft.nbtdatabase.NBTDatabase;
import com.luneruniverse.minecraft.nbtdatabase.connection.LocalNBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.NBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.NBTDatabaseAccessServer;
import com.luneruniverse.minecraft.nbtdatabase.connection.RemoteNBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.RequestFailedException;
import com.luneruniverse.minecraft.nbtdatabase.connection.ServerException;

import jnafilechooser.api.JnaFileChooser;

public class GUI {
	
	private static JScrollPane createJScrollPane(JComponent component) {
		JScrollPane scrollPane = new JScrollPane(component);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(15);
		scrollPane.getVerticalScrollBar().setUnitIncrement(15);
		return scrollPane;
	}
	
	private final JFrame frame;
	private final JLabel connectionLabel;
	private final JLabel serverLabel;
	private final JPanel mainPanel;
	private final JTabbedPane tabs;
	private final MetadataTab metadataTab;
	private final EntriesTab entriesTab;
	private final TagsTab tagsTab;
	private final JButton refreshBtn;
	
	private NBTDatabase localDatabase;
	private NBTDatabaseAccess connection;
	private NBTDatabaseAccessServer server;
	
	public GUI() {
		DataVersion.loadVersions();
		
		frame = new JFrame("NBT Database");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				closeConnection();
			}
		});
		frame.setSize(500, 500);
		frame.setLocationRelativeTo(null);
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		JMenu databaseMenu = new JMenu("Database");
		databaseMenu.setMnemonic('D');
		menuBar.add(databaseMenu);
		
		JMenuItem newFileDatabaseMenuItem = new JMenuItem("New File", 'N');
		databaseMenu.add(newFileDatabaseMenuItem);
		newFileDatabaseMenuItem.addActionListener(event -> newFileDatabaseMenuItem());
		
		JMenuItem openFileDatabaseMenuItem = new JMenuItem("Open File", 'O');
		databaseMenu.add(openFileDatabaseMenuItem);
		openFileDatabaseMenuItem.addActionListener(event -> openFileDatabaseMenuItem());
		
		JMenuItem connectToRemoteDatabaseMenuItem = new JMenuItem("Connect To Remote", 'R');
		databaseMenu.add(connectToRemoteDatabaseMenuItem);
		connectToRemoteDatabaseMenuItem.addActionListener(event -> connectToRemoteDatabaseMenuItem());
		
		JMenuItem closeDatabaseMenuItem = new JMenuItem("Close", 'C');
		databaseMenu.add(closeDatabaseMenuItem);
		closeDatabaseMenuItem.addActionListener(event -> closeDatabaseMenuItem());
		
		JMenu serverMenu = new JMenu("Server");
		serverMenu.setMnemonic('S');
		menuBar.add(serverMenu);
		
		JMenu startServerMenu = new JMenu("Start");
		startServerMenu.setMnemonic('S');
		serverMenu.add(startServerMenu);
		
		JMenuItem defaultPortStartServerMenuItem = new JMenuItem("Port 25560", 'P');
		startServerMenu.add(defaultPortStartServerMenuItem);
		defaultPortStartServerMenuItem.addActionListener(event -> defaultPortStartServerMenuItem());
		
		JMenuItem customPortStartServerMenuItem = new JMenuItem("Custom Port", 'C');
		startServerMenu.add(customPortStartServerMenuItem);
		customPortStartServerMenuItem.addActionListener(event -> customPortStartServerMenuItem());
		
		JMenuItem stopServerMenuItem = new JMenuItem("Stop", 't');
		serverMenu.add(stopServerMenuItem);
		stopServerMenuItem.addActionListener(event -> stopServerMenuItem());
		
		frame.setLayout(new BorderLayout());
		
		mainPanel = new JPanel(new CardLayout());
		frame.add(mainPanel, BorderLayout.CENTER);
		
		mainPanel.add(new JLabel("Connect to a database", JLabel.CENTER));
		
		tabs = new JTabbedPane();
		mainPanel.add(tabs);
		
		JPanel metadataTab = new JPanel();
		tabs.addTab("Metadata", createJScrollPane(metadataTab));
		this.metadataTab = new MetadataTab(this, frame, metadataTab);
		
		JPanel entriesTab = new JPanel();
		tabs.addTab("Entries", createJScrollPane(entriesTab));
		this.entriesTab = new EntriesTab(this, frame, entriesTab);
		
		JPanel tagsTab = new JPanel();
		tabs.addTab("Tags", createJScrollPane(tagsTab));
		this.tagsTab = new TagsTab(this, frame, tagsTab);
		
		JPanel connectionInfo = new JPanel(new BorderLayout());
		frame.add(connectionInfo, BorderLayout.SOUTH);
		
		JPanel leftConnectionInfo = new JPanel();
		leftConnectionInfo.setLayout(new BoxLayout(leftConnectionInfo, BoxLayout.X_AXIS));
		connectionInfo.add(leftConnectionInfo, BorderLayout.CENTER);
		
		leftConnectionInfo.add(Box.createHorizontalStrut(4));
		
		connectionLabel = new JLabel();
		leftConnectionInfo.add(connectionLabel);
		
		leftConnectionInfo.add(Box.createHorizontalStrut(4));
		
		JSeparator seperatorLeftConnectionInfo = new JSeparator(SwingConstants.VERTICAL);
		seperatorLeftConnectionInfo.setMaximumSize(new Dimension(2, Integer.MAX_VALUE));
		leftConnectionInfo.add(seperatorLeftConnectionInfo);
		
		leftConnectionInfo.add(Box.createHorizontalStrut(4));
		
		serverLabel = new JLabel();
		leftConnectionInfo.add(serverLabel);
		
		refreshBtn = new JButton("Refresh");
		connectionInfo.add(refreshBtn, BorderLayout.EAST);
		refreshBtn.setToolTipText("Refresh the current tab");
		refreshBtn.addActionListener(event -> refreshBtn());
		
		updateConnectionInfo();
	}
	
	public NBTDatabase getLocalDatabase() {
		return localDatabase;
	}
	
	public NBTDatabaseAccess getConnection() {
		return connection;
	}
	
	public NBTDatabaseAccessServer getServer() {
		return server;
	}
	
	private void updateConnectionInfo() {
		connectionLabel.setText(connection == null ? "Disconnected" : connection.getName());
		serverLabel.setText(server == null ? "No Server" : "Server On Port " + server.getPort());
		refreshBtn.setEnabled(connection != null);
	}
	
	private void onConnectionOpen() {
		((CardLayout) mainPanel.getLayout()).last(mainPanel);
		metadataTab.refresh();
		entriesTab.refresh();
		tagsTab.refresh();
	}
	
	private void onConnectionClose() {
		((CardLayout) mainPanel.getLayout()).first(mainPanel);
	}
	
	private void onServerStart() {}
	
	private void onServerStop() {}
	
	public <T> void whenComplete(CompletableFuture<T> future, Consumer<T> consumer) {
		future.whenComplete((value, e) -> {
			if (e == null)
				consumer.accept(value);
			else {
				if (e instanceof IllegalRequestException || e instanceof RequestFailedException) {
					if (e instanceof IllegalRequestException) {
						System.err.println("[Database] " + e.getMessage());
						JOptionPane.showMessageDialog(frame, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
					} else if (e instanceof ServerException) {
						System.err.println("[Server] " + e.getMessage());
						JOptionPane.showMessageDialog(frame, e.getMessage(), "Server Error", JOptionPane.ERROR_MESSAGE);
					} else {
						System.err.println(e.getMessage());
						JOptionPane.showMessageDialog(frame, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					}
					if (e.getCause() != null)
						e.getCause().printStackTrace();
				} else
					e.printStackTrace();
			}
		});
	}
	
	public boolean checkConnectionExists() {
		if (connection == null) {
			JOptionPane.showMessageDialog(frame, "There is not an open connection", "Error", JOptionPane.ERROR_MESSAGE);
			return true;
		}
		
		return false;
	}
	
	public boolean checkServerExists() {
		if (server == null) {
			JOptionPane.showMessageDialog(frame, "There is not a running server", "Error", JOptionPane.ERROR_MESSAGE);
			return true;
		}
		
		return false;
	}
	
	private void closeConnection() {
		closeServer();
		
		if (connection != null) {
			whenComplete(connection.closeAsync(), v -> {});
			connection = null;
			onConnectionClose();
		}
		
		if (localDatabase != null) {
			try {
				localDatabase.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			localDatabase = null;
		}
	}
	
	private void closeServer() {
		if (server != null) {
			whenComplete(server.closeAsync(), v -> {});
			server = null;
			onServerStop();
		}
	}
	
	private void newFileDatabaseMenuItem() {
		JnaFileChooser chooser = new JnaFileChooser(".");
		chooser.setTitle("New Database File");
		chooser.addFilter("Database (*.db)", "db");
		chooser.addFilter("All Files (*.*)", "*");
		if (!chooser.showSaveDialog(frame))
			return;
		File file = chooser.getSelectedFile();
		
		if (file.exists()) {
			if (JOptionPane.showConfirmDialog(frame, "'" + file.getName() + "' already exists. Overwrite?",
					"New Database File", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
				return;
			}
			
			file.delete();
		}
		
		closeConnection();
		
		try {
			localDatabase = new NBTDatabase(file);
			connection = new LocalNBTDatabaseAccess(localDatabase);
			onConnectionOpen();
		} catch (SQLException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Failed to create '" + file.getName() + "'", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		updateConnectionInfo();
	}
	
	private void openFileDatabaseMenuItem() {
		JnaFileChooser chooser = new JnaFileChooser(".");
		chooser.setTitle("Open Database File");
		chooser.addFilter("Database (*.db)", "db");
		chooser.addFilter("All Files (*.*)", "*");
		if (!chooser.showOpenDialog(frame))
			return;
		File file = chooser.getSelectedFile();
		
		if (!file.exists()) {
			if (JOptionPane.showConfirmDialog(frame, "'" + file.getName() + "' doesn't exist. Create?",
					"Open Database File", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
				return;
			}
		}
		
		closeConnection();
		
		try {
			localDatabase = new NBTDatabase(file);
			connection = new LocalNBTDatabaseAccess(localDatabase);
			onConnectionOpen();
		} catch (SQLException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Failed to open '" + file.getName() + "'", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		updateConnectionInfo();
	}
	
	private void connectToRemoteDatabaseMenuItem() {
		String address = JOptionPane.showInputDialog(frame, "Enter address:\n(Port will default to 25560)",
				"Connect To Remote", JOptionPane.QUESTION_MESSAGE);
		if (address == null)
			return;
		int lastColon = address.lastIndexOf(':');
		String ip;
		int port;
		if (lastColon == -1) {
			ip = address;
			port = 25560;
		} else {
			try {
				port = Integer.parseInt(address.substring(lastColon + 1));
				ip = address.substring(0, lastColon);
			} catch (NumberFormatException e) {
				ip = address;
				port = 25560;
			}
		}
		
		closeConnection();
		
		try {
			connection = new RemoteNBTDatabaseAccess(ip, port);
			onConnectionOpen();
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Failed to connect to '" + ip + ":" + port + "'", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		updateConnectionInfo();
	}
	
	private void closeDatabaseMenuItem() {
		if (checkConnectionExists())
			return;
		
		closeConnection();
		
		updateConnectionInfo();
	}
	
	private void defaultPortStartServerMenuItem() {
		if (checkConnectionExists())
			return;
		
		closeServer();
		
		try {
			server = new NBTDatabaseAccessServer(connection, 25560);
			onServerStart();
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Failed to start server", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		updateConnectionInfo();
	}
	
	private void customPortStartServerMenuItem() {
		String portStr = JOptionPane.showInputDialog(frame, "Enter port:", "Start Server", JOptionPane.QUESTION_MESSAGE);
		int port;
		try {
			port = Integer.parseInt(portStr);
			if (port < 0 || port > 0xFFFF)
				throw new NumberFormatException();
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(frame, "Invalid port number '" + portStr + "'", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		if (checkConnectionExists())
			return;
		
		closeServer();
		
		try {
			server = new NBTDatabaseAccessServer(connection, port);
			onServerStart();
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Failed to start server on port '" + port + "'", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		updateConnectionInfo();
	}
	
	private void stopServerMenuItem() {
		if (checkServerExists())
			return;
		
		closeServer();
		
		updateConnectionInfo();
	}
	
	private void refreshBtn() {
		switch (tabs.getSelectedIndex()) {
			case 0:
				metadataTab.refresh();
				break;
			case 1:
				entriesTab.refresh();
				break;
			case 2:
				tagsTab.refresh();
				break;
		}
	}
	
	public void open() {
		frame.setVisible(true);
	}
	
	public void close() {
		frame.dispose();
	}
	
}
