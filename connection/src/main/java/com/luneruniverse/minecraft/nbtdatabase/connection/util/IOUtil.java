package com.luneruniverse.minecraft.nbtdatabase.connection.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IOUtil {
	
	public interface IOFunction<I, O> {
		public O apply(I input) throws IOException;
	}
	
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
		} catch (Exception e) {
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
	
	public static <T> T processResourceFolder(String folder, IOFunction<Path, T> processor) throws IOException {
		try {
			URI uri = IOUtil.class.getClassLoader().getResource(folder).toURI();
			if (uri.getScheme().equals("jar")) {
				try (FileSystem fileSystem = FileSystems.newFileSystem(uri, new HashMap<>())) {
					return processor.apply(fileSystem.getPath(folder));
				}
			}
			return processor.apply(Paths.get(uri));
		} catch (URISyntaxException e) {
			throw new IOException("Failed to find resource folder: " + folder, e);
		}
	}
	
	public static void extractResources(String resourceFolder, Path targetFolder) throws IOException {
		processResourceFolder(resourceFolder, resources -> {
			Files.walkFileTree(resources, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path resource, BasicFileAttributes attrs) throws IOException {
					Path target = targetFolder.resolve(resources.relativize(resource).toString());
					if (!Files.isDirectory(target))
						Files.copy(resource, target);
					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult visitFile(Path resource, BasicFileAttributes attrs) throws IOException {
					Files.copy(resource, targetFolder.resolve(resources.relativize(resource).toString()));
					return FileVisitResult.CONTINUE;
				}
			});
			return null;
		});
	}
	
	public static List<String> extractResourcesDryRun(String resourceFolder, Path targetFolder) throws IOException {
		return processResourceFolder(resourceFolder, resources -> {
			List<String> conflicts = new ArrayList<>();
			Files.walkFileTree(resources, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path resourceDir, BasicFileAttributes attrs) throws IOException {
					String resourceName = resources.relativize(resourceDir).toString();
					Path target = targetFolder.resolve(resourceName);
					if (Files.exists(target) && !Files.isDirectory(target))
						conflicts.add(resourceName);
					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult visitFile(Path resource, BasicFileAttributes attrs) throws IOException {
					String resourceName = resources.relativize(resource).toString();
					if (Files.exists(targetFolder.resolve(resourceName)))
						conflicts.add(resourceName);
					return FileVisitResult.CONTINUE;
				}
			});
			return conflicts;
		});
	}
	
}
