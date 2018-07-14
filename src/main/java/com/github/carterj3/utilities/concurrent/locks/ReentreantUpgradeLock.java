package com.github.carterj3.utilities.concurrent.locks;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.validation.constraints.NotNull;

import com.github.carterj3.utilities.NumberUtils;

/**
 * Implementation similar to {@link ReentrantReadWriteLock} but is capable of
 * converting a {@link ReadLock} into a {@link WriteLock} as well as acquiring
 * {@link ReadLock}s while that {@link Thread} holds a {@link WriteLock} and
 * vice-versa.
 * 
 * @author jeffrey.carter
 *
 */
public class ReentreantUpgradeLock implements ReadWriteLock {

	/**
	 * Node pointing to the last "Thread" in the tail so when future Threads attempt
	 * to acquire the Lock they can place themselves last in line easily.
	 */
	private AtomicReference<LockNode> tail;

	/**
	 * Node pointing to the current "Thread" that has the Lock so that when it gets
	 * unlocked it can awake the Thread after it.</br>
	 * 
	 * NOTE: Each "Thread" puts themselves into the queue before doing any
	 * operations so if a bunch of ReadLock's are acquired current will point to
	 * "Thread"s that are going to finish acquiring the Lock very shortly but
	 * current will keep getting updated so it'll be correct.
	 */
	private LockNode current;

	/**
	 * The total number of owners of a {@link ReadLock} (NOTE: each {@link Thread}
	 * can hold multiple times)
	 */
	private AtomicLong numberOfReadLockOwners;

	/**
	 * How many times this {@link Thread} has currently acquired the
	 * {@link ReadLock}
	 */
	private ThreadLocal<Long> readLockReentrantCounter;

	/**
	 * What {@link Thread} currently owns the {@link WriteLock}
	 */
	private AtomicReference<Thread> writeLockOwner;

	/**
	 * How many times this {@link Thread} has current acquired the {@link ReadLock}
	 */
	private ThreadLocal<Long> writeLockReentrantCounter;

	/**
	 * A cached {@link ReadLock} linked to this Lock to provide when requested
	 */
	private UpgradableReadLock readLock;

	/**
	 * A cached {@link WriteLock} linked to this lock to provide when requested
	 */
	private DowngradableWriteLock writeLock;

	public ReentreantUpgradeLock() {
		this.tail = new AtomicReference<>(null);
		this.current = null;

		this.numberOfReadLockOwners = new AtomicLong(0L);
		this.readLockReentrantCounter = ThreadLocal.withInitial(() -> 0L);

		this.writeLockOwner = new AtomicReference<>();
		this.writeLockReentrantCounter = ThreadLocal.withInitial(() -> 0L);

		this.readLock = new UpgradableReadLock(this);
		this.writeLock = new DowngradableWriteLock(this);
	}

	@Override
	public String toString() {
		return String.format("rlOwners: %d (%d), wlOwners: %s (%d), tail: %s, current: %s",
				numberOfReadLockOwners.get(), readLockReentrantCounter.get(), writeLockOwner.get(),
				writeLockReentrantCounter.get(), tail, current);
	}

	@NotNull
	@Override
	public UpgradableReadLock readLock() {
		return readLock;
	}

	@NotNull
	@Override
	public DowngradableWriteLock writeLock() {
		return writeLock;
	}

