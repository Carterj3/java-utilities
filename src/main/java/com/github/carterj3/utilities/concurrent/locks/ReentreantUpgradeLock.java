package com.github.carterj3.utilities.concurrent.locks;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReadWriteLock;


/*
 * Goal: A ReentreantLock that after acquiring the ReadLock can be upgraded to be a WriteLock and then downgraded to ReadLock after the Write is finished.
 * 
 * try(? readLock = RUL.readLock().lock())
 * {
 * 		... (reading)
 * 
 * 		try(? writeLock = RUL.writeLock.lock())
 * 		try(? writeLock = readlock.upgrade())
 * 		{
 * 
 * 			
 * 		} // Implicitly call writeLock.downgrade() which leaves the readLock still acquired. 
 * }
 */
public class ReentreantUpgradeLock implements ReadWriteLock {

	private Queue<Thread> waitingThreads;
	private List<Thread> readLockOwners;
	private AtomicReference<Thread> writeLockOwner;

	private UpgradableReadLock readLock;
	private DowngradableWriteLock writeLock;

	public ReentreantUpgradeLock() {
		this.waitingThreads = new ConcurrentLinkedQueue<>();
		this.readLockOwners = new CopyOnWriteArrayList<>();
		this.writeLockOwner = new AtomicReference<>();

		this.readLock = new UpgradableReadLock(this);
		this.writeLock = new DowngradableWriteLock(this);
	}

	public UpgradableReadLock readLock() {
		return readLock;
	}

	public DowngradableWriteLock writeLock() {
		return writeLock;
	}

	void lockReadLock() {
		Thread current = Thread.currentThread();

		if (current == writeLockOwner.get()) {
			readLockOwners.add(current);
			return;
		}

		if (readLockOwners.contains(current)) {
			readLockOwners.add(current);
			return;
		}

		waitingThreads.add(current);
		boolean wasInterrupted = false;
		while (!writeLockOwner.compareAndSet(null, current)) {
			LockSupport.park();

			// Check Thread.interrupted() first to clear it
			wasInterrupted = Thread.interrupted() || wasInterrupted;
		}

		waitingThreads.remove(current);
		readLockOwners.add(current);
		writeLockOwner.set(null);

		if (wasInterrupted) {
			current.interrupt();
		}
	}

	boolean tryLockReadLock(long arg0, TimeUnit arg1) throws InterruptedException {
		Thread current = Thread.currentThread();

		if (current == writeLockOwner.get()) {
			return readLockOwners.add(current);
		}

		if (readLockOwners.contains(current)) {
			return readLockOwners.add(current);
		}

		waitingThreads.add(current);
		long maximumDuration = arg1.toNanos(arg0);
		long startTime = System.nanoTime();
		while (!writeLockOwner.compareAndSet(null, current)) {

			long duration = System.nanoTime() - startTime;
			if (duration <= 0) {
				waitingThreads.remove(current);
				return false;
			}

			LockSupport.parkNanos(this, maximumDuration - duration);

			if (Thread.interrupted()) {
				waitingThreads.remove(current);
				throw new InterruptedException();
			}
		}

		waitingThreads.remove(current);
		writeLockOwner.set(null);
		return readLockOwners.add(current);
	}

	void unlockReadLock() {
		Thread current = Thread.currentThread();

		if (readLockOwners.remove(current)) {
			waitingThreads.forEach(thread -> LockSupport.unpark(thread));
		} else {
			throw new IllegalStateException("Cannot release Lock that is not owned by the thread");
		}
	}

}
