package com.github.carterj3.utilities.concurrent.locks;

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * Helper to store a pointer to the previous Node (which wraps a pointer to the
 * previous's next thread) and the current node's next Thread.
 * 
 * @author jeffrey.carter
 * 
 */
public class LockNode {

	/**
	 * The previous Node that points to this Node.</br>
	 * May be null if there is no Node before this one.
	 */
	@Nullable
	private LockNode previous;

	/**
	 * Points to the next Node.</br>
	 * The {@link AtomicReference} is never null but the value contained may be null
	 * if there are no Nodes after this one
	 */
	@NotNull
	private AtomicReference<Thread> next;

	public LockNode() {
		previous = null;
		next = new AtomicReference<>(null);
	}

	@Nullable
	public LockNode getPrevious() {
		return this.previous;
	}

	public void setPrevious(@Nullable LockNode previous) {
		this.previous = previous;
	}

	@NotNull
	public AtomicReference<Thread> getNext() {
		return this.next;
	}

}
