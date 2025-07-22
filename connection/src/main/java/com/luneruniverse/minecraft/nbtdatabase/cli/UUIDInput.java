package com.luneruniverse.minecraft.nbtdatabase.cli;

import java.util.UUID;

import com.luneruniverse.simplecli.CommandParseException;
import com.luneruniverse.simplecli.inputs.ArgumentOrFlagImpl;

public class UUIDInput extends ArgumentOrFlagImpl<UUID> {
	
	@Override
	protected UUID parseUnfiltered(String str) throws CommandParseException {
		try {
			if (str.matches("^[0-9a-fA-F]{32}$")) {
				long mostSigBits = Long.parseUnsignedLong(str.substring(0, 16), 16);
				long leastSigBits = Long.parseUnsignedLong(str.substring(16), 16);
				return new UUID(mostSigBits, leastSigBits);
			}
			return UUID.fromString(str);
		} catch (IllegalArgumentException e) {
			throw new CommandParseException("Invalid UUID '" + str + "'");
		}
	}
	
}
