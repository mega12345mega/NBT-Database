package com.luneruniverse.minecraft.nbtdatabase.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class UIUtil {
	
	public static String formatTimestamp(long utcMillis) {
		return Instant.ofEpochMilli(utcMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.RFC_1123_DATE_TIME);
	}
	
	public static <T> Optional<T> edit(T originalValue, T newValue) {
		if (newValue == null || originalValue.equals(newValue))
			return Optional.empty();
		return Optional.of(newValue);
	}
	
}
