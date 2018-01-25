package com.github.carterj3.utilities;

public final class ThrowableUtils {
	private ThrowableUtils() {
		super();
	}
	
	// https://stackoverflow.com/a/39719455
	public static final <T extends Throwable> T uncheck(Throwable throwable) throws T {
		throw (T) throwable;
	}
}
