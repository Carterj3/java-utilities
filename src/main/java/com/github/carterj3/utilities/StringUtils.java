package com.github.carterj3.utilities;

public final class StringUtils {

	private StringUtils() {
		super();
	}

	public static final String format(String formatString, Object... formatArgs) {
		// TODO: Validate the formatString & args

		return String.format(formatString, formatArgs);
	}
}
