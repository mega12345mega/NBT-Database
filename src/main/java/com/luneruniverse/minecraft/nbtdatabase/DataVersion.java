package com.luneruniverse.minecraft.nbtdatabase;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.luneruniverse.minecraft.nbtdatabase.cli.DataVersionInput;

public class DataVersion {
	
	public static final Map<String, Integer> MC_TO_DATA = Collections.synchronizedMap(new HashMap<>());
	public static final Map<Integer, String> DATA_TO_MC = Collections.synchronizedMap(new HashMap<>());
	
	private static volatile boolean loading;
	public static void loadVersions() {
		synchronized (DataVersionInput.class) {
			if (loading)
				return;
			loading = true;
		}
		
		ForkJoinPool.commonPool().execute(() -> {
			try {
				URL url = new URL("https://raw.githubusercontent.com/PrismarineJS/minecraft-data/refs/heads/master/data/pc/common/protocolVersions.json");
				try (InputStream in = url.openStream();
						Reader reader = new InputStreamReader(in)) {
					JsonArray array = new Gson().fromJson(reader, JsonArray.class);
					for (JsonElement element : array) {
						if (!element.isJsonObject())
							continue;
						JsonObject obj = element.getAsJsonObject();
						if (!obj.has("minecraftVersion") || !obj.has("dataVersion"))
							continue;
						JsonElement mcVersion = obj.get("minecraftVersion");
						JsonElement dataVersion = obj.get("dataVersion");
						if (!(mcVersion.isJsonPrimitive() && mcVersion.getAsJsonPrimitive().isString()) ||
								!(dataVersion.isJsonPrimitive() && dataVersion.getAsJsonPrimitive().isNumber()))
							continue;
						MC_TO_DATA.put(mcVersion.getAsString(), dataVersion.getAsInt());
						DATA_TO_MC.put(dataVersion.getAsInt(), mcVersion.getAsString());
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
	
	public static String toString(int dataVersion) {
		String mcVersion = DATA_TO_MC.get(dataVersion);
		if (mcVersion == null)
			return "" + dataVersion;
		return mcVersion + " (" + dataVersion + ")";
	}
	
}
