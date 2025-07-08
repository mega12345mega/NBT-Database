package com.luneruniverse.minecraft.nbtdatabase.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import com.luneruniverse.minecraft.nbtdatabase.DataVersion;
import com.luneruniverse.minecraft.nbtdatabase.EntryFilter;
import com.luneruniverse.minecraft.nbtdatabase.NBTEntry;
import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.TagFilter;
import com.luneruniverse.minecraft.nbtdatabase.Util;
import com.luneruniverse.minecraft.nbtdatabase.cli.DataVersionInput;
import com.luneruniverse.minecraft.nbtdatabase.cli.UUIDInput;
import com.luneruniverse.simplecli.CommandParseException;

import jnafilechooser.api.JnaFileChooser;
import net.querz.nbt.io.NBTSerializer;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.IntTag;

public class EntriesTab {
	
	private final GUI gui;
	private final JFrame frame;
	private final JPanel entries;
	private EntryFilter filter;
	
	public EntriesTab(GUI gui, JFrame frame, JPanel panel) {
		this.gui = gui;
		this.frame = frame;
		
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(new EmptyBorder(4, 4, 4, 4));
		
		JPanel options = new JPanel();
		options.setLayout(new BoxLayout(options, BoxLayout.X_AXIS));
		panel.add(options);
		options.setAlignmentX(0);
		
		JTextField nameFilterField = new JTextField();
		options.add(nameFilterField);
		nameFilterField.setMaximumSize(new Dimension(nameFilterField.getMaximumSize().width, nameFilterField.getPreferredSize().height));
		GUIUtil.addTextFieldChangeListener(nameFilterField, text -> filter.filterByName(text.isEmpty() ? null : text));
		nameFilterField.addActionListener(event -> refresh());
		
		options.add(Box.createRigidArea(new Dimension(4, 0)));
		
		JButton advancedSearchBtn = new JButton("Advanced Search");
		options.add(advancedSearchBtn);
		advancedSearchBtn.addActionListener(event -> advancedSearchBtn());
		
		options.add(Box.createRigidArea(new Dimension(4, 0)));
		
		JButton addEntryBtn = new JButton("Add Entry");
		options.add(addEntryBtn);
		addEntryBtn.addActionListener(event -> editEntryBtn(null, null));
		
		entries = new JPanel();
		entries.setLayout(new BoxLayout(entries, BoxLayout.Y_AXIS));
		panel.add(entries);
		
		filter = new EntryFilter();
	}
	
	private JPanel addEntry(NBTEntry entry) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		entries.add(panel);
		panel.setAlignmentX(0);
		
