package com.luneruniverse.minecraft.nbtdatabase;

public class Config {
	
	private int maxNbtSize;
	private int maxNumResults;
	
	public Config(int maxNbtSize, int maxNumResults) {
		this.maxNbtSize = maxNbtSize;
		this.maxNumResults = maxNumResults;
	}
	
	public Config setMaxNbtSize(int maxNbtSize) {
		this.maxNbtSize = maxNbtSize;
		return this;
	}
	public Config setMaxNumResults(int maxNumResults) {
		this.maxNumResults = maxNumResults;
		return this;
	}
	
	public int getMaxNbtSize() {
		return maxNbtSize;
	}
	public int getMaxNumResults() {
		return maxNumResults;
	}
	
}
