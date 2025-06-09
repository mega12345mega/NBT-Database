package com.luneruniverse.minecraft.nbtdatabase;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class Util {
	
	public interface ThrowableSupplier<T> {
		public T get() throws Throwable;
	}
	public interface ThrowableRunnable {
		public void run() throws Throwable;
	}
	public interface ThrowableFunction<I, O> {
		public O apply(I input) throws Throwable;
	}
	
	public static <T> CompletableFuture<T> supplyAsync(ThrowableSupplier<T> supplier, Executor executor) {
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
	
	public static CompletableFuture<Void> runAsync(ThrowableRunnable runnable, Executor executor) {
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
	
	public static <T> CompletableFuture<T> finallyDo(CompletableFuture<T> first, ThrowableRunnable last) {
		CompletableFuture<T> future = new CompletableFuture<>();
		first.whenComplete((value, e) -> {
			try {
				last.run();
				if (e != null)
					future.completeExceptionally(e);
				else
					future.complete(value);
			} catch (Throwable e2) {
				future.completeExceptionally(e2);
			}
		});
		return future;
	}
	
	public static <I, O> CompletableFuture<O> thenApply(CompletableFuture<I> input, ThrowableFunction<I, O> function) {
		CompletableFuture<O> future = new CompletableFuture<>();
		input.whenComplete((value, e) -> {
			if (e != null)
				future.completeExceptionally(e);
			else {
				try {
					future.complete(function.apply(value));
				} catch (Throwable e2) {
					future.completeExceptionally(e2);
				}
			}
		});
		return future;
	}
	
	public static CompletableFuture<Void> shutdown(ExecutorService executor) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		executor.shutdown();
		ForkJoinPool.commonPool().execute(() -> {
			try {
				executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
				future.complete(null);
			} catch (InterruptedException e) {
				future.completeExceptionally(e);
			}
		});
		return future;
	}
	
	public static String formatTimestamp(long utcMillis) {
		return Instant.ofEpochMilli(utcMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.RFC_1123_DATE_TIME);
	}
	
}
