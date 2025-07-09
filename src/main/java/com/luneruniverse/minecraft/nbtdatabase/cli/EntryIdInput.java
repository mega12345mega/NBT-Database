package com.luneruniverse.minecraft.nbtdatabase.cli;

import java.util.List;
import java.util.function.Supplier;

import com.luneruniverse.minecraft.nbtdatabase.NBTEntry;
import com.luneruniverse.simplecli.CommandParseException;
import com.luneruniverse.simplecli.inputs.ArgumentOrFlagImpl;

public class EntryIdInput extends ArgumentOrFlagImpl<Long> {
	
	private final Supplier<List<NBTEntry>> results;
	
	public EntryIdInput(Supplier<List<NBTEntry>> results) {
		this.results = results;
	}
	
	@Override
	protected Long parseUnfiltered(String str) throws CommandParseException {
		try {
			if (str.startsWith("#")) {
				List<NBTEntry> results = this.results.get();
				if (results == null)
					throw new CommandParseException("There are no saved results");
				
				int resultIndex = Integer.parseInt(str.substring(1));
				if (resultIndex < 0 || resultIndex >= results.size())
					throw new CommandParseException("Invalid result index: " + resultIndex);
				return results.get(resultIndex).id;
			}
			
			return Long.parseLong(str);
		} catch (NumberFormatException e) {
			throw new CommandParseException("Invalid entry id or result index '" + str + "'");
		}
	}
	
}
