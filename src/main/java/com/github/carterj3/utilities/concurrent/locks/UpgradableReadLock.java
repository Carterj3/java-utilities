package com.github.carterj3.utilities.concurrent.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class UpgradableReadLock implements Lock, AutoCloseable {

	private ReentreantUpgradeLock parent;
	

	UpgradableReadLock(ReentreantUpgradeLock parent) {
		this.parent = parent;
	}

	public void lock() {
		parent.lockReadLock();
	}

	public void lockInterruptibly() throws InterruptedException {
		parent.tryLockReadLock(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
	}

	public Condition newCondition() {
		throw new UnsupportedOperationException("TODO: Understand newCondition and implement it");
	}

	public boolean tryLock() {
		try {
			return parent.tryLockReadLock(0, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	public boolean tryLock(long arg0, TimeUnit arg1) throws InterruptedException {
		return parent.tryLockReadLock(arg0, arg1);
	}

	public void unlock() {
		parent.unlockReadLock();
		
	}

	@Override
	public void close() {
		this.unlock();
	}
}
