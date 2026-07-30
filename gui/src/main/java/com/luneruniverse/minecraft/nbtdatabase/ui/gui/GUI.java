package com.luneruniverse.minecraft.nbtdatabase.ui.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.spongepowered.configurate.ConfigurateException;

import com.luneruniverse.minecraft.nbtdatabase.DataVersion;
import com.luneruniverse.minecraft.nbtdatabase.NBTDatabase;
import com.luneruniverse.minecraft.nbtdatabase.connection.AsyncCloseable;
import com.luneruniverse.minecraft.nbtdatabase.connection.access.LocalNBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.access.NBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.access.RemoteNBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.RequestFailedException;
import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.ServerException;
import com.luneruniverse.minecraft.nbtdatabase.connection.server.NBTDatabaseAccessServer;
import com.luneruniverse.minecraft.nbtdatabase.connection.server.ServerConfig;
import com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.permission.GlobalPermissionManager;
import com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.permission.PermissionAuthorizationManager;
import com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.permission.Permissions;
import com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.permission.Roles;
import com.luneruniverse.minecraft.nbtdatabase.connection.user.Profile;
import com.luneruniverse.minecraft.nbtdatabase.connection.user.User;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.ConfigurateUtil;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.FutureUtil;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.IOUtil;
import com.luneruniverse.minecraft.nbtdatabase.request.IllegalRequestException;
import com.luneruniverse.minecraft.nbtdatabase.ui.LoginUtil;
import com.luneruniverse.minecraft.nbtdatabase.ui.UIUtil;

import net.raphimc.minecraftauth.java.model.MinecraftToken;

