package com.luneruniverse.minecraft.nbtdatabase.ui.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
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
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.luneruniverse.minecraft.nbtdatabase.DataVersion;
import com.luneruniverse.minecraft.nbtdatabase.NBTDatabase;
import com.luneruniverse.minecraft.nbtdatabase.connection.NBTDatabaseAccessServer;
import com.luneruniverse.minecraft.nbtdatabase.connection.access.LocalNBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.access.NBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.access.RemoteNBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.RequestFailedException;
import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.ServerException;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.LoginPacket.User;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.FutureUtil;
import com.luneruniverse.minecraft.nbtdatabase.request.IllegalRequestException;
import com.luneruniverse.minecraft.nbtdatabase.ui.LoginUtil;
import com.luneruniverse.minecraft.nbtdatabase.ui.UIUtil;

import jnafilechooser.api.JnaFileChooser;
import net.raphimc.minecraftauth.step.java.StepMCToken.MCToken;

public class GUI implements AutoCloseable {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		LoginUtil.setMinecraftAuthLogger();
		new GUI().open();
	}
	
	private static JScrollPane createJScrollPane(JComponent component) {
		JScrollPane scrollPane = new JScrollPane(component);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(15);
		scrollPane.getVerticalScrollBar().setUnitIncrement(15);
		return scrollPane;
	}
	
	private final JFrame frame;
	private final JMenu accountMenu;
	private final JLabel connectionLabel;
	private final JLabel serverLabel;
	private final JPanel mainPanel;
	private final JTabbedPane tabs;
	private final EntriesTab entriesTab;
	private final TagsTab tagsTab;
	private final JButton refreshBtn;
	
	private User user;
	private MCToken accessToken;
	private NBTDatabase localDatabase;
	private NBTDatabaseAccess connection;
	private NBTDatabaseAccessServer server;
	
	public GUI() {
		DataVersion.loadVersions();
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		frame = new JFrame("NBT Database");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				closeConnection();
			}
		});
		frame.setSize(500, 500);
		frame.setLocationRelativeTo(null);
		UIUtil.setJFrameLogo(frame);
		
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
		
		JMenuItem configDatabaseMenuItem = new JMenuItem("Config", 'g');
		databaseMenu.add(configDatabaseMenuItem);
		configDatabaseMenuItem.addActionListener(event -> configDatabaseMenuItem());
		
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
		
		accountMenu = new JMenu("Account");
		accountMenu.setMnemonic('A');
		menuBar.add(accountMenu);
		
		JMenuItem loginAccountMenuItem = new JMenuItem("Login", 'i');
		accountMenu.add(loginAccountMenuItem);
		loginAccountMenuItem.addActionListener(event -> loginAccountMenuItem());
		
		JMenuItem logoutAccountMenuItem = new JMenuItem("Logout", 'o');
		accountMenu.add(logoutAccountMenuItem);
		logoutAccountMenuItem.addActionListener(event -> logoutAccountMenuItem());
		
		frame.setLayout(new BorderLayout());
		
		mainPanel = new JPanel(new CardLayout());
		frame.add(mainPanel, BorderLayout.CENTER);
		
		mainPanel.add(new JLabel("Connect to a database", JLabel.CENTER));
		
		tabs = new JTabbedPane();
		mainPanel.add(tabs);
		
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
	
	private void updateAccountMenu() {
		accountMenu.setText(user == null ? "Account" : "Account: " + user.getUsername());
	}
	
	private void updateConnectionInfo() {
		connectionLabel.setText(connection == null ? "Disconnected" : connection.getName());
		serverLabel.setText(server == null ? "No Server" : "Server On Port " + server.getPort());
		refreshBtn.setEnabled(connection != null);
	}
	
	private void onConnectionOpen() {
		NBTDatabaseAccess connection = this.connection;
		connection.getCloseFuture().whenComplete((v, e) -> {
			if (e != null) {
				EventQueue.invokeLater(() -> {
					if (connection == this.connection) {
						closeConnection();
						updateConnectionInfo();
						JOptionPane.showMessageDialog(frame, e.getMessage(), "Disconnected", JOptionPane.ERROR_MESSAGE);
					}
				});
			}
		});
		((CardLayout) mainPanel.getLayout()).last(mainPanel);
		entriesTab.refresh();
		tagsTab.refresh();
	}
	
	private void onConnectionClose() {
		((CardLayout) mainPanel.getLayout()).first(mainPanel);
		entriesTab.clear();
		tagsTab.clear();
	}
	
	private void onServerStart() {}
	
	private void onServerStop() {}
	
	public <T> void whenComplete(CompletableFuture<T> future, Consumer<T> consumer) {
		NBTDatabaseAccess connection = this.connection;
		future.whenComplete((value, e) -> {
			EventQueue.invokeLater(() -> {
				if (connection != this.connection)
					return;
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
			
			try {
				Files.delete(file.toPath());
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(frame, "Failed to delete '" + file.getName() + "'", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		
		closeConnection();
		
		try {
			localDatabase = new NBTDatabase(file);
			connection = new LocalNBTDatabaseAccess(localDatabase);
			onConnectionOpen();
		} catch (IllegalRequestException e) {
			System.err.println("[Database] " + e.getMessage());
			JOptionPane.showMessageDialog(frame, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
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
		} catch (IllegalRequestException e) {
			System.err.println("[Database] " + e.getMessage());
			JOptionPane.showMessageDialog(frame, e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
		} catch (SQLException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Failed to open '" + file.getName() + "'", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		updateConnectionInfo();
	}
	
	private void connectToRemoteDatabaseMenuItem() {
		if (accessToken != null && accessToken.isExpired()) {
			user = null;
			accessToken = null;
			updateAccountMenu();
			JOptionPane.showMessageDialog(frame, "Your access token has expired; you are now logged out",
					"Error", JOptionPane.ERROR_MESSAGE);
		}
		
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
			connection = new RemoteNBTDatabaseAccess(ip, port, user, accessToken == null ? null : accessToken.getAccessToken());
			onConnectionOpen();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Failed to connect to '" + ip + ":" + port + "'", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		updateConnectionInfo();
	}
	
	private void configDatabaseMenuItem() {
		if (checkConnectionExists())
			return;
		
		whenComplete(connection.getConfig(), config -> {
			JPanel panel = new JPanel(TableLayout.ofColumns(2, 4));
			
			panel.add(new JLabel("max_nbt_size"));
			
			JSpinner maxNbtSizeField = new JSpinner();
			panel.add(maxNbtSizeField);
			((SpinnerNumberModel) maxNbtSizeField.getModel()).setMinimum(0);
			maxNbtSizeField.setValue(config.getMaxNbtSize());
			
			panel.add(new JLabel("max_num_results"));
			
			JSpinner maxNumResultsField = new JSpinner();
			panel.add(maxNumResultsField);
			((SpinnerNumberModel) maxNumResultsField.getModel()).setMinimum(0);
			maxNumResultsField.setValue(config.getMaxNumResults());
			
			if (JOptionPane.showConfirmDialog(frame, panel, "Config", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
				return;
			
			config.setMaxNbtSize((int) maxNbtSizeField.getValue());
			config.setMaxNumResults((int) maxNumResultsField.getValue());
			
			whenComplete(connection.setConfig(config), v -> {});
		});
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
		} catch (NoSuchAlgorithmException | IOException | InterruptedException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Failed to start server", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		updateConnectionInfo();
	}
	
	private void customPortStartServerMenuItem() {
		String portStr = JOptionPane.showInputDialog(frame, "Enter port:", "Start Server", JOptionPane.QUESTION_MESSAGE);
		if (portStr == null)
			return;
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
		} catch (NoSuchAlgorithmException | IOException | InterruptedException e) {
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
	
	private void loginAccountMenuItem() {
		FutureUtil.DAEMON_EXECUTOR.execute(() -> {
			AtomicBoolean cancelled = new AtomicBoolean();
			LoginUtil.loginWithDeviceCode(url -> {
				switch (JOptionPane.showOptionDialog(frame, "Use this link to login: (expires in 5 minutes)\n" + url,
						"Login", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
						new String[] {"Open Link", "Copy Link", "Cancel"}, null)) {
					case -1: // Close
						break;
					case 0: // Open Link
						try {
							Desktop.getDesktop().browse(new URI(url));
						} catch (IOException | URISyntaxException e) {
							e.printStackTrace();
						}
						break;
					case 1: // Copy Link
						Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url), null);
						break;
					case 2: // Cancel
						cancelled.set(true);
						break;
				}
			}, session -> {
				EventQueue.invokeLater(() -> {
					user = new User(session.getMcProfile().getId(), session.getMcProfile().getName());
					accessToken = session.getMcProfile().getMcToken();
					updateAccountMenu();
					JOptionPane.showMessageDialog(frame, "Logged in as " + user.getUsername() + " (" + user.getUuid() + ")",
							"Login", JOptionPane.INFORMATION_MESSAGE);
				});
			}, () -> {
				if (!cancelled.get())
					JOptionPane.showMessageDialog(frame, "Login timed out", "Error", JOptionPane.ERROR_MESSAGE);
			});
		});
	}
	
	private void logoutAccountMenuItem() {
		if (user == null) {
			JOptionPane.showMessageDialog(frame, "You are already logged out", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		user = null;
		accessToken = null;
		updateAccountMenu();
	}
	
	private void refreshBtn() {
		switch (tabs.getSelectedIndex()) {
			case 0:
				entriesTab.refresh();
				break;
			case 1:
				tagsTab.refresh();
				break;
		}
	}
	
	public void open() {
		frame.setVisible(true);
	}
	
	public boolean isOpen() {
		return frame.isVisible();
	}
	
	@Override
	public void close() {
		frame.dispose();
	}
	
}
