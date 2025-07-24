package com.luneruniverse.minecraft.nbtdatabase.ui.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
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
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import com.luneruniverse.minecraft.nbtdatabase.DataVersion;
import com.luneruniverse.minecraft.nbtdatabase.Entry;
import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.FutureUtil;
import com.luneruniverse.minecraft.nbtdatabase.request.EntryFilter;
import com.luneruniverse.minecraft.nbtdatabase.request.EntryView;
import com.luneruniverse.minecraft.nbtdatabase.request.TagFilter;
import com.luneruniverse.minecraft.nbtdatabase.ui.DataVersionInput;
import com.luneruniverse.minecraft.nbtdatabase.ui.UIUtil;
import com.luneruniverse.minecraft.nbtdatabase.ui.UUIDInput;
import com.luneruniverse.simplecli.CommandParseException;

import jnafilechooser.api.JnaFileChooser;
import net.querz.nbt.io.NBTSerializer;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.IntTag;
import net.querz.nbt.tag.StringTag;

public class EntriesTab {
	
	private final GUI gui;
	private final JFrame frame;
	private final JPanel entries;
	private EntryFilter filter;
	private EntryView view;
	
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
		view = new EntryView();
	}
	
	private void addEntry(Entry entry) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		entries.add(panel);
		panel.setAlignmentX(0);
		
		TitledBorder border = new TitledBorder(entry.getName() + (entry.isVerified() ? " ✔" : ""));
		panel.setBorder(border);
		border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD));
		if (entry.isVerified())
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
		gui.whenComplete(gui.getConnection().getTags(new TagFilter().filterByEntryId(entry.getId())), tags2 -> {
			for (Tag tag : tags2) {
				if (tags.getComponentCount() == 0)
					tags.setBorder(new EmptyBorder(0, 0, 4, 0));
				else
					tags.add(Box.createRigidArea(new Dimension(4, 0)));
				tags.add(GUIUtil.createTag(tag));
			}
			tags.revalidate();
			tags.repaint();
		});
		
		JLabel author = new JLabel("Author: " + entry.getAuthorUsername());
		author.setToolTipText("UUID: " + entry.getAuthorUuid());
		details.add(author);
		
		details.add(new JLabel("Type: " + entry.getType()));
		
		details.add(new JLabel("Data Version: " + DataVersion.toViewableString(entry.getDataVersion())));
		
		details.add(new JLabel("Bytes: " + String.format("%,d", entry.getNbtLength())));
		
		JLabel created = new JLabel("Created: " + UIUtil.formatTimestamp(entry.getCreated()));
		if (entry.getCreated() == entry.getModified())
			created.setToolTipText("Never Modified");
		else
			created.setToolTipText("Modified: " + UIUtil.formatTimestamp(entry.getModified()));
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
		exportEntryBtn.addActionListener(event -> exportEntryBtn(entry.getId(), entry.getName()));
		
		JButton editEntryBtn = new JButton("Edit");
		options.add(editEntryBtn);
		editEntryBtn.addActionListener(event -> {
			gui.whenComplete(gui.getConnection().getTags(new TagFilter().filterByEntryId(entry.getId())),
					previousTags -> editEntryBtn(entry, previousTags.stream().map(Tag::getName).collect(Collectors.toSet())));
		});
		
		JButton removeEntryBtn = new JButton("-");
		options.add(removeEntryBtn);
		removeEntryBtn.addActionListener(event -> removeEntryBtn(entry.getId(), entry.getName(), panel));
	}
	
	public void refresh() {
		view.setOffset(0);
		
		gui.whenComplete(gui.getConnection().getEntries(filter, view), entries -> {
			this.entries.removeAll();
			
			if (entries.isEmpty()) {
				JLabel noEntriesLabel = new JLabel("No entries found");
				noEntriesLabel.setBorder(new EmptyBorder(4, 4, 4, 4));
				this.entries.add(noEntriesLabel);
			}
			for (Entry entry : entries)
				addEntry(entry);
			
			view.setOffset(entries.size());
			
			JButton loadMoreBtn = new JButton("Load More");
			this.entries.add(loadMoreBtn);
			loadMoreBtn.addActionListener(event -> {
				gui.whenComplete(gui.getConnection().getEntries(filter, view), entries2 -> {
					if (entries2.isEmpty()) {
						JOptionPane.showMessageDialog(frame, "There are no more entries", "Load More", JOptionPane.INFORMATION_MESSAGE);
						return;
					}
					
					for (Entry entry : entries2)
						addEntry(entry);
					
					view.setOffset(view.getOffset() + entries2.size());
					
					// Move to end
					this.entries.add(loadMoreBtn);
					
					this.entries.revalidate();
					this.entries.repaint();
				});
			});
			
			this.entries.revalidate();
			this.entries.repaint();
		});
	}
	
	private void advancedSearchBtn() {
		gui.whenComplete(gui.getConnection().getTags(new TagFilter()), tags -> {
			JPanel panel = new JPanel(TableLayout.ofColumns(2, 4));
			
			panel.add(new JLabel("Order:"));
			
			JComboBox<EntryView.Order> orderField = new JComboBox<>(EntryView.Order.values());
			panel.add(orderField);
			orderField.setSelectedItem(view.getOrder());
			
			panel.add(new JLabel("Reversed Order:"));
			
			JCheckBox reversedOrderField = new JCheckBox();
			panel.add(reversedOrderField);
			reversedOrderField.setSelected(view.isReversedOrder());
			
			panel.add(new JSeparator());
			
			panel.add(new JSeparator());
			
			panel.add(new JLabel("Min Bytes:"));
			
			JSpinner minNbtLengthField = new JSpinner();
			panel.add(minNbtLengthField);
			minNbtLengthField.setPreferredSize(new Dimension(200, minNbtLengthField.getPreferredSize().height));
			((SpinnerNumberModel) minNbtLengthField.getModel()).setMinimum(0);
			if (filter.getMinNbtLength() == null)
				minNbtLengthField.setValue(0);
			else
				minNbtLengthField.setValue(filter.getMinNbtLength());
			
			panel.add(new JLabel("Max Bytes:"));
			
			JSpinner maxNbtLengthField = new JSpinner();
			panel.add(maxNbtLengthField);
			((SpinnerNumberModel) maxNbtLengthField.getModel()).setMinimum(0);
			if (filter.getMaxNbtLength() == null)
				maxNbtLengthField.setValue(Integer.MAX_VALUE);
			else
				maxNbtLengthField.setValue(filter.getMaxNbtLength());
			
			panel.add(new JLabel("Type:"));
			
			JComboBox<Object> typeField = new JComboBox<>();
			panel.add(typeField);
			typeField.addItem("");
			for (Entry.Type type : Entry.Type.values())
				typeField.addItem(type);
			if (filter.getType() != null)
				typeField.setSelectedItem(filter.getType());
			
			panel.add(new JLabel("Min Data Version:"));
			
			JTextField minDataVersionField = new JTextField();
			panel.add(minDataVersionField);
			if (filter.getMinDataVersion() != null)
				minDataVersionField.setText(DataVersion.toParsableString(filter.getMinDataVersion()));
			
			panel.add(new JLabel("Max Data Version:"));
			
			JTextField maxDataVersionField = new JTextField();
			panel.add(maxDataVersionField);
			if (filter.getMaxDataVersion() != null)
				maxDataVersionField.setText(DataVersion.toParsableString(filter.getMaxDataVersion()));
			
			panel.add(new JLabel("Author UUID:"));
			
			JTextField authorUuidField = new JTextField();
			panel.add(authorUuidField);
			if (filter.getAuthorUuid() != null)
				authorUuidField.setText(filter.getAuthorUuid().toString());
			
			panel.add(new JLabel("Author Username:"));
			
			JTextField authorUsernameField = new JTextField();
			panel.add(authorUsernameField);
			if (filter.getAuthorUsername() != null)
				authorUsernameField.setText(filter.getAuthorUsername());
			
			panel.add(new JLabel("Tags:"));
			
			Map<String, JCheckBox> tagFields = new HashMap<>();
			if (tags.isEmpty()) {
				panel.add(new JLabel("There are no tags"));
			} else {
				panel.add(new JLabel());
				
				for (Tag tag : tags) {
					panel.add(GUIUtil.createTag(tag));
					
					JCheckBox tagField = new JCheckBox();
					tagFields.put(tag.getName(), tagField);
					panel.add(tagField);
					if (filter.getTags() != null && filter.getTags().contains(tag.getName()))
						tagField.setSelected(true);
				}
			}
			
			if (JOptionPane.showConfirmDialog(frame, panel, "Advanced Search", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
				return;
			
			view.setOrder((EntryView.Order) orderField.getSelectedItem()).setReversedOrder(reversedOrderField.isSelected());
			
			String nameField = filter.getName();
			filter = new EntryFilter();
			filter.filterByName(nameField);
			
			if ((int) minNbtLengthField.getValue() > 0)
				filter.filterByMinNbtLength((int) minNbtLengthField.getValue());
			
			if ((int) maxNbtLengthField.getValue() < Integer.MAX_VALUE)
				filter.filterByMaxNbtLength((int) maxNbtLengthField.getValue());
			
			if (typeField.getSelectedIndex() != 0)
				filter.filterByType((Entry.Type) typeField.getSelectedItem());
			
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
				filter.filterByAuthorUsername(authorUsernameField.getText());
			
			filter.filterByTags(tagFields.entrySet().stream()
					.filter(entry -> entry.getValue().isSelected()).map(Map.Entry::getKey).collect(Collectors.toSet()));
			
			refresh();
		});
	}
	
	private void editEntryBtn(Entry previousEntry, Set<String> previousTags) {
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
			verifiedField.setSelected(true);
			
			panel.add(new JLabel("Tags:"));
			
			Map<String, JCheckBox> tagFields = new HashMap<>();
			if (tags.isEmpty()) {
				panel.add(new JLabel("There are no tags"));
			} else {
				panel.add(new JLabel());
				
				for (Tag tag : tags) {
					panel.add(GUIUtil.createTag(tag));
					
					JCheckBox tagField = new JCheckBox();
					tagFields.put(tag.getName(), tagField);
					panel.add(tagField);
				}
			}
			
			if (previousEntry != null) {
				nameField.setText(previousEntry.getName());
				authorUuidField.setText(previousEntry.getAuthorUuid().toString());
				authorUsernameField.setText(previousEntry.getAuthorUsername());
				verifiedField.setSelected(previousEntry.isVerified());
				for (String tag : previousTags)
					tagFields.get(tag).setSelected(true);
			}
			
			String dialogTitle = previousEntry == null ? "Add Entry" : "Edit Entry: " + previousEntry.getName();
			if (JOptionPane.showConfirmDialog(frame, panel, dialogTitle, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
				return;
			
			File file = selectFileField.get();
			Entry.Type type;
			Integer dataVersion;
			byte[] nbt;
			if (file == null) {
				if (previousEntry == null) {
					JOptionPane.showMessageDialog(frame, "You must select a file!", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				} else {
					type = null;
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
				
				if (rootValue.containsKey("type") && rootValue.get("type") instanceof StringTag)
					type = Entry.Type.fromNBT(rootValue.getString("type"));
				else {
					JPanel typePanel = new JPanel(new GridLayout(0, 1));
					typePanel.add(new JLabel("Enter type:"));
					JComboBox<Entry.Type> typeField = new JComboBox<>(Entry.Type.values());
					typePanel.add(typeField);
					if (JOptionPane.showConfirmDialog(frame, typePanel, dialogTitle, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
						return;
					type = (Entry.Type) typeField.getSelectedItem();
				}
				
				if (rootValue.containsKey("DataVersion") && rootValue.get("DataVersion") instanceof IntTag)
					dataVersion = rootValue.getInt("DataVersion");
				else {
					String dataVersionStr = JOptionPane.showInputDialog(frame, "Enter data version:", dialogTitle, JOptionPane.QUESTION_MESSAGE);
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
				gui.whenComplete(gui.getConnection().addEntry(nameField.getText(), nbt, type, dataVersion,
						authorUuid, authorUsernameField.getText(), verifiedField.isSelected()), id -> {
					gui.whenComplete(FutureUtil.allOf(tagFields.entrySet().stream()
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
								tagFutures.add(gui.getConnection().addTagToEntry(previousEntry.getId(), tag.getKey()));
							else
								tagFutures.add(gui.getConnection().removeTagFromEntry(previousEntry.getId(), tag.getKey()));
						}
					}
					gui.whenComplete(FutureUtil.allOf(tagFutures.toArray(new CompletableFuture[tagFutures.size()])), v2 -> refresh());
				};
				Optional<String> nameEdit = UIUtil.edit(previousEntry.getName(), nameField.getText());
				Optional<byte[]> nbtEdit = Optional.ofNullable(nbt);
				Optional<Entry.Type> typeEdit = UIUtil.edit(previousEntry.getType(), type);
				Optional<Integer> dataVersionEdit = UIUtil.edit(previousEntry.getDataVersion(), dataVersion);
				Optional<UUID> authorUuidEdit = UIUtil.edit(previousEntry.getAuthorUuid(), authorUuid);
				Optional<String> authorUsernameEdit = UIUtil.edit(previousEntry.getAuthorUsername(), authorUsernameField.getText());
				Optional<Boolean> verifiedEdit = UIUtil.edit(previousEntry.isVerified(), verifiedField.isSelected());
				if (nameEdit.isPresent() || nbtEdit.isPresent() || dataVersionEdit.isPresent() ||
						authorUuidEdit.isPresent() || authorUsernameEdit.isPresent() || verifiedEdit.isPresent()) {
					gui.whenComplete(gui.getConnection().editEntry(previousEntry.getId(), nameEdit, nbtEdit, typeEdit,
							dataVersionEdit, authorUuidEdit, authorUsernameEdit, verifiedEdit), v -> editTags.run());
				} else {
					editTags.run();
				}
			}
		});
	}
	
	private void detailsEntryBtn(Entry entry) {
		JPanel panel = new JPanel(TableLayout.ofColumns(2, 4));
		
		panel.add(new JLabel("ID:"));
		
		panel.add(new JLabel(entry.getId() + ""));
		
		panel.add(new JLabel("Hash:"));
		
		panel.add(new JLabel(entry.getHash()));
		
		JOptionPane.showMessageDialog(frame, panel, "Entry Details: " + entry.getName(), JOptionPane.INFORMATION_MESSAGE);
	}
	
	private void exportEntryBtn(long id, String name) {
		gui.whenComplete(gui.getConnection().getEntryNBT(id), nbt -> {
			if (nbt == null) {
				JOptionPane.showMessageDialog(frame, "Entry doesn't exist: " + id, "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
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
		});
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
