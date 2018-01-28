package com.github.carterj3.utilities.concurrent.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import javax.validation.constraints.NotNull;

public class UpgradableReadLock implements Lock, AutoCloseable {

	private ReentreantUpgradeLock parent;

	UpgradableReadLock(@NotNull ReentreantUpgradeLock parent) {
		this.parent = parent;
	}

	public void lock() {
		try {
			parent.tryLockReadLock(Long.MAX_VALUE, TimeUnit.NANOSECONDS, false);
		} catch (InterruptedException e) {
			throw new IllegalStateException("Unable to reach as tryLockReadLock does not throw when !isInterruptable",
					e);
		}
	}

	public void lockInterruptibly() throws InterruptedException {
		parent.tryLockReadLock(Long.MAX_VALUE, TimeUnit.NANOSECONDS, true);
	}

	public Condition newCondition() {
		throw new UnsupportedOperationException("TODO: Understand newCondition and implement it");
	}

	public boolean tryLock() {
		try {
			return parent.tryLockReadLock(0, TimeUnit.SECONDS, true);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	public boolean tryLock(long arg0, @NotNull TimeUnit arg1) throws InterruptedException {
		return parent.tryLockReadLock(arg0, arg1, true);
	}

	public void unlock() {
		parent.unlockReadLock();

	}

	@NotNull
	public DowngradableWriteLock upgrade() throws InterruptedException {
		parent.tryLockWriteLock(Long.MAX_VALUE, TimeUnit.NANOSECONDS, true);
		return parent.writeLock();
	}

	@NotNull
	public UpgradableReadLock open() throws InterruptedException {
		this.lockInterruptibly();

		return this;
	}

	@Override
	public void close() {
		this.unlock();
	}
}
