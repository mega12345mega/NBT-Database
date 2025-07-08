package com.luneruniverse.minecraft.nbtdatabase.gui;

import java.awt.Dimension;
import java.util.Optional;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.TagFilter;
import com.luneruniverse.minecraft.nbtdatabase.Util;

public class TagsTab {
	
	private final GUI gui;
	private final JFrame frame;
	private final JPanel table;
	private TagFilter filter;
	
	public TagsTab(GUI gui, JFrame frame, JPanel panel) {
		this.gui = gui;
		this.frame = frame;
		
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(new EmptyBorder(4, 4, 4, 4));
		
		JPanel options = new JPanel();
		options.setLayout(new BoxLayout(options, BoxLayout.X_AXIS));
		panel.add(options);
		
		JTextField nameFilterField = new JTextField();
		options.add(nameFilterField);
		nameFilterField.setMaximumSize(new Dimension(nameFilterField.getMaximumSize().width, nameFilterField.getPreferredSize().height));
		GUIUtil.addTextFieldChangeListener(nameFilterField, text -> filter.filterByName(text.isEmpty() ? null : text));
		nameFilterField.addActionListener(event -> refresh());
		
		options.add(Box.createRigidArea(new Dimension(4, 0)));
		
		JButton addTagBtn = new JButton("Add Tag");
		options.add(addTagBtn);
		addTagBtn.addActionListener(event -> editTagBtn(null));
		
		panel.add(Box.createVerticalStrut(4));
		
		table = new JPanel(TableLayout.ofColumns(3, 4));
		panel.add(table);
		
		filter = new TagFilter();
	}
	
	private void addTagEntry(Tag tag) {
		table.add(GUIUtil.createTag(tag));
		
		JButton editTagBtn = new JButton("Edit");
		table.add(editTagBtn);
		editTagBtn.addActionListener(event -> editTagBtn(tag));
		
		JButton removeTagBtn = new JButton("-");
		table.add(removeTagBtn);
		removeTagBtn.addActionListener(event -> removeTagBtn(tag.name));
	}
	
	public void refresh() {
		gui.whenComplete(gui.getConnection().getTags(filter), tags -> {
			table.removeAll();
			
			for (Tag tag : tags)
				addTagEntry(tag);
			
			table.revalidate();
			table.repaint();
		});
	}
	
	private void editTagBtn(Tag previousTag) {
		JPanel panel = new JPanel(TableLayout.ofColumns(2, 4));
		
		panel.add(new JLabel("Name"));
		
		JTextField nameField = new JTextField();
		panel.add(nameField);
		
		panel.add(new JLabel("Color"));
		
		JColorField colorField = new JColorField();
		panel.add(colorField);
		
		if (previousTag != null) {
			nameField.setText(previousTag.name);
			colorField.setColor(previousTag.color);
		}
		
		if (JOptionPane.showConfirmDialog(frame, panel, previousTag == null ? "Add Tag" : "Edit Tag: " + previousTag.name,
				JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
			return;
		}
		
		if (previousTag == null)
			gui.whenComplete(gui.getConnection().addTag(nameField.getText(), colorField.getColor()), v -> refresh());
		else {
			Optional<String> nameEdit = Util.edit(previousTag.name, nameField.getText());
			Optional<Integer> colorEdit = Util.edit(previousTag.color, colorField.getColor());
			if (nameEdit.isPresent() || colorEdit.isPresent())
				gui.whenComplete(gui.getConnection().editTag(previousTag.name, nameEdit, colorEdit), v -> refresh());
		}
	}
	
	private void removeTagBtn(String name) {
		if (JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete the tag '" + name + "'?",
				"Remove Tag", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
			return;
		}
		
		gui.whenComplete(gui.getConnection().removeTag(name), v -> refresh());
	}
	
}
