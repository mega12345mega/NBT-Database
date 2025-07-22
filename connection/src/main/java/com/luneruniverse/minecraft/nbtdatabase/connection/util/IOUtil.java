package com.luneruniverse.minecraft.nbtdatabase.connection.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class IOUtil {
	
	public static byte[] readAllBytes(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int numRead;
		while ((numRead = in.read(buf)) != -1)
			out.write(buf, 0, numRead);
		return out.toByteArray();
	}
	
	public static byte[] readAllBytesAndClose(InputStream in) throws IOException {
		try {
			return readAllBytes(in);
		} finally {
			in.close();
		}
	}
	
	public static byte[] readAllBytesAndCloseOrNull(InputStream in) {
		try {
			return readAllBytesAndClose(in);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static String readStringAndCloseOrNull(InputStream in) {
		byte[] bytes = readAllBytesAndCloseOrNull(in);
		if (bytes == null)
			return null;
		return new String(bytes, StandardCharsets.UTF_8);
	}
	
}
