package com.luneruniverse.minecraft.nbtdatabase.connection.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FutureUtil {
	
	public static <T> CompletableFuture<T> failedFuture(Throwable throwable) {
		CompletableFuture<T> future = new CompletableFuture<>();
		future.completeExceptionally(throwable);
		return future;
	}
	
	public static Throwable exceptionNow(CompletableFuture<?> future) {
		if (!future.isCompletedExceptionally())
			return null;
		return future.handle((value, e) -> e).join();
	}
	
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
				transferResult(first, future);
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
	
	public static <I, O> CompletableFuture<O> thenCompose(CompletableFuture<I> input, ThrowableFunction<I, CompletableFuture<O>> function) {
		CompletableFuture<O> future = new CompletableFuture<>();
		input.whenComplete((value, e) -> {
			if (e != null)
				future.completeExceptionally(e);
			else {
				try {
					transferResult(function.apply(value), future);
				} catch (Throwable e2) {
					future.completeExceptionally(e2);
				}
			}
		});
		return future;
	}
	
	public static <T> void transferResult(CompletableFuture<T> from, CompletableFuture<T> to) {
		from.whenComplete((value, e) -> {
			if (e != null)
				to.completeExceptionally(e);
			else
				to.complete(value);
		});
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
	
	public static ThreadFactory daemonThreadFactory() {
		ThreadFactory defaultFactory = Executors.defaultThreadFactory();
		return r -> {
			Thread thread = defaultFactory.newThread(r);
			thread.setDaemon(true);
			return thread;
		};
	}
	
	public static final Executor DAEMON_EXECUTOR = Executors.newCachedThreadPool(daemonThreadFactory());
	
}
