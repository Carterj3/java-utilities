package com.github.carterj3.utilities.concurrent.locks;

import java.util.concurrent.locks.Lock;

public class DowngradableWriteLock implements Lock {

	private ReentreantUpgradeLock parent;

	DowngradableWriteLock(ReentreantUpgradeLock parent) {
		this.parent = parent;
	}
}