		TitledBorder border = new TitledBorder(entry.name + (entry.verified ? " ✔️" : ""));
		panel.setBorder(new CompoundBorder(new EmptyBorder(4, 0, 0, 0), border));
		border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD));
		if (entry.verified)
			border.setTitleColor(new Color(0x008800));
		
		@SuppressWarnings("serial")
		JPanel details = new JPanel() {
			@Override
			public Dimension getMaximumSize() {
				return new Dimension(Integer.MAX_VALUE, super.getMaximumSize().height);
			}
		};
		details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
		panel.add(details);
		details.setAlignmentY(1);
		
		JPanel tags = new JPanel();
		tags.setLayout(new BoxLayout(tags, BoxLayout.X_AXIS));
		details.add(tags);
		tags.setAlignmentX(0);
		
		JLabel author = new JLabel("Author: " + entry.authorUsername);
		author.setToolTipText("UUID: " + entry.authorUuid);
		details.add(author);
		
		details.add(new JLabel("Data Version: " + DataVersion.toString(entry.dataVersion)));
		
		details.add(new JLabel("Bytes: " + String.format("%,d", entry.nbt.length)));
		
		JLabel created = new JLabel("Created: " + Util.formatTimestamp(entry.created));
		if (entry.created == entry.modified)
			created.setToolTipText("Never Modified");
		else
			created.setToolTipText("Modified: " + Util.formatTimestamp(entry.modified));
		details.add(created);
		
		JPanel options = new JPanel();
		options.setLayout(new BoxLayout(options, BoxLayout.X_AXIS));
		panel.add(options);
		options.setAlignmentY(1);
		
		JButton detailsEntryBtn = new JButton("Details");
		options.add(detailsEntryBtn);
		detailsEntryBtn.addActionListener(event -> detailsEntryBtn(entry));
		
		JButton exportEntryBtn = new JButton("Export");
		options.add(exportEntryBtn);
		exportEntryBtn.addActionListener(event -> exportEntryBtn(entry.name, entry.nbt));
		
		JButton editEntryBtn = new JButton("Edit");
		options.add(editEntryBtn);
		editEntryBtn.addActionListener(event -> {
			gui.whenComplete(gui.getConnection().getTags(new TagFilter().filterByEntryId(entry.id)),
					previousTags -> editEntryBtn(entry, previousTags.stream().map(tag -> tag.name).collect(Collectors.toSet())));
		});
		
		JButton removeEntryBtn = new JButton("-");
		options.add(removeEntryBtn);
		removeEntryBtn.addActionListener(event -> removeEntryBtn(entry.id, entry.name, panel));
		
		return tags;
	}
	
	public void refresh() {
		gui.whenComplete(gui.getConnection().getEntries(filter), entries -> {
			this.entries.removeAll();
			
			for (NBTEntry entry : entries) {
				JPanel tagsPanel = addEntry(entry);
				
				gui.whenComplete(gui.getConnection().getTags(new TagFilter().filterByEntryId(entry.id)), tags -> {
					for (Tag tag : tags) {
						if (tagsPanel.getComponentCount() == 0)
							tagsPanel.setBorder(new EmptyBorder(0, 0, 4, 0));
						else
							tagsPanel.add(Box.createRigidArea(new Dimension(4, 0)));
						tagsPanel.add(GUIUtil.createTag(tag));
					}
					
					tagsPanel.revalidate();
					tagsPanel.repaint();
				});
			}
			
			this.entries.revalidate();
			this.entries.repaint();
		});
	}
	
	private void advancedSearchBtn() {
		gui.whenComplete(gui.getConnection().getTags(new TagFilter()), tags -> {
			JPanel panel = new JPanel(TableLayout.ofColumns(2, 4));
			
			panel.add(new JLabel("Min Data Version:"));
			
			JTextField minDataVersionField = new JTextField();
			panel.add(minDataVersionField);
			minDataVersionField.setPreferredSize(new Dimension(200, minDataVersionField.getPreferredSize().height));
			if (filter.getMinDataVersion() != null)
				minDataVersionField.setText(DataVersion.toString(filter.getMinDataVersion()));
			
			panel.add(new JLabel("Max Data Version:"));
			
			JTextField maxDataVersionField = new JTextField();
			panel.add(maxDataVersionField);
			if (filter.getMaxDataVersion() != null)
				maxDataVersionField.setText(DataVersion.toString(filter.getMaxDataVersion()));
			
			panel.add(new JLabel("Author UUID:"));
			
			JTextField authorUuidField = new JTextField();
			panel.add(authorUuidField);
			if (filter.getAuthorUuid() != null)
				authorUuidField.setText(filter.getAuthorUuid().toString());
			
			panel.add(new JLabel("Author Username:"));
			
			JTextField authorUsernameField = new JTextField();
			panel.add(authorUsernameField);
			if (filter.getAuthorName() != null)
				authorUsernameField.setText(filter.getAuthorName());
			
			panel.add(new JLabel("Tags:"));
			
			Map<String, JCheckBox> tagFields = new HashMap<>();
			if (tags.isEmpty()) {
				panel.add(new JLabel("There are no tags"));
			} else {
				panel.add(new JLabel());
				
				for (Tag tag : tags) {
					panel.add(GUIUtil.createTag(tag));
					
					JCheckBox tagField = new JCheckBox();
					tagFields.put(tag.name, tagField);
					panel.add(tagField);
					if (filter.getTags() != null && filter.getTags().contains(tag.name))
						tagField.setSelected(true);
				}
			}
			
			if (JOptionPane.showConfirmDialog(frame, panel, "Advanced Search", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
				return;
			
			String nameField = filter.getName();
			filter = new EntryFilter();
			filter.filterByName(nameField);
			
			if (!minDataVersionField.getText().isEmpty()) {
				try {
					filter.filterByMinDataVersion(new DataVersionInput().parse(minDataVersionField.getText()));
				} catch (CommandParseException e) {
					JOptionPane.showMessageDialog(frame, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
			
			if (!maxDataVersionField.getText().isEmpty()) {
				try {
					filter.filterByMaxDataVersion(new DataVersionInput().parse(maxDataVersionField.getText()));
				} catch (CommandParseException e) {
					JOptionPane.showMessageDialog(frame, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
			
			if (!authorUuidField.getText().isEmpty()) {
				try {
					filter.filterByAuthorUuid(new UUIDInput().parse(authorUuidField.getText()));
				} catch (CommandParseException e) {
					JOptionPane.showMessageDialog(frame, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
			
			if (!authorUsernameField.getText().isEmpty())
				filter.filterByAuthorName(authorUsernameField.getText());
			
			filter.filterByTags(tagFields.entrySet().stream()
					.filter(entry -> entry.getValue().isSelected()).map(Map.Entry::getKey).collect(Collectors.toSet()));
			
			refresh();
		});
	}
	
	private void editEntryBtn(NBTEntry previousEntry, Set<String> previousTags) {
		gui.whenComplete(gui.getConnection().getTags(new TagFilter()), tags -> {
			JPanel panel = new JPanel(TableLayout.ofColumns(2, 4));
			
			panel.add(new JLabel("Name:"));
			
			JTextField nameField = new JTextField();
			panel.add(nameField);
			nameField.setPreferredSize(new Dimension(200, nameField.getPreferredSize().height));
			
			panel.add(new JLabel("File:"));
			
			JButton selectFileBtn = new JButton("Select File");
			panel.add(selectFileBtn);
			AtomicReference<File> selectFileField = new AtomicReference<>();
			selectFileBtn.addActionListener(event -> {
				JnaFileChooser chooser = new JnaFileChooser(".");
				chooser.setTitle("Select NBT File");
				chooser.addFilter("Named Binary Tag (*.nbt)", "nbt");
				chooser.addFilter("All Files (*.*)", "*");
				if (!chooser.showOpenDialog(frame))
					return;
				File file = chooser.getSelectedFile();
				
				selectFileField.set(file);
				selectFileBtn.setText(file.getName());
				if (nameField.getText().isEmpty()) {
					nameField.setText(file.getName().endsWith(".nbt") ?
							file.getName().substring(0, file.getName().length() - ".nbt".length()) : file.getName());
				}
			});
			
			panel.add(new JLabel("Author UUID:"));
			
			JTextField authorUuidField = new JTextField();
			panel.add(authorUuidField);
			
			panel.add(new JLabel("Author Username:"));
			
			JTextField authorUsernameField = new JTextField();
			panel.add(authorUsernameField);
			
			panel.add(new JLabel("Verified:"));
			
			JCheckBox verifiedField = new JCheckBox();
			panel.add(verifiedField);
			
			panel.add(new JLabel("Tags:"));
			
			Map<String, JCheckBox> tagFields = new HashMap<>();
			if (tags.isEmpty()) {
				panel.add(new JLabel("There are no tags"));
			} else {
				panel.add(new JLabel());
				
				for (Tag tag : tags) {
					panel.add(GUIUtil.createTag(tag));
					
					JCheckBox tagField = new JCheckBox();
					tagFields.put(tag.name, tagField);
					panel.add(tagField);
				}
			}
			
			if (previousEntry != null) {
				nameField.setText(previousEntry.name);
				authorUuidField.setText(previousEntry.authorUuid.toString());
				authorUsernameField.setText(previousEntry.authorUsername);
				verifiedField.setSelected(previousEntry.verified);
				for (String tag : previousTags)
					tagFields.get(tag).setSelected(true);
			}
			
			if (JOptionPane.showConfirmDialog(frame, panel,
					previousEntry == null ? "Add Entry" : "Edit Entry: " + previousEntry.name,
					JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
				return;
			}
			
			File file = selectFileField.get();
			Integer dataVersion;
			byte[] nbt;
			if (file == null) {
				if (previousEntry == null) {
					JOptionPane.showMessageDialog(frame, "You must select a file!", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				} else {
					dataVersion = null;
					nbt = null;
				}
			} else {
				if (!file.exists()) {
					JOptionPane.showMessageDialog(frame, "'" + file.getName() + "' doesn't exist!", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				NamedTag rootTag;
				CompoundTag rootValue;
				try {
					rootTag = NBTUtil.read(file);
					if (rootTag.getTag() instanceof CompoundTag)
						rootValue = (CompoundTag) rootTag.getTag();
					else
						throw new IOException();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(frame, "'" + file.getName() + "' isn't a valid NBT file!", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				if (rootValue.containsKey("DataVersion") && rootValue.get("DataVersion") instanceof IntTag)
					dataVersion = rootValue.getInt("DataVersion");
				else {
					String dataVersionStr = JOptionPane.showInputDialog(frame, "Enter data version:", "Add Entry", JOptionPane.QUESTION_MESSAGE);
					if (dataVersionStr == null)
						return;
					try {
						dataVersion = new DataVersionInput().parse(dataVersionStr);
					} catch (CommandParseException e) {
						JOptionPane.showMessageDialog(frame, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				
				try {
					nbt = new NBTSerializer(true).toBytes(rootTag);
				} catch (IOException e) {
					// Impossible
					throw new RuntimeException("Failed to serialize NBT", e);
				}
			}
			
			UUID authorUuid;
			try {
				authorUuid = new UUIDInput().parse(authorUuidField.getText());
			} catch (CommandParseException e) {
				JOptionPane.showMessageDialog(frame, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			if (previousEntry == null) {
				gui.whenComplete(gui.getConnection().addEntry(nameField.getText(), nbt, dataVersion,
						authorUuid, authorUsernameField.getText(), verifiedField.isSelected()), id -> {
					gui.whenComplete(Util.allOf(tagFields.entrySet().stream()
							.filter(entry -> entry.getValue().isSelected())
							.map(entry -> gui.getConnection().addTagToEntry(id, entry.getKey()))
							.toArray(CompletableFuture[]::new)), v -> refresh());
				});
			} else {
				Runnable editTags = () -> {
					List<CompletableFuture<Void>> tagFutures = new ArrayList<>();
					for (Map.Entry<String, JCheckBox> tag : tagFields.entrySet()) {
						if (previousTags.contains(tag.getKey()) != tag.getValue().isSelected()) {
							if (tag.getValue().isSelected())
								tagFutures.add(gui.getConnection().addTagToEntry(previousEntry.id, tag.getKey()));
							else
								tagFutures.add(gui.getConnection().removeTagFromEntry(previousEntry.id, tag.getKey()));
						}
					}
					gui.whenComplete(Util.allOf(tagFutures.toArray(new CompletableFuture[tagFutures.size()])), v2 -> refresh());
				};
				Optional<String> nameEdit = Util.edit(previousEntry.name, nameField.getText());
				Optional<byte[]> nbtEdit = Util.edit(previousEntry.nbt, nbt);
				Optional<Integer> dataVersionEdit = Util.edit(previousEntry.dataVersion, dataVersion);
				Optional<UUID> authorUuidEdit = Util.edit(previousEntry.authorUuid, authorUuid);
				Optional<String> authorUsernameEdit = Util.edit(previousEntry.authorUsername, authorUsernameField.getText());
				Optional<Boolean> verifiedEdit = Util.edit(previousEntry.verified, verifiedField.isSelected());
				if (nameEdit.isPresent() || nbtEdit.isPresent() || dataVersionEdit.isPresent() ||
						authorUuidEdit.isPresent() || authorUsernameEdit.isPresent() || verifiedEdit.isPresent()) {
					gui.whenComplete(gui.getConnection().editEntry(previousEntry.id, nameEdit, nbtEdit, dataVersionEdit,
							authorUuidEdit, authorUsernameEdit, verifiedEdit), v -> editTags.run());
				} else {
					editTags.run();
				}
			}
		});
	}
	
	private void detailsEntryBtn(NBTEntry entry) {
		JPanel panel = new JPanel(TableLayout.ofColumns(2, 4));
		
		panel.add(new JLabel("ID:"));
		
		panel.add(new JLabel(entry.id + ""));
		
		panel.add(new JLabel("Hash:"));
		
		panel.add(new JLabel(entry.hash));
		
		JOptionPane.showMessageDialog(frame, panel, "Entry Details: " + entry.name, JOptionPane.INFORMATION_MESSAGE);
	}
	
	private void exportEntryBtn(String name, byte[] nbt) {
		JnaFileChooser chooser = new JnaFileChooser(".");
		chooser.setTitle("Export NBT Entry");
		chooser.setDefaultFileName(name + ".nbt");
		chooser.addFilter("Named Binary Tag (*.nbt)", "nbt");
		chooser.addFilter("All Files (*.*)", "*");
		if (!chooser.showSaveDialog(frame))
			return;
		File file = chooser.getSelectedFile();
		
		if (file.exists()) {
			if (JOptionPane.showConfirmDialog(frame, "'" + file.getName() + "' already exists. Overwrite?",
					"Export NBT Entry", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
				return;
			}
		}
		
		try {
			Files.write(file.toPath(), nbt);
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Failed to export to '" + file.getName() + "'", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void removeEntryBtn(long id, String name, JPanel panel) {
		if (JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete the entry '" + name + "'?",
				"Remove Entry", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
			return;
		}
		
		gui.whenComplete(gui.getConnection().removeEntry(id), v -> {
			entries.remove(panel);
			entries.revalidate();
			entries.repaint();
		});
	}
	
}
