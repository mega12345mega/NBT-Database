package com.luneruniverse.minecraft.nbtdatabase.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.JColorChooser;
import javax.swing.JComponent;

@SuppressWarnings("serial")
public class ColorButton extends JComponent {
	
	private int color;
	
	public ColorButton(int color, Consumer<Integer> onColorChange) {
		this.color = color;
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Color newColor = JColorChooser.showDialog(ColorButton.this, "", new Color(color));
				if (newColor != null) {
					ColorButton.this.color = newColor.getRGB() & 0xFFFFFF;
					onColorChange.accept(ColorButton.this.color);
					repaint();
				}
			}
		});
	}
	public ColorButton(int color) {
		this(color, newColor -> {});
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
