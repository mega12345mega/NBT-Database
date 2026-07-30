package com.luneruniverse.minecraft.nbtdatabase.ui.gui;

import java.awt.Window;
import java.io.File;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import io.github.vinceglb.filekit.FileKit;
import io.github.vinceglb.filekit.PlatformFile;
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings;
import io.github.vinceglb.filekit.dialogs.FileKitKt;
import io.github.vinceglb.filekit.dialogs.FileKitMacOSSettings;
import io.github.vinceglb.filekit.dialogs.FileKitType;
import io.github.vinceglb.filekit.dialogs.FileKit_jvmKt;
import kotlin.Result;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class FileKitWrapper {
	
	private static final PlatformFile INITIAL_DIR = new PlatformFile(new File(".").getAbsoluteFile().getParentFile());
	
	private static File awaitFile(Consumer<Continuation<PlatformFile>> function) {
		CompletableFuture<PlatformFile> future = new CompletableFuture<>();
		function.accept(new Continuation<PlatformFile>() {
			@Override
			public void resumeWith(Object result) {
				if (result instanceof Result.Failure)
					future.completeExceptionally(((Result.Failure) result).exception);
				else
					future.complete((PlatformFile) result);
			}
			@Override
			public CoroutineContext getContext() {
				return EmptyCoroutineContext.INSTANCE;
			}
		});
		
		try {
			PlatformFile file = future.get();
			if (file == null)
				return null;
			return file.component1();
		} catch (ExecutionException e) {
			if (e.getCause() == null)
				e.printStackTrace();
			else
				e.getCause().printStackTrace();
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}
	
	public static File openFilePicker(Window parent, String title, String extension) {
		return awaitFile(continuation -> FileKitKt.openFilePicker(FileKit.INSTANCE, new FileKitType.File(extension), INITIAL_DIR,
				new FileKitDialogSettings(title, parent, new FileKitMacOSSettings()), continuation));
	}
	
	public static File openFileSaver(Window parent, String title, String defaultFileName, String extension) {
		return awaitFile(continuation -> FileKit_jvmKt.platformOpenFileSaver(FileKit.INSTANCE,
				defaultFileName == null ? "" : defaultFileName, extension, Collections.singleton(extension), INITIAL_DIR,
				new FileKitDialogSettings(title, parent, new FileKitMacOSSettings()), continuation));
	}
	
	public static File openDirectoryPicker(Window parent, String title) {
		return awaitFile(continuation -> FileKit_jvmKt.openDirectoryPicker(FileKit.INSTANCE, INITIAL_DIR,
				new FileKitDialogSettings(title, parent, new FileKitMacOSSettings()), continuation));
	}
	
}
