package com.github.carterj3.utilities;

import org.junit.Assert;
import org.junit.Test;

public class NumberUtilsTest {

	@Test
	public void testAddWithDefault() {
		Assert.assertEquals(-1L, NumberUtils.INSTANCE.addWithDefault(0L, Long.MAX_VALUE, Long.MIN_VALUE));

		Assert.assertEquals(0L, NumberUtils.INSTANCE.addWithDefault(0L, Long.MIN_VALUE, Long.MIN_VALUE));
		Assert.assertEquals(0L, NumberUtils.INSTANCE.addWithDefault(0L, Long.MAX_VALUE, Long.MAX_VALUE));
		Assert.assertEquals(0L, NumberUtils.INSTANCE.addWithDefault(0L, -1, Long.MIN_VALUE));
		Assert.assertEquals(0L, NumberUtils.INSTANCE.addWithDefault(0L, 1, Long.MAX_VALUE));
		Assert.assertEquals(0L, NumberUtils.INSTANCE.addWithDefault(0L, Long.MIN_VALUE, -1));
		Assert.assertEquals(0L, NumberUtils.INSTANCE.addWithDefault(0L, Long.MAX_VALUE, 1));

		Assert.assertEquals(-2L, NumberUtils.INSTANCE.addWithDefault(0L, -1, -1));
		Assert.assertEquals(2L, NumberUtils.INSTANCE.addWithDefault(0L, 1, 1));
	}

}
