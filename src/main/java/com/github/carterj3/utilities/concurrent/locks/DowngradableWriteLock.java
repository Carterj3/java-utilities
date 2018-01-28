package com.github.carterj3.utilities.concurrent.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import javax.validation.constraints.NotNull;

public class DowngradableWriteLock implements Lock, AutoCloseable {

	private ReentreantUpgradeLock parent;

	DowngradableWriteLock(@NotNull ReentreantUpgradeLock parent) {
		this.parent = parent;
	}

	@Override
	public void lock() {
		try {
			parent.tryLockWriteLock(Long.MAX_VALUE, TimeUnit.NANOSECONDS, false);
		} catch (InterruptedException e) {
			throw new IllegalStateException("Unable to reach as tryLockWriteLock does not throw when !isInterruptable",
					e);
		}
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		parent.tryLockWriteLock(Long.MAX_VALUE, TimeUnit.NANOSECONDS, true);

	}

	@Override
	public Condition newCondition() {
		throw new UnsupportedOperationException("TODO: Understand newCondition and implement it");
	}

	@Override
	public boolean tryLock() {
		try {
			return parent.tryLockWriteLock(0L, TimeUnit.NANOSECONDS, true);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	public boolean tryLock(long arg0, @NotNull TimeUnit arg1) throws InterruptedException {
		return parent.tryLockWriteLock(arg0, arg1, true);
	}

	public void unlock() {
		parent.unlockWriteLock();
		
	}
	
	@NotNull
	public UpgradableReadLock downgrade() throws InterruptedException {
		parent.tryLockReadLock(Long.MAX_VALUE, TimeUnit.NANOSECONDS, true);
		this.unlock();
		return parent.readLock();
		
	}
	
	@NotNull
	public DowngradableWriteLock open() throws InterruptedException {
		this.lockInterruptibly();
		
		return this;
	}

	@Override
	public void close() {
		this.unlock();
	}
}
