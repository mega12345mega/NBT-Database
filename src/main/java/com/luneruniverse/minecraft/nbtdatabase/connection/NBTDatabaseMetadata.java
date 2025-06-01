package com.luneruniverse.minecraft.nbtdatabase.connection;

public class NBTDatabaseMetadata {
	
	private final int maxNbtSize;
	private final int maxNumResults;
	
	public NBTDatabaseMetadata(int maxNbtSize, int maxNumResults) {
		this.maxNbtSize = maxNbtSize;
		this.maxNumResults = maxNumResults;
	}
	
	public int getMaxNbtSize() {
		return maxNbtSize;
	}
	
	public int getMaxNumResults() {
		return maxNumResults;
	}
	
	@Override
	public String toString() {
		return "NBTDatabaseMetadata[maxNbtSize=" + maxNbtSize + ", maxNumResults=" + maxNumResults + "]";
	}
	
}
