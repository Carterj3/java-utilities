package com.github.carterj3.utilities;

public class NumberUtils {

	public static final NumberUtils INSTANCE = new NumberUtils();

	private NumberUtils() {

	}

	/**
	 * Adds the two numbers together and if an overflow occurs returns the provided
	 * value
	 * 
	 * @param x
	 *            one of the numbers to add
	 * @param y
	 *            one of the numbers to add
	 * @param overflowValue
	 *            the value to return if overflow occurs
	 * @return (x + y) if no overflow, otherwise `overflowValue` if overflow occurs
	 */
	public long addWithDefault(long overflowValue, long... summands) {
		long sum = 0;

		for (long x : summands) {
			boolean sameSign = (x ^ sum) >= 0;
			if (sameSign && ((x < 0 && Long.MIN_VALUE - x > sum) || (x > 0 && Long.MAX_VALUE - x < sum))) {
				return overflowValue;
			}

			sum = sum + x;
		}

		return sum;
	}

}
