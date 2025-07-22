package com.luneruniverse.minecraft.nbtdatabase.gui;

import java.awt.Color;
import java.awt.Font;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.luneruniverse.minecraft.nbtdatabase.Tag;

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
		JLabel label = new JLabel(tag.getName());
		
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		label.setForeground(tag.isTextColorWhite() ? Color.WHITE : Color.BLACK);
		
		label.setOpaque(true);
		label.setBackground(new Color(tag.getColor()));
		label.setBorder(new EmptyBorder(4, 4, 4, 4));
		
		return label;
	}
	
	public static <T> Optional<T> edit(T originalValue, T newValue) {
		if (newValue == null || originalValue.equals(newValue))
			return Optional.empty();
		return Optional.of(newValue);
	}
	
}
