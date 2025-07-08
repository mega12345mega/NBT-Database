package com.luneruniverse.minecraft.nbtdatabase.gui;

import java.awt.FlowLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class MetadataTab {
	
	private final GUI gui;
	private final JLabel maxNbtSize;
	private final JLabel maxNumResults;
	
	public MetadataTab(GUI gui, JFrame frame, JPanel panel) {
		this.gui = gui;
		
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		
		JPanel table = new JPanel(TableLayout.ofColumns(2, 4));
		panel.add(table);
		
		table.add(new JLabel("Max NBT Size:"));
		
		maxNbtSize = new JLabel();
		table.add(maxNbtSize);
		
		table.add(new JLabel("Max Num Results:"));
		
		maxNumResults = new JLabel();
		table.add(maxNumResults);
	}
	
	public void refresh() {
		gui.whenComplete(gui.getConnection().getMetadata(), metadata -> {
			maxNbtSize.setText(String.format("%,d", metadata.getMaxNbtSize()));
			maxNumResults.setText(String.format("%,d", metadata.getMaxNumResults()));
		});
	}
	
}
