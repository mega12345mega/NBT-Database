package com.luneruniverse.minecraft.nbtdatabase.cli;

import com.luneruniverse.simplecli.CommandParseException;
import com.luneruniverse.simplecli.inputs.ArgumentOrFlagImpl;

public class ColorInput extends ArgumentOrFlagImpl<Integer> {
	
	public static String toString(int color) {
		String str = Integer.toHexString(color & 0xFFFFFF).toUpperCase();
		while (str.length() < 6)
			str = "0" + str;
		return str;
	}
	
	@Override
	protected Integer parseUnfiltered(String str) throws CommandParseException {
		if (!str.matches("^[0-9a-fA-F]{6}$"))
			throw new CommandParseException("Invalid color '" + str + "'");
		return Integer.parseInt(str, 16);
	}
	
}
