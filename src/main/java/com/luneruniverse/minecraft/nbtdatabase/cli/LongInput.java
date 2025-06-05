package com.luneruniverse.minecraft.nbtdatabase.cli;

import com.luneruniverse.simplecli.CommandParseException;
import com.luneruniverse.simplecli.inputs.ArgumentOrFlagImpl;

public class LongInput extends ArgumentOrFlagImpl<Long> {
	
	@Override
	protected Long parseUnfiltered(String str) throws CommandParseException {
		try {
			return Long.parseLong(str);
		} catch (NumberFormatException e) {
			throw new CommandParseException("Invalid long '" + str + "'");
		}
	}
	
}
