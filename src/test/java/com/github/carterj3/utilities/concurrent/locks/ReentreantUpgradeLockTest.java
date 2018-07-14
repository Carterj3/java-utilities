package com.github.carterj3.utilities.concurrent.locks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.carterj3.utilities.concurrent.locks.DowngradableWriteLock;
import com.github.carterj3.utilities.concurrent.locks.ReentreantUpgradeLock;
import com.github.carterj3.utilities.concurrent.locks.UpgradableReadLock;

public class ReentreantUpgradeLockTest {

	private ExecutorService executorService;

	@BeforeEach
	public void setup() {
		executorService = Executors.newCachedThreadPool();
	}

	@AfterEach
	public void tearDown() {
		executorService.shutdown();
	}

	@Test
	void test_readLock_timings() throws Throwable {
		ReentreantUpgradeLock rul = new ReentreantUpgradeLock();
		UpgradableReadLock rulRl = rul.readLock();

		ReentrantLock rel = new ReentrantLock();

		ReadWriteLock rwl = new ReentrantReadWriteLock();
		Lock rwlRl = rwl.readLock();

		/*
		 * ReentrantReadWriteLock only supports 65535 concurrent attempts (to be fair,
		 * 65,535 threads is a lot).
		 */
		int cycles = 65535;

		long rulStartTime = System.nanoTime();
		for (int i = 0; i < cycles; i++) {
			rulRl.lock();
		}

		long rulMidTime = System.nanoTime();
		for (int i = 0; i < cycles; i++) {
			rulRl.unlock();
		}
		long rulLockTime = rulMidTime - rulStartTime;
		long rulUnlockTime = System.nanoTime() - rulMidTime;

		long relStartTime = System.nanoTime();
		for (int i = 0; i < cycles; i++) {
			rel.lock();
		}

		long relMidTime = System.nanoTime();
		for (int i = 0; i < cycles; i++) {
			rel.unlock();
		}
		long relLockTime = relMidTime - relStartTime;
		long relUnlockTime = System.nanoTime() - relMidTime;

		long rwlStartTime = System.nanoTime();
		for (int i = 0; i < cycles; i++) {
			rwlRl.lock();
		}

		long rwlMidTime = System.nanoTime();
		for (int i = 0; i < cycles; i++) {
			rwlRl.unlock();
		}
		long rwlLockTime = rwlMidTime - rwlStartTime;
		long rwlUnlockTime = System.nanoTime() - rwlMidTime;

		/*
		 * These numbers need a huge grain of salt since if you rearrange the order of
		 * when the blocks are run the times change significantly
		 */
		// RUL: [10108513, 7961890]ns [0.010, 0.008]s 154.246 ns/L, 121.491 ns/UL
		// REL: [3164437, 2859529]ns [0.003, 0.003]s 48.286 ns/L, 43.634 ns/UL
		// RWL: [6869648, 3825305]ns [0.007, 0.004]s 104.824 ns/L, 58.370 ns/UL

		double rulLockPerNs = (rulLockTime / 1.0) / cycles;
		double rulUnlockPerNs = (rulUnlockTime / 1.0) / cycles;

		double relLockPerNs = (relLockTime / 1.0) / cycles;
		double relUnlockPerNs = (relUnlockTime / 1.0) / cycles;

		double rwlLockPerNs = (rwlLockTime / 1.0) / cycles;
		double rwlUnlockPerNs = (rwlUnlockTime / 1.0) / cycles;

		System.out.println(String.format("RUL: [%d, %d]ns \t [%.3f, %.3f]s \t %.3f ns/L, %.3f ns/UL", rulLockTime,
				rulUnlockTime, rulLockTime / 1e9, rulUnlockTime / 1e9, rulLockPerNs, rulUnlockPerNs));
		System.out.println(String.format("REL: [%d, %d]ns \t [%.3f, %.3f]s \t %.3f ns/L, %.3f ns/UL", relLockTime,
				relUnlockTime, relLockTime / 1e9, relUnlockTime / 1e9, relLockPerNs, relUnlockPerNs));
		System.out.println(String.format("RWL: [%d, %d]ns \t [%.3f, %.3f]s \t %.3f ns/L, %.3f ns/UL", rwlLockTime,
				rwlUnlockTime, rwlLockTime / 1e9, rwlUnlockTime / 1e9, rwlLockPerNs, rwlUnlockPerNs));
	}

	@Test
	void test_readlock() throws Throwable {
		ReentreantUpgradeLock rul = new ReentreantUpgradeLock();
		UpgradableReadLock rl = rul.readLock();

		try (UpgradableReadLock sameLock = rl.open()) {
			rl.lock();
			rl.lock();

			rl.unlock();
			rl.unlock();
		}

		try {
			rl.unlock();
			Assert.fail();
		} catch (IllegalStateException e) {
			Assert.assertEquals("Cannot release Lock that is not owned by the thread", e.getLocalizedMessage());
		}
	}

	@Test
	void test_readlock_multipleThreads() throws Throwable {
		int numThreads = 100;
		CountDownLatch latch = new CountDownLatch(numThreads);

		ReentreantUpgradeLock rul = new ReentreantUpgradeLock();
		UpgradableReadLock rl = rul.readLock();

		List<Future<?>> futures = new ArrayList<>();
		for (int i = 0; i < numThreads; i++) {
			futures.add(executorService.submit(() -> {
				try {
					rl.lock();
					latch.countDown();
					latch.await();
				} catch (InterruptedException e) {
					return e;
				} finally {
					rl.unlock();
				}
				return null;
			}));
		}

		latch.await();

		// A tiny bit of time for the futures to unlock
		Thread.sleep(100L);

		for (int i = 0; i < numThreads; i++) {
			Assert.assertTrue(futures.get(i).isDone());
			Assert.assertNull(futures.get(i).get());
		}
	}

	@Test
	public void test_writelock() throws Throwable {
		ReentreantUpgradeLock rul = new ReentreantUpgradeLock();
		UpgradableReadLock rl = rul.readLock();
		DowngradableWriteLock wl = rul.writeLock();

		Assert.assertTrue(wl.tryLock());
		Assert.assertTrue(wl.tryLock());

		Assert.assertTrue(rl.tryLock());
		Assert.assertTrue(rl.tryLock());

		wl.unlock();
		wl.unlock();

		rl.unlock();
		rl.unlock();

		// -
		int numThreads = 1_000;
		CountDownLatch finishedLatch = new CountDownLatch(numThreads);
		CountDownLatch halfwayLatch = new CountDownLatch(numThreads);
		List<Future<Throwable>> futures = new ArrayList<>();
		for (int i = 0; i < numThreads; i++) {
			futures.add(executorService.submit(() -> {
				try {
					halfwayLatch.countDown();

					try (AutoCloseable sameLock = wl.open()) {
						Assert.assertTrue(rl.tryLock());
						Assert.assertTrue(wl.tryLock());
						Assert.assertTrue(wl.tryLock());

						wl.unlock();
						rl.unlock();
					}

					halfwayLatch.await();
					wl.unlock();

					return null;
				} catch (Throwable e) {
					return e;
				} finally {
					finishedLatch.countDown();
				}
			}));
		}

		finishedLatch.await();
		// A tiny bit of time for the futures to finish
		Thread.sleep(100L);

		for (int i = 0; i < numThreads; i++) {
			Assert.assertTrue(futures.get(i).isDone());
			Throwable e = futures.get(i).get();
			if (e != null) {
				throw e;
			}
		}

	}
}
