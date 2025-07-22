package com.luneruniverse.minecraft.nbtdatabase.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JColorChooser;
import javax.swing.JComponent;

@SuppressWarnings("serial")
public class JColorField extends JComponent {
	
	private int color;
	
	public JColorField() {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Color newColor = JColorChooser.showDialog(JColorField.this, "", new Color(color));
				if (newColor != null) {
					JColorField.this.color = newColor.getRGB() & 0xFFFFFF;
					repaint();
				}
			}
		});
	}
	
	public void setColor(int color) {
		this.color = color;
	}
	
	public int getColor() {
		return color;
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(20, 20);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		g.setColor(new Color(color));
		g.fillRect(0, 0, getWidth(), getHeight());
	}
	
}