public class GUI implements AsyncCloseable {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		new GUI().open();
	}
	
	private static JScrollPane createJScrollPane(JComponent component) {
		JScrollPane scrollPane = new JScrollPane(component);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(15);
		scrollPane.getVerticalScrollBar().setUnitIncrement(15);
		return scrollPane;
	}
	
	private final CompletableFuture<Void> closeFuture;
	
	private final JFrame frame;
	private final JMenu accountMenu;
	private final JLabel connectionLabel;
	private final JLabel serverLabel;
	private final JPanel mainPanel;
	private final JTabbedPane tabs;
	private final EntriesTab entriesTab;
	private final TagsTab tagsTab;
	private final JButton refreshBtn;
	
	private Profile profile;
	private MinecraftToken accessToken;
	private NBTDatabase localDatabase;
	private NBTDatabaseAccess connection;
	private NBTDatabaseAccessServer server;
	
	public GUI() {
		DataVersion.loadVersions();
		
		closeFuture = new CompletableFuture<>();
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		frame = new JFrame("NBT Database");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				FutureUtil.whenComplete(closeAsync(), (v, e) -> {
					if (e != null)
						e.printStackTrace();
				});
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
		
		JMenuItem quickStartServerMenuItem = new JMenuItem("Quick Start", 'Q');
		serverMenu.add(quickStartServerMenuItem);
		quickStartServerMenuItem.addActionListener(event -> quickStartServerMenuItem());
		
		JMenuItem advancedStartServerMenuItem = new JMenuItem("Advanced Start", 'A');
		serverMenu.add(advancedStartServerMenuItem);
		advancedStartServerMenuItem.addActionListener(event -> advancedStartServerMenuItem());
		
		JMenuItem stopServerMenuItem = new JMenuItem("Stop", 'S');
		serverMenu.add(stopServerMenuItem);
		stopServerMenuItem.addActionListener(event -> stopServerMenuItem());
		
		JMenuItem templateServerMenuItem = new JMenuItem("Template", 'T');
		serverMenu.add(templateServerMenuItem);
		templateServerMenuItem.addActionListener(event -> templateServerMenuItem());
		
		JMenuItem usersServerMenuItem = new JMenuItem("Users", 'U');
		serverMenu.add(usersServerMenuItem);
		usersServerMenuItem.addActionListener(event -> usersServerMenuItem());
		
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
		accountMenu.setText(profile == null ? "Account" : "Account: " + profile.getUsername());
	}
	
	private void updateConnectionInfo() {
		connectionLabel.setText(connection == null ? "Disconnected" : connection.getName());
		serverLabel.setText(server == null ? "No Server" : "Server On Port " + server.getPort());
		refreshBtn.setEnabled(connection != null);
	}
	
	private void onConnectionOpen() {
		NBTDatabaseAccess connection = this.connection;
		FutureUtil.whenCompleteAsync(connection.getCloseFuture(), (v, e) -> {
			if (e != null && connection == this.connection) {
				closeConnection();
				updateConnectionInfo();
				System.err.println("[Disconnected] " + e.getMessage());
				JOptionPane.showMessageDialog(frame, e.getMessage(), "Disconnected", JOptionPane.ERROR_MESSAGE);
			}
		}, EventQueue::invokeLater);
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
		FutureUtil.whenCompleteAsync(future, (value, e) -> {
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
				} else {
					e.printStackTrace();
					JOptionPane.showMessageDialog(frame, e.getMessage(), "Internal Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}, EventQueue::invokeLater);
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
	
	private synchronized void closeConnection() {
		closeServer();
		
		if (connection != null) {
			try {
				connection.close();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
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
	
	private synchronized void closeServer() {
		if (server != null) {
			try {
				server.close();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
			server = null;
			onServerStop();
		}
	}
	
	private void newFileDatabaseMenuItem() {
		File file = FileKitWrapper.openFileSaver(frame, "New Database File", null, "db");
		if (file == null)
			return;
		
		if (file.exists()) {
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
		File file = FileKitWrapper.openFilePicker(frame, "Open Database File", "db");
		if (file == null)
			return;
		
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
			profile = null;
			accessToken = null;
			updateAccountMenu();
			JOptionPane.showMessageDialog(frame, "Your access token has expired; you are now logged out",
					"Error", JOptionPane.ERROR_MESSAGE);
		}
		
		String address = JOptionPane.showInputDialog(frame,
				"Enter address:\n(Port will default to " + NBTDatabase.DEFAULT_PORT + ")",
				"Connect To Remote", JOptionPane.QUESTION_MESSAGE);
		if (address == null || address.isEmpty())
			return;
		
		URI uri;
		try {
			uri = RemoteNBTDatabaseAccess.parseNBTUri(address);
		} catch (URISyntaxException e) {
			JOptionPane.showMessageDialog(frame, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		closeConnection();
		
		try {
			connection = new RemoteNBTDatabaseAccess(uri, profile, accessToken == null ? null : accessToken.getToken());
			onConnectionOpen();
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Failed to connect to '" + uri + "'", "Error", JOptionPane.ERROR_MESSAGE);
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
	
	private void quickStartServerMenuItem() {
		if (checkConnectionExists())
			return;
		
		closeServer();
		
		try {
			server = new NBTDatabaseAccessServer(connection, NBTDatabase.DEFAULT_PORT);
			onServerStart();
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Failed to start server", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		updateConnectionInfo();
	}
	
	private void advancedStartServerMenuItem() {
		if (checkConnectionExists())
			return;
		
		JPanel panel = new JPanel(TableLayout.ofColumns(1, 4));
		
		JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		panel.add(portPanel);
		
		portPanel.add(new JLabel("Port:"));
		
		JSpinner portField = new JSpinner(new SpinnerNumberModel(NBTDatabase.DEFAULT_PORT, 0, 0xFFFF, 1));
		portPanel.add(portField);
		
		JTabbedPane configTabs = new JTabbedPane();
		panel.add(configTabs);
		
		JPanel basicConfigPanel = new JPanel(TableLayout.ofColumns(1, 4));
		configTabs.addTab("Basic Config", basicConfigPanel);
		
		JRadioButton noPerms = new JRadioButton("Authorization: Allow");
		basicConfigPanel.add(noPerms);
		noPerms.setSelected(true);
		
		JRadioButton globalPerms = new JRadioButton("Authorization: Global Permission");
		basicConfigPanel.add(globalPerms);
		
		ButtonGroup permsRadio = new ButtonGroup();
		permsRadio.add(noPerms);
		permsRadio.add(globalPerms);
		
		JPanel permissionsPanel = new JPanel(TableLayout.ofColumns(4, 4).columnMajor());
		basicConfigPanel.add(permissionsPanel);
		
		List<String> permissions = new ArrayList<>();
		permissions.addAll(Roles.getRoles().keySet());
		permissions.addAll(Permissions.getPermissions());
		
		Map<String, JCheckBox> permissionFields = new HashMap<>();
		for (String permission : permissions) {
			JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
			permissionsPanel.add(row);
			
			JCheckBox checkbox = new JCheckBox();
			row.add(checkbox);
			checkbox.setEnabled(false);
			permissionFields.put(permission, checkbox);
			
			JLabel label = new JLabel(permission);
			row.add(label);
			
			label.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent mouseEvent) {
					if (checkbox.isEnabled())
						checkbox.setSelected(!checkbox.isSelected());
				}
			});
		}
		
		noPerms.addActionListener(event -> permissionFields.values().forEach(checkbox -> checkbox.setEnabled(false)));
		globalPerms.addActionListener(event -> permissionFields.values().forEach(checkbox -> checkbox.setEnabled(true)));
		
		JPanel fullConfigPanel = new JPanel(new BorderLayout(4, 4));
		configTabs.addTab("Full Config", fullConfigPanel);
		fullConfigPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
		
		JPanel fullConfigHeaderPanel = new JPanel(TableLayout.ofColumns(1, 4));
		fullConfigPanel.add(BorderLayout.NORTH, fullConfigHeaderPanel);
		
		JPanel configFilePanel = new JPanel(TableLayout.ofRows(1, 4));
		fullConfigHeaderPanel.add(BorderLayout.NORTH, configFilePanel);
		
		JButton loadConfigFileBtn = new JButton("Load Config File");
		configFilePanel.add(loadConfigFileBtn);
		
		JButton saveConfigFileBtn = new JButton("Save Config File");
		configFilePanel.add(saveConfigFileBtn);
		
		JPanel serverRootPanel = new JPanel(TableLayout.ofRows(1, 4));
		fullConfigHeaderPanel.add(BorderLayout.NORTH, serverRootPanel);
		
		serverRootPanel.add(new JLabel("Server Root:"));
		
		JTextField serverRootField = new JTextField(new File(".").getAbsoluteFile().getParent());
		serverRootPanel.add(serverRootField);
		
		JTextArea configField = new JTextArea();
		fullConfigPanel.add(BorderLayout.CENTER, new JScrollPane(configField));
		
		loadConfigFileBtn.addActionListener(event -> {
			File file = FileKitWrapper.openFilePicker(frame, "Load Config File", "yaml");
			if (file == null)
				return;
			
			try {
				configField.setText(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).replace("\r", ""));
				serverRootField.setText(file.getAbsoluteFile().getParent());
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(frame, "Failed to load '" + file.getName() + "'", "Error", JOptionPane.ERROR_MESSAGE);
			}
		});
		
		saveConfigFileBtn.addActionListener(event -> {
			File file = FileKitWrapper.openFileSaver(frame, "Save Config File", null, "yaml");
			if (file == null)
				return;
			
			try {
				Files.write(file.toPath(), configField.getText().getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(frame, "Failed to save '" + file.getName() + "'", "Error", JOptionPane.ERROR_MESSAGE);
			}
		});
		
		if (JOptionPane.showConfirmDialog(frame, panel, "Advanced Start Server", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
			return;
		
		int port = (Integer) portField.getValue();
		
		ServerConfig config;
		if (configTabs.getSelectedComponent() == basicConfigPanel) {
			ServerConfig.Builder configBuilder = ServerConfig.builder();
			
			if (globalPerms.isSelected()) {
				permissions.clear();
				
				for (Map.Entry<String, JCheckBox> permission : permissionFields.entrySet()) {
					if (permission.getValue().isSelected())
						permissions.add(permission.getKey());
				}
				
				configBuilder.authorizationManager(PermissionAuthorizationManager.create(
						GlobalPermissionManager.fromMatchers(permissions.toArray(new String[0]))));
			}
			
			config = configBuilder.build();
		} else {
			File serverRoot = new File(serverRootField.getText());
			if (!serverRoot.isDirectory()) {
				JOptionPane.showMessageDialog(frame, "'" + serverRoot.getName() + "' is not a folder", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			try {
				config = ServerConfig.fromNode(serverRoot, ConfigurateUtil.parseYamlString(configField.getText()));
			} catch (ConfigurateException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(frame, "Failed to parse config", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		
		closeServer();
		
		try {
			server = new NBTDatabaseAccessServer(connection, port, config);
			onServerStart();
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Failed to start server", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
		updateConnectionInfo();
	}
	
	private void stopServerMenuItem() {
		if (checkServerExists())
			return;
		
		closeServer();
		
		updateConnectionInfo();
	}
	
	private void templateServerMenuItem() {
		File folder = FileKitWrapper.openDirectoryPicker(frame, "Export Server Template");
		if (folder == null)
			return;
		
		try {
			List<String> conflicts = IOUtil.extractResourcesDryRun("server_template", folder.toPath());
			if (!conflicts.isEmpty()) {
				JOptionPane.showMessageDialog(frame, conflicts.stream().map(conflict -> new File(folder, conflict).getAbsolutePath())
						.reduce("File(s) already exist:", (a, b) -> a + "\n- " + b), "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			IOUtil.extractResources("server_template", folder.toPath());
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Failed to export to '" + folder.getName() + "'", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void usersServerMenuItem() {
		if (checkServerExists())
			return;
		
		Collection<User> users = server.getUsers();
		
		JPanel panel = new JPanel(TableLayout.ofColumns(3, 4));
		
		if (users.isEmpty()) {
			panel.add(new JLabel("No users"));
		} else {
			Font boldFont = panel.getFont().deriveFont(Font.BOLD);
			
			JLabel userLabel = new JLabel("User");
			panel.add(userLabel);
			userLabel.setFont(boldFont);
			
			JLabel ipLabel = new JLabel("IP");
			panel.add(ipLabel);
			ipLabel.setFont(boldFont);
			
			JLabel clientTypeLabel = new JLabel("Client Type");
			panel.add(clientTypeLabel);
			clientTypeLabel.setFont(boldFont);
			
			for (User user : users) {
				panel.add(new JLabel(user.toString()));
				panel.add(new JLabel(user.getIp()));
				panel.add(new JLabel(user.getClientType().toString()));
			}
		}
		
		JOptionPane.showMessageDialog(frame, panel, "Server Users", JOptionPane.INFORMATION_MESSAGE);
	}
	
	private void loginAccountMenuItem() {
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
						JOptionPane.showMessageDialog(frame, "Failed to open link", "Error", JOptionPane.ERROR_MESSAGE);
					}
					break;
				case 1: // Copy Link
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url), null);
					break;
				case 2: // Cancel
					cancelled.set(true);
					break;
			}
		}, (profile, accessToken) -> {
			EventQueue.invokeLater(() -> {
				this.profile = profile;
				this.accessToken = accessToken;
				updateAccountMenu();
				JOptionPane.showMessageDialog(frame, "Logged in as " + profile.getUsername(),
						"Login", JOptionPane.INFORMATION_MESSAGE);
			});
		}, () -> {
			if (!cancelled.get())
				JOptionPane.showMessageDialog(frame, "Login timed out", "Error", JOptionPane.ERROR_MESSAGE);
		}, e -> {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Failed to login", "Error", JOptionPane.ERROR_MESSAGE);
		});
	}
	
	private void logoutAccountMenuItem() {
		if (profile == null) {
			JOptionPane.showMessageDialog(frame, "You are already logged out", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		profile = null;
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
	
	@Override
	public CompletableFuture<Void> getCloseFuture() {
		return closeFuture;
	}
	
	@Override
	public CompletableFuture<Void> closeAsync() {
		closeFuture.complete(null);
		return FutureUtil.runAsync(this::close, ForkJoinPool.commonPool());
	}
	
	@Override
	public void close() throws InterruptedException {
		closeFuture.complete(null);
		try {
			// frame.dispose() deadlocks when called from another thread
			if (EventQueue.isDispatchThread()) {
				frame.dispose();
			} else {
				CountDownLatch disposeLatch = new CountDownLatch(1);
				EventQueue.invokeLater(() -> {
					try {
						frame.dispose();
					} finally {
						disposeLatch.countDown();
					}
				});
				disposeLatch.await();
			}
		} finally {
			closeConnection();
		}
	}
	
}
