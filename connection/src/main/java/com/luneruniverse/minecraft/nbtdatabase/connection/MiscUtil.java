package com.luneruniverse.minecraft.nbtdatabase.connection;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MiscUtil {
	
	public static String formatTimestamp(long utcMillis) {
		return Instant.ofEpochMilli(utcMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.RFC_1123_DATE_TIME);
	}
	
}
