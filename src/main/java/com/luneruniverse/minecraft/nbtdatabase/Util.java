package com.luneruniverse.minecraft.nbtdatabase;

import java.awt.Color;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
	
	public static CompletableFuture<Void> allOf(CompletableFuture<?>... futures) {
		if (futures.length == 0)
			return CompletableFuture.completedFuture(null);
		
		CompletableFuture<Void> future = new CompletableFuture<>();
		AtomicInteger numCompleted = new AtomicInteger();
		for (CompletableFuture<?> i : futures) {
			i.whenComplete((value, e) -> {
				if (e != null)
					future.completeExceptionally(e);
				else if (numCompleted.incrementAndGet() == futures.length)
					future.complete(null);
			});
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
	
	public static boolean isColorBright(Color color) {
		float[] channels = new float[] {color.getRed(), color.getGreen(), color.getBlue()};
		for (int i = 0; i < 3; i++) {
			float channel = channels[i] / 255;
			if (channel <= 0.04045)
				channel /= 12.92;
			else
				channel = (float) Math.pow((channel + 0.055) / 1.055, 2.4);
			channels[i] = channel;
		}
		return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722 > 0.179;
	}
	
	public static <T> Optional<T> edit(T originalValue, T newValue) {
		if (newValue == null || originalValue.equals(newValue))
			return Optional.empty();
		return Optional.of(newValue);
	}
	
}
