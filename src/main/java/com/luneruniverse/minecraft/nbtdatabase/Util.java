package com.luneruniverse.minecraft.nbtdatabase;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

public class Util {
	
	public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier, Executor executor) {
		CompletableFuture<T> future = new CompletableFuture<>();
		try {
			executor.execute(() -> {
				try {
					future.complete(supplier.get());
				} catch (Throwable e) {
					future.completeExceptionally(e);
				}
			});
		} catch (RejectedExecutionException e) {
			future.completeExceptionally(e);
		}
		return future;
	}
	
	public static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		try {
			executor.execute(() -> {
				try {
					runnable.run();
					future.complete(null);
				} catch (Throwable e) {
					future.completeExceptionally(e);
				}
			});
		} catch (RejectedExecutionException e) {
			future.completeExceptionally(e);
		}
		return future;
	}
	
}
