package com.github.carterj3.utilities.concurrent.locks;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReadWriteLock;

import javax.validation.constraints.NotNull;

public class ReentreantUpgradeLock implements ReadWriteLock {

	/*
	 * TODO: Investigate if each Thread can figure out who the next Thread to call
	 * is so that instead of storing this information in a Queue it'd be store in
	 * the Stack.
	 */
	private Queue<Thread> waitingThreads;

	private AtomicLong numberOfReadLockOwners;
	private ConcurrentMap<Thread, AtomicLong> readLockOwners;

	private AtomicLong numberOfWriteLockOwners;
	private AtomicReference<Thread> writeLockOwner;

	private UpgradableReadLock readLock;
	private DowngradableWriteLock writeLock;

	public ReentreantUpgradeLock() {
		this.waitingThreads = new ConcurrentLinkedQueue<>();

		this.numberOfReadLockOwners = new AtomicLong(0L);
		this.readLockOwners = new ConcurrentHashMap<>();

		this.numberOfWriteLockOwners = new AtomicLong(0L);
		this.writeLockOwner = new AtomicReference<>();

		this.readLock = new UpgradableReadLock(this);
		this.writeLock = new DowngradableWriteLock(this);
	}

	@NotNull
	public UpgradableReadLock readLock() {
		return readLock;
	}

	@NotNull
	public DowngradableWriteLock writeLock() {
		return writeLock;
	}

	boolean tryLockReadLock(long arg0, @NotNull TimeUnit arg1, boolean isInterruptable) throws InterruptedException {
		Thread currentThread = Thread.currentThread();
		AtomicLong currentCounter = readLockOwners.computeIfAbsent(currentThread, key -> new AtomicLong(0L));

		if (currentThread == writeLockOwner.get()) {
			/*
			 * The current thread cannot release the writeLock while it is acquiring the
			 * readLock so this is thread-safe.
			 */
			numberOfReadLockOwners.incrementAndGet();
			currentCounter.incrementAndGet();
			return true;
		}

		if (readLockOwners.get(currentThread).get() > 0) {
			/*
			 * The current thread cannot release a readLock while it is acquiring another
			 * readLock so this is thread-safe.
			 */
			numberOfReadLockOwners.incrementAndGet();
			currentCounter.incrementAndGet();
			return true;
		}

		waitingThreads.add(currentThread);
		long maximumDuration = arg1.toNanos(arg0);
		long startTime = System.nanoTime();
		boolean wasInterrupted = false;
		while (!writeLockOwner.compareAndSet(null, currentThread)) {

			long duration = System.nanoTime() - startTime;
			if (duration > maximumDuration) {
				waitingThreads.remove(currentThread);
				return false;
			}

			LockSupport.parkNanos(maximumDuration - duration);

			if (Thread.interrupted()) {

				if (isInterruptable) {
					waitingThreads.remove(currentThread);
					throw new InterruptedException();
				} else {
					wasInterrupted = true;
				}
			}
		}

		numberOfReadLockOwners.incrementAndGet();
		currentCounter.incrementAndGet();
		writeLockOwner.set(null);
		waitingThreads.remove(currentThread);

		LockSupport.unpark(waitingThreads.peek());

		if (wasInterrupted) {
			currentThread.interrupt();
		}

		return true;
	}

	void unlockReadLock() {
		Thread currentThread = Thread.currentThread();
		AtomicLong currentCounter = readLockOwners.computeIfAbsent(currentThread, key -> new AtomicLong(0L));

		if (!(currentCounter.get() > 0)) {
			throw new IllegalStateException("Cannot release Lock that is not owned by the thread");
		}

		currentCounter.decrementAndGet();
		numberOfReadLockOwners.decrementAndGet();

		LockSupport.unpark(writeLockOwner.get());
		LockSupport.unpark(waitingThreads.peek());
	}

	boolean tryLockWriteLock(long arg0, @NotNull TimeUnit arg1, boolean isInterruptable) throws InterruptedException {
		Thread currentThread = Thread.currentThread();
		AtomicLong currentCounter = readLockOwners.computeIfAbsent(currentThread, key -> new AtomicLong(0L));

		if (currentThread == writeLockOwner.get()) {
			/*
			 * The current thread cannot release the writeLock while it is acquiring the
			 * writeLock so this is thread-safe.
			 */
			numberOfWriteLockOwners.incrementAndGet();
			tryLockReadLock(arg0, arg1, isInterruptable);
			return true;
		}

		waitingThreads.add(currentThread);
		long maximumDuration = arg1.toNanos(arg0);
		long startTime = System.nanoTime();
		boolean wasInterrupted = false;
		while (!((currentThread.equals(writeLockOwner.get()) || writeLockOwner.compareAndSet(null, currentThread))
				&& (numberOfReadLockOwners.get() == currentCounter.get()))) {

			long duration = System.nanoTime() - startTime;
			if (duration > maximumDuration) {
				waitingThreads.remove(currentThread);
				return false;
			}

			LockSupport.parkNanos(maximumDuration - duration);

			if (Thread.interrupted()) {

				if (isInterruptable) {
					waitingThreads.remove(currentThread);
					throw new InterruptedException();
				} else {
					wasInterrupted = true;
				}
			}
		}

		numberOfWriteLockOwners.incrementAndGet();
		tryLockReadLock(arg0, arg1, isInterruptable);
		waitingThreads.remove(currentThread);

		if (wasInterrupted) {
			currentThread.interrupt();
		}

		return true;
	}

	void unlockWriteLock() {
		Thread currentThread = Thread.currentThread();

		if (!(currentThread.equals(writeLockOwner.get()))) {
			throw new IllegalStateException("Cannot release Lock that is not owned by the thread");
		}

		unlockReadLock();
		if (numberOfWriteLockOwners.decrementAndGet() == 0) {
			writeLockOwner.set(null);
		}

		LockSupport.unpark(waitingThreads.peek());
	}

}
