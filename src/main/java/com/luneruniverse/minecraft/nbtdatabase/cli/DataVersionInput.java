package com.luneruniverse.minecraft.nbtdatabase.cli;

import com.luneruniverse.minecraft.nbtdatabase.DataVersion;
import com.luneruniverse.simplecli.CommandParseException;
import com.luneruniverse.simplecli.inputs.ArgumentOrFlagImpl;

public class DataVersionInput extends ArgumentOrFlagImpl<Integer> {
	
	@Override
	protected Integer parseUnfiltered(String str) throws CommandParseException {
		Integer version = DataVersion.MC_TO_DATA.get(str);
		if (version != null)
			return version;
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			throw new CommandParseException("Invalid data version '" + str + "'");
		}
	}
	
}
