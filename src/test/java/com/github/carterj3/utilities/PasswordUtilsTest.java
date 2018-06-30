package com.github.carterj3.utilities;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class PasswordUtilsTest {

	@Test
	public void test() {
		Set<String> passwords = Arrays.asList("sdakoks", "password", "123456", "password1", "111111", "1q2w3e4r5t",
				"myspace1", "zag12wsx", "fuckyou1", "1qaz2wsx", "CorrectHorseBatteryStaple", "oiadsvnoifoniuqee").stream().collect(Collectors.toSet());

		for (String password : passwords) {
			System.out.println(String.format("Password: [%s], nist: [%.3f], custom: [%.3f]", password,
					PasswordUtils.INSTANCE.GetNISTNumBits(password.toCharArray(), 1.0),
					PasswordUtils.INSTANCE.ComputePasswordStrength(password.toCharArray(), 1.0, 4, 0.0)));
		}
	}
}