	/**
	 * Attempts to acquire the {@link ReadLock} within the specified time.
	 * 
	 * @param duration
	 *            the amount of time to fail acquiring after
	 * @param unit
	 *            the {@link TimeUnit} associated with the duration
	 * @param isInterruptable
	 *            if true, throw an {@link InterruptedException} when the current
	 *            {@link Thread} is interrupted
	 * @return true if the {@link ReadLock} was acquired and false if time expired
	 * @throws InterruptedException
	 *             if `isInterruptable` was true and the current {@link Thread} and
	 *             {@link Thread#interrupted()} became true
	 */
	boolean tryLockReadLock(long duration, @NotNull TimeUnit unit, boolean isInterruptable)
			throws InterruptedException {

		/* Already have a ReadLock so just increment counters */
		if (readLockReentrantCounter.get() > 0) {
			readLockReentrantCounter.set(1 + readLockReentrantCounter.get());
			this.numberOfReadLockOwners.incrementAndGet();

			return true;
		}

		long startTime = System.nanoTime();
		long endTime = NumberUtils.INSTANCE.addWithDefault(Long.MAX_VALUE, startTime, unit.toNanos(duration));
		Thread currentThread = Thread.currentThread();
		LockNode node = addNodeToQueue(currentThread);

		try {
			/* Acquire the WriteLock temporarily since that means we can definitely Read */
			if (!acquireWriteLock(isInterruptable, endTime)) {
				return false;
			}

			/* Have the WriteLock so increment the relevant Read counters */
			this.readLockReentrantCounter.set(1 + readLockReentrantCounter.get());
			this.numberOfReadLockOwners.incrementAndGet();
			this.writeLockOwner.set(null);

			/* Wake the next Thread in the Queue ( LockSupport::unpark has a null check ) */
			current = node;
			LockSupport.unpark(node.getNext().get());

			return true;
		} finally {
			cleanupNodeQueue(node);
		}

	}

	/**
	 * Unlocks the {@link ReadLock} held by this {@link Thread}
	 * 
	 * @throws IllegalStateException
	 *             if the current {@link Thread} does not hold a {@link ReadLock}
	 */
	void unlockReadLock() {
		if ((readLockReentrantCounter.get() < 1)
				|| (readLockReentrantCounter.get() == writeLockReentrantCounter.get())) {
			throw new IllegalStateException("Cannot release Lock that is not owned by the thread");
		}

		readLockReentrantCounter.set(readLockReentrantCounter.get() - 1);
		numberOfReadLockOwners.decrementAndGet();

		/* LockSupport::unpark has a null check */
		LockSupport.unpark(current.getNext().get());
	}

	/**
	 * Attempts to acquire the {@link WriteLock} within the specified time.
	 * 
	 * @param duration
	 *            the amount of time to fail acquiring after
	 * @param unit
	 *            the {@link TimeUnit} associated with the duration
	 * @param isInterruptable
	 *            if true, throw an {@link InterruptedException} when the current
	 *            {@link Thread} is interrupted
	 * @return true if the {@link WriteLock} was acquired and false if time expired
	 * @throws InterruptedException
	 *             if `isInterruptable` was true and the current {@link Thread} and
	 *             {@link Thread#interrupted()} became true
	 */
	boolean tryLockWriteLock(long duration, @NotNull TimeUnit unit, boolean isInterruptable)
			throws InterruptedException {

		/* Already have a WriteLock so just increment counters */
		if (writeLockReentrantCounter.get() > 0) {
			writeLockReentrantCounter.set(1 + writeLockReentrantCounter.get());
			readLockReentrantCounter.set(1 + readLockReentrantCounter.get());
			this.numberOfReadLockOwners.incrementAndGet();
			return true;
		}

		long startTime = System.nanoTime();
		long endTime = NumberUtils.INSTANCE.addWithDefault(Long.MAX_VALUE, startTime, unit.toNanos(duration));
		Thread currentThread = Thread.currentThread();
		LockNode node = addNodeToQueue(currentThread);

		try {
			/* Acquire WriteLock to prevent future Threads from becoming readers */
			if (!acquireWriteLock(isInterruptable, endTime)) {
				return false;
			}

			/* Wait until only this Thread is a reader */
			while (numberOfReadLockOwners.get() != readLockReentrantCounter.get()) {
				LockSupport.parkNanos(endTime - System.nanoTime());

				if (isInterruptable && Thread.interrupted()) {
					writeLockOwner.set(null);
					LockSupport.unpark(node.getNext().get());
					throw new InterruptedException();
				}

				if (endTime >= System.nanoTime()) {
					writeLockOwner.set(null);
					LockSupport.unpark(node.getNext().get());
					return false;
				}
			}

			/* No other Thread is Reading or Writing so increment counters */
			writeLockReentrantCounter.set(1 + writeLockReentrantCounter.get());
			readLockReentrantCounter.set(1 + readLockReentrantCounter.get());
			this.numberOfReadLockOwners.incrementAndGet();

			current = node;
			LockSupport.unpark(node.getNext().get());

			return true;
		} finally {
			cleanupNodeQueue(node);
		}

	}

