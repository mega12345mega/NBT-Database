package com.luneruniverse.minecraft.nbtdatabase.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.TagFilter;

public class TagsTab {
	
	private final GUI gui;
	private final JFrame frame;
	private final JPanel table;
	private final JPanel nameColumn;
	private final JPanel colorColumn;
	private final JPanel addRemoveColumn;
	private final JTextField nameAddTagField;
	private final ColorButton colorAddTagField;
	
	public TagsTab(GUI gui, JFrame frame, JPanel panel) {
		this.gui = gui;
		this.frame = frame;
		
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		
		table = new JPanel();
		table.setLayout(new BoxLayout(table, BoxLayout.X_AXIS));
		panel.add(table);
		
		nameColumn = new JPanel(new GridLayout(0, 1, 0, 4));
		table.add(nameColumn);
		
		table.add(Box.createHorizontalStrut(4));
		
		colorColumn = new JPanel(new GridLayout(0, 1, 0, 4));
		table.add(colorColumn);
		
		table.add(Box.createHorizontalStrut(4));
		
		addRemoveColumn = new JPanel(new GridLayout(0, 1, 0, 4));
		table.add(addRemoveColumn);
		
		nameColumn.add(new JLabel("Name"));
		colorColumn.add(new JLabel("Color"));
		addRemoveColumn.add(new JLabel());
		
		nameAddTagField = new JTextField();
		nameColumn.add(nameAddTagField);
		nameAddTagField.setPreferredSize(new Dimension(100, nameAddTagField.getPreferredSize().width));
		
		colorAddTagField = new ColorButton(0x000000);
		colorColumn.add(colorAddTagField);
		
		JButton addTagBtn = new JButton("+");
		addRemoveColumn.add(addTagBtn);
		addTagBtn.addActionListener(event -> addTagBtn());
	}
	
	private void addTagEntry(String name, int color) {
		nameColumn.add(new JLabel(name));
		colorColumn.add(new ColorButton(color, this::setTagColorBtn));
		
		JButton removeTagBtn = new JButton("-");
		addRemoveColumn.add(removeTagBtn);
		removeTagBtn.addActionListener(event -> removeTagBtn(name));
	}
	
	public void refresh() {
		gui.whenComplete(gui.getConnection().getTags(new TagFilter()), tags -> {
			while (nameColumn.getComponentCount() > 2)
				nameColumn.remove(2);
			while (colorColumn.getComponentCount() > 2)
				colorColumn.remove(2);
			while (addRemoveColumn.getComponentCount() > 2)
				addRemoveColumn.remove(2);
			
			for (Tag tag : tags)
				addTagEntry(tag.name, tag.color);
			
			table.revalidate();
			table.repaint();
		});
	}
	
	private void addTagBtn() {
		String name = nameAddTagField.getText();
		int color = colorAddTagField.getColor();
		
		nameAddTagField.setText("");
		
		gui.whenComplete(gui.getConnection().addTag(name, color), v -> {
			addTagEntry(name, color);
			
			table.revalidate();
			table.repaint();
		});
	}
	
	private void removeTagBtn(String name) {
		if (JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete the tag '" + name + "'?",
				"Remove Tag", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
			return;
		}
		
		gui.whenComplete(gui.getConnection().removeTag(name), v -> {
			int index = 2;
			while (!((JLabel) nameColumn.getComponent(index)).getText().equals(name))
				index++;
			
			nameColumn.remove(index);
			colorColumn.remove(index);
			addRemoveColumn.remove(index);
			
			table.revalidate();
			table.repaint();
		});
	}
	
	private void setTagColorBtn(int color) {
		// TODO
	}
	
}
