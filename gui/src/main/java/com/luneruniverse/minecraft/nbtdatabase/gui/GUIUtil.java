package com.luneruniverse.minecraft.nbtdatabase.gui;

import java.awt.Color;
import java.awt.Font;
import java.util.function.Consumer;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.Util;

public class GUIUtil {
	
	public static void addTextFieldChangeListener(JTextField textField, Consumer<String> listener) {
		textField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent event) {
				listener.accept(textField.getText());
			}
			@Override
			public void removeUpdate(DocumentEvent event) {
				listener.accept(textField.getText());
			}
			@Override
			public void changedUpdate(DocumentEvent event) {
				listener.accept(textField.getText());
			}
		});
	}
	
	public static JLabel createTag(Tag tag) {
		Color color = new Color(tag.getColor());
		
		JLabel label = new JLabel(tag.getName());
		
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		label.setForeground(Util.isColorBright(color) ? Color.BLACK : Color.WHITE);
		
		label.setOpaque(true);
		label.setBackground(color);
		label.setBorder(new EmptyBorder(4, 4, 4, 4));
		
		return label;
	}
	
}