	/**
	 * Unlocks the {@link WriteLock} held by this {@link Thread}
	 * 
	 * @throws IllegalStateException
	 *             if the current {@link Thread} does not hold a {@link WriteLock}
	 */
	void unlockWriteLock() {
		Thread currentThread = Thread.currentThread();

		if (!(currentThread.equals(writeLockOwner.get()))) {
			throw new IllegalStateException("Cannot release Lock that is not owned by the thread");
		}

		writeLockReentrantCounter.set(writeLockReentrantCounter.get() - 1);
		readLockReentrantCounter.set(readLockReentrantCounter.get() - 1);
		this.numberOfReadLockOwners.decrementAndGet();

		if (writeLockReentrantCounter.get() == 0) {
			writeLockOwner.set(null);
			LockSupport.unpark(current.getNext().get());
		}
	}

	/**
	 * Sets the {@link #writeLockOwner} if it is currently not set, otherwise waits
	 * until the specified `endTime` for an opportunity to set the value.
	 * 
	 * @param isInterruptable
	 *            if this method should throw {@link InterruptedException} when
	 *            interrupted (NOTE: if false, this method won't gobble the
	 *            interrupted flag)
	 * @param endTime
	 *            the time (in nanos) to stop waiting
	 * @return true if the {@link #writeLockOwner} is now set to the currentThread,
	 *         otherwise false
	 * @throws InterruptedException
	 *             if this {@link Thread} is interrupted while acquiring the
	 *             {@link #writeLockOwner}
	 */
	private boolean acquireWriteLock(boolean isInterruptable, long endTime) throws InterruptedException {
		Thread currentThread = Thread.currentThread();

		if (currentThread.equals(writeLockOwner.get())) {
			return true;
		}

		while (!this.writeLockOwner.compareAndSet(null, currentThread)) {
			LockSupport.parkNanos(endTime - System.nanoTime());

			if (isInterruptable && Thread.interrupted()) {
				throw new InterruptedException();
			}

			if (endTime <= System.nanoTime()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Cleans up the node queue.</br>
	 * 
	 * @param node
	 *            the Node that is being removed from the queue.
	 * @see #addNodeToQueue(Thread)
	 */
	private void cleanupNodeQueue(LockNode node) {
		/* Need to fix the Queue if an Exception occurs */
		if (tail.compareAndSet(node, null)) {
			/* Fixed, nothing is after this node */
			return;
		}

		/* Not the last node so can just fix pointers */
		if (Objects.isNull(node.getPrevious())) {
			/* Fixed, nothing came before this node */
			LockSupport.unpark(node.getNext().get());
			return;
		}

		node.getPrevious().getNext().set(node.getNext().get());
		/* Fixed, previous points to next */
		LockSupport.unpark(node.getNext().get());
		return;
	}

	/**
	 * Only the Tail & Current nodes are known by the Lock and each of the links are
	 * stored on the stack inside the acquire lock functions. When a lock is
	 * acquired the links can be removed because only the latest Thread that needs
	 * to be awakened when a Lock is released needs to be known and is as `current`.
	 * 
	 * @param thread
	 *            the thread to insert in the queue
	 * @return a {@link LockNode} for this link in the queue
	 */
	private LockNode addNodeToQueue(Thread thread) {
		LockNode node = new LockNode();

		for (;;) {
			LockNode previous = tail.compareAndExchange(null, node);
			if (previous == null) {
				/* No other threads are in the queue so don't need to fix the previous Thread */
				return node;
			}

			if (previous.getNext().compareAndSet(null, thread)) {
				/* Previous node points to this Thread now */
				if (tail.compareAndSet(previous, node)) {
					return node;
				}
			}
		}
	}

}
