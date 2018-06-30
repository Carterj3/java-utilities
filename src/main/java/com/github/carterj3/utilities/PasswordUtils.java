package com.github.carterj3.utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.SoftReference;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class PasswordUtils {

	public static final PasswordUtils INSTANCE = new PasswordUtils();

	public final String[] QWERTY_STRINGS = { "1234567890-qwertyuiopasdfghjkl;zxcvbnm,./",
			"1qaz2wsx3edc4rfv5tgb6yhn7ujm8ik,9ol.0p;/-['=]:?_{\"+}", "1qaz2wsx3edc4rfv5tgb6yhn7ujm8ik9ol0p",
			"qazwsxedcrfvtgbyhnujmik,ol.p;/-['=]:?_{\"+}", "qazwsxedcrfvtgbyhnujmikolp",
			"]\"/=[;.-pl,0okm9ijn8uhb7ygv6tfc5rdx4esz3wa2q1", "pl0okm9ijn8uhb7ygv6tfc5rdx4esz3wa2q1",
			"]\"/[;.pl,okmijnuhbygvtfcrdxeszwaq", "plokmijnuhbygvtfcrdxeszwaq",
			"014725836914702583697894561230258/369*+-*/", "abcdefghijklmnopqrstuvwxyz" };

	public final Character EMPTY_CHARACTER = '\u200C';

	public final Set<char[]> WORD_DICTIONARY;
	public final Map<Character, Character> KEYBOARD_DOWN_NO_SHIFT;
	public final Map<Character, Character> KEYBOARD_DOWN_RIGHT_SHIFT;
	public final Map<Character, Character> KEYBOARD_DOWN_LEFT_SHIFT;
	public final Map<Character, Collection<Character>> LEET_SPEAK_MAP;

	private PasswordUtils() {
		super();

		WORD_DICTIONARY = new TreeSet<>((a1, a2) -> {
			if (a1 == a2) {
				return 0;
			}

			if (a1 == null) {
				return -1;
			} else if (a2 == null) {
				return 1;
			}

			if (a1.length != a2.length) {
				return a1.length - a2.length;
			}

			for (int i = 0; i < a1.length; i++) {
				if (a1[i] != a2[i]) {
					return a1[i] - a2[i];
				}
			}

			return 0;
		});

		URL url = getClass().getClassLoader().getResource("SSO_Dictionary.nsv");

		if (url != null) {
			try (Scanner scanner = new Scanner(new File(url.toURI()))) {
				while (scanner.hasNextLine()) {
					WORD_DICTIONARY.add(scanner.nextLine().toCharArray());
				}
			} catch (FileNotFoundException | URISyntaxException e) {
			}
		}

		/*
		 * On a qwerty keyboard there aren't exactly letters below the bottom row.
		 * (Spacebar and etc are being ignored).
		 */
		KEYBOARD_DOWN_NO_SHIFT = new HashMap<>();
		KEYBOARD_DOWN_NO_SHIFT.put('z', EMPTY_CHARACTER);
		KEYBOARD_DOWN_NO_SHIFT.put('x', EMPTY_CHARACTER);
		KEYBOARD_DOWN_NO_SHIFT.put('c', EMPTY_CHARACTER);
		KEYBOARD_DOWN_NO_SHIFT.put('v', EMPTY_CHARACTER);
		KEYBOARD_DOWN_NO_SHIFT.put('b', EMPTY_CHARACTER);
		KEYBOARD_DOWN_NO_SHIFT.put('n', EMPTY_CHARACTER);
		KEYBOARD_DOWN_NO_SHIFT.put('m', EMPTY_CHARACTER);
		KEYBOARD_DOWN_NO_SHIFT.put(',', EMPTY_CHARACTER);
		KEYBOARD_DOWN_NO_SHIFT.put('<', EMPTY_CHARACTER);
		KEYBOARD_DOWN_NO_SHIFT.put('.', EMPTY_CHARACTER);
		KEYBOARD_DOWN_NO_SHIFT.put('>', EMPTY_CHARACTER);
		KEYBOARD_DOWN_NO_SHIFT.put('/', EMPTY_CHARACTER);
		KEYBOARD_DOWN_NO_SHIFT.put('?', EMPTY_CHARACTER);

		KEYBOARD_DOWN_RIGHT_SHIFT = new HashMap<>();
		KEYBOARD_DOWN_RIGHT_SHIFT.put('a', 'z');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('q', 'a');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('1', 'q');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('s', 'x');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('w', 's');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('2', 'w');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('d', 'c');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('e', 'd');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('3', 'e');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('f', 'v');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('r', 'f');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('4', 'r');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('g', 'b');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('t', 'g');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('5', 't');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('h', 'n');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('y', 'h');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('6', 'y');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('j', 'm');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('u', 'j');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('7', 'u');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('i', 'k');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('8', 'i');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('o', 'l');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('9', 'o');
		KEYBOARD_DOWN_RIGHT_SHIFT.put('0', 'p');

		KEYBOARD_DOWN_LEFT_SHIFT = new HashMap<>();
		KEYBOARD_DOWN_LEFT_SHIFT.put('2', 'q');
		KEYBOARD_DOWN_LEFT_SHIFT.put('w', 'a');
		KEYBOARD_DOWN_LEFT_SHIFT.put('3', 'w');
		KEYBOARD_DOWN_LEFT_SHIFT.put('s', 'z');
		KEYBOARD_DOWN_LEFT_SHIFT.put('e', 's');
		KEYBOARD_DOWN_LEFT_SHIFT.put('4', 'e');
		KEYBOARD_DOWN_LEFT_SHIFT.put('d', 'x');
		KEYBOARD_DOWN_LEFT_SHIFT.put('r', 'd');
		KEYBOARD_DOWN_LEFT_SHIFT.put('5', 'r');
		KEYBOARD_DOWN_LEFT_SHIFT.put('f', 'c');
		KEYBOARD_DOWN_LEFT_SHIFT.put('t', 'f');
		KEYBOARD_DOWN_LEFT_SHIFT.put('6', 't');
		KEYBOARD_DOWN_LEFT_SHIFT.put('g', 'v');
		KEYBOARD_DOWN_LEFT_SHIFT.put('y', 'g');
		KEYBOARD_DOWN_LEFT_SHIFT.put('7', 'y');
		KEYBOARD_DOWN_LEFT_SHIFT.put('h', 'b');
		KEYBOARD_DOWN_LEFT_SHIFT.put('u', 'h');
		KEYBOARD_DOWN_LEFT_SHIFT.put('8', 'u');
		KEYBOARD_DOWN_LEFT_SHIFT.put('j', 'n');
		KEYBOARD_DOWN_LEFT_SHIFT.put('i', 'j');
		KEYBOARD_DOWN_LEFT_SHIFT.put('9', 'i');
		KEYBOARD_DOWN_LEFT_SHIFT.put('k', 'm');
		KEYBOARD_DOWN_LEFT_SHIFT.put('o', 'k');
		KEYBOARD_DOWN_LEFT_SHIFT.put('0', 'o');
		KEYBOARD_DOWN_LEFT_SHIFT.put('p', 'l');
		KEYBOARD_DOWN_LEFT_SHIFT.put('-', 'p');

		LEET_SPEAK_MAP = new HashMap<>();
		LEET_SPEAK_MAP.put('@', Arrays.asList('a'));
		LEET_SPEAK_MAP.put('!', Arrays.asList('i'));
		LEET_SPEAK_MAP.put('$', Arrays.asList('s'));
		LEET_SPEAK_MAP.put('1', Arrays.asList('i', 'l'));
		LEET_SPEAK_MAP.put('2', Arrays.asList('z'));
		LEET_SPEAK_MAP.put('3', Arrays.asList('e'));
		LEET_SPEAK_MAP.put('4', Arrays.asList('a'));
		LEET_SPEAK_MAP.put('5', Arrays.asList('s'));
		LEET_SPEAK_MAP.put('6', Arrays.asList('g'));
		LEET_SPEAK_MAP.put('7', Arrays.asList('t'));
		LEET_SPEAK_MAP.put('8', Arrays.asList('b'));
		LEET_SPEAK_MAP.put('9', Arrays.asList('g'));
		LEET_SPEAK_MAP.put('0', Arrays.asList('o'));
	}

	public double GetNISTNumBits(char[] password, double repeatCharacterPenalty) {
		return GetNISTNumBits(password, repeatCharacterPenalty, -1, -1);
	}

	/**
	 * 
	 * NOTE: A 'repeatCharacterPenalty' of 1.0 is equivilant to the NIST algorithm.
	 * A 'repeatCharacterPenalty' of 0.75 is what Thomas Hruska uses.
	 * 
	 * @param password
	 * @param repeatCharacterPenalty
	 * @return
	 * @see <a href=
	 *      "http://cubicspot.blogspot.com/2012/06/how-to-calculate-password-strength-part.html">Based
	 *      on SSO_GetNISTNumBits</a>
	 */
	public double GetNISTNumBits(char[] password, double repeatCharacterPenalty, int ignoreCharactersStart,
			int ignoreCharactersEnd) {
		/*
		 * MIT License The MIT License
		 * 
		 * Copyright (c) 2013 CubicleSoft
		 * 
		 * Permission is hereby granted, free of charge, to any person obtaining a copy
		 * of this software and associated documentation files (the "Software"), to deal
		 * in the Software without restriction, including without limitation the rights
		 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
		 * copies of the Software, and to permit persons to whom the Software is
		 * furnished to do so, subject to the following conditions:
		 * 
		 * The above copyright notice and this permission notice shall be included in
		 * all copies or substantial portions of the Software.
		 * 
		 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
		 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
		 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
		 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
		 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
		 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
		 * SOFTWARE.
		 */

		if (repeatCharacterPenalty < 0.0 || repeatCharacterPenalty > 1.0) {
			throw new IllegalArgumentException(
					"repeatCharacterPenalty should be between 0.0 (no bits for repeats, worst penalty) and 1.0 (full bits for repeats, no penalty)");
		}

		Map<Character, Double> seenCharacters = new HashMap<>();
		double numBits = 1.0;
		for (int i = 0, c = 0; i < password.length; i++) {
			if (i > ignoreCharactersStart && i < ignoreCharactersEnd) {
				continue;
			}
			c++;

			Character character = password[i];

			if (character == EMPTY_CHARACTER) {
				c--;
				continue;
			}

			double multiplier = seenCharacters.compute(character,
					(key, value) -> (value == null) ? 1.0 : value * repeatCharacterPenalty);

			if (c > 19) {
				numBits *= 1.0 * multiplier;
			} else if (c > 7) {
				numBits *= 1.5 * multiplier;
			} else if (c > 0) {
				numBits *= 2.0 * multiplier;
			} else {
				numBits *= 4.0 * multiplier; // Always 4
			}
		}

		return numBits;
	}

	/**
	 * 
	 * NOTE: A 'repeatCharacterPenalty' of 1.0 is equivalent to the NIST algorithm.
	 * A 'repeatCharacterPenalty' of 0.75 is what Thomas Hruska uses.
	 * 
	 * @param password
	 * @param repeatCharacterPenalty
	 * @param minWordLength
	 * @param shortCircuitBits
	 *            the number of bits to stop computing password strength if password
	 *            is below
	 * @return
	 * @see <a href=
	 *      "http://cubicspot.blogspot.com/2012/06/how-to-calculate-password-strength-part.html">Based
	 *      on SSO_IsStrongPassword</a>
	 */
	public double ComputePasswordStrength(char[] password, double repeatCharacterPenalty, int minWordLength,
			double shortCircuitBits) {
		/*
		 * MIT License The MIT License
		 * 
		 * Copyright (c) 2013 CubicleSoft
		 * 
		 * Permission is hereby granted, free of charge, to any person obtaining a copy
		 * of this software and associated documentation files (the "Software"), to deal
		 * in the Software without restriction, including without limitation the rights
		 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
		 * copies of the Software, and to permit persons to whom the Software is
		 * furnished to do so, subject to the following conditions:
		 * 
		 * The above copyright notice and this permission notice shall be included in
		 * all copies or substantial portions of the Software.
		 * 
		 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
		 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
		 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
		 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
		 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
		 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
		 * SOFTWARE.
		 */
		double minimumBits = Double.MAX_VALUE;

		/*
		 * Standard
		 */
		double numberOfUppercase = 0.0;
		double numberOfLowercase = 0.0;
		double numberOfNumeric = 0.0;
		double numberOfSpace = 0.0;
		double numberOfOther = 0.0;

		for (int i = 0; i < password.length; i++) {
			Character character = password[i];

			if (Character.isUpperCase(character)) {
				numberOfUppercase += 1.0;
			} else if (Character.isLowerCase(character)) {
				numberOfLowercase += 1.0;
			} else if (Character.isDigit(character)) {
				numberOfNumeric += 1.0;
			} else if (Character.isWhitespace(character)) {
				numberOfSpace += 1.0;
			} else {
				numberOfOther += 1.0;
			}

		}

		/*
		 * NIST password strength rules allow up to 6 extra bits for mixed case and
		 * non-alphabetic.
		 */
		double extraBits;
		if (numberOfUppercase > 0 && numberOfLowercase > 0 && numberOfOther > 0) {
			extraBits = (numberOfNumeric > 0) ? 6 : 5;
		} else if (numberOfNumeric > 0 && !(numberOfUppercase > 0) && !(numberOfLowercase > 0)) {
			extraBits = (numberOfOther > 0) ? -2 : -6;
		} else {
			extraBits = 0;
		}

		if (numberOfSpace > 3) {
			extraBits += 1;
		} else if (numberOfSpace > 0) {
			extraBits += 0;
		} else {
			extraBits += -2;
		}

		double passwordBits = GetNISTNumBits(password, repeatCharacterPenalty) + extraBits;
		minimumBits = Math.min(minimumBits, passwordBits);
		if (minimumBits < shortCircuitBits) {
			return minimumBits;
		}

		char[] lowercasePassword = lowercase(password);
		char[] reverseLowercasePassword = reverseAndLowercase(password);

		/*
		 * Qwerty
		 */
		for (String qwertyString : QWERTY_STRINGS) {
			char[] qLowercasePassword = Arrays.copyOf(lowercasePassword, lowercasePassword.length);
			char[] qReverseLowercasePassword = Arrays.copyOf(reverseLowercasePassword, reverseLowercasePassword.length);

			for (int subsequenceLength = 6; subsequenceLength > 2; subsequenceLength--) {
				for (int i = 0; i < (qwertyString.length() - subsequenceLength); i++) {
					CharSequence subSequence = qwertyString.subSequence(i, i + subsequenceLength);

					for (int l = 0; l < password.length; l++) {
						int s = 0, sr = 0;
						for (; s < subsequenceLength && (s + l) < password.length; s++) {
							if (qLowercasePassword[l + s] != subSequence.charAt(s)) {
								s = 0;
								break;
							}
						}

						for (; sr < subsequenceLength && (sr + l) < password.length; sr++) {
							if (qReverseLowercasePassword[l + sr] != subSequence.charAt(sr)) {
								sr = 0;
								break;
							}
						}

						if (s > 0) {
							for (; s < subsequenceLength && (s + l) < password.length; s++) {
								qLowercasePassword[l + s] = EMPTY_CHARACTER;
							}
						}

						if (sr > 0) {
							for (; s < subsequenceLength && (s + l) < password.length; s++) {
								qLowercasePassword[l + s] = EMPTY_CHARACTER;
							}
						}
					}

				}
				subsequenceLength += -1;
			}

			double qPasswordBits = GetNISTNumBits(qLowercasePassword, repeatCharacterPenalty) + extraBits;
			double qReversePasswordBits = GetNISTNumBits(qReverseLowercasePassword, repeatCharacterPenalty) + extraBits;

			minimumBits = Math.min(minimumBits, qPasswordBits);
			minimumBits = Math.min(minimumBits, qReversePasswordBits);
			if (minimumBits < shortCircuitBits) {
				return minimumBits;
			}

		}

		/*
		 * Dictionary
		 */
		List<char[]> passwords = new ArrayList<>();

		passwords.add(substituteCharacters(lowercasePassword, KEYBOARD_DOWN_NO_SHIFT));
		passwords.add(reverseAndLowercase(substituteCharacters(lowercasePassword, KEYBOARD_DOWN_NO_SHIFT)));

		passwords.add(substituteCharacters(lowercasePassword, KEYBOARD_DOWN_RIGHT_SHIFT));
		passwords.add(reverseAndLowercase(substituteCharacters(lowercasePassword, KEYBOARD_DOWN_RIGHT_SHIFT)));

		passwords.add(substituteCharacters(lowercasePassword, KEYBOARD_DOWN_LEFT_SHIFT));
		passwords.add(reverseAndLowercase(substituteCharacters(lowercasePassword, KEYBOARD_DOWN_LEFT_SHIFT)));

		passwords.addAll(substituteLeakSpeak(password));

		for (char[] shiftedPassword : passwords) {

			for (int x = 0; x < shiftedPassword.length; x++) {
				if (shiftedPassword[x] < 'a' || shiftedPassword[x] > 'z') {
					/*
					 * Speed improvement, all of the words are english lowercase
					 */
					continue;
				}

				int x2 = x + 1;
				while (x2 < shiftedPassword.length && shiftedPassword[x2] >= 'a' && shiftedPassword[x2] <= 'z') {
					x2 = x2 + 1;
				}

				while (((x2 - x) >= minWordLength)) {
					for (int i = x; i < x2 - minWordLength; i++) {
						if (WORD_DICTIONARY.contains(Arrays.copyOfRange(shiftedPassword, i, x2))) {
							minimumBits = Math.min(minimumBits,
									GetNISTNumBits(shiftedPassword, repeatCharacterPenalty, i, x2) + extraBits);
							if (minimumBits < shortCircuitBits) {
								return minimumBits;
							}
						}
					}

					x2--;
				}
			}
		}

		return minimumBits;

	}

	private char[] lowercase(char[] password) {
		char[] lowercasePassword = new char[password.length];

		for (int i = 0; i < password.length; i++) {
			Character character = Character.toLowerCase(password[i]);

			lowercasePassword[i] = character;
		}
		return lowercasePassword;
	}

	private char[] reverseAndLowercase(char[] password) {
		char[] reverseLowercasePassword = new char[password.length];

		for (int i = 0; i < password.length; i++) {
			Character character = Character.toLowerCase(password[password.length - (i + 1)]);

			reverseLowercasePassword[i] = character;
		}
		return reverseLowercasePassword;
	}

	private List<char[]> substituteLeakSpeak(char[] password) {
		List<char[]> result = new ArrayList<>();
		List<char[]> buffer = new ArrayList<>();

		result.add(password);
		result.add(reverseAndLowercase(password));

		for (Entry<Character, Collection<Character>> entry : LEET_SPEAK_MAP.entrySet()) {
			for (char[] previous : result) {
				for (Character replacement : entry.getValue()) {
					buffer.add(substituteCharacters(previous, Collections.singletonMap(entry.getKey(), replacement)));
				}
			}

			if (!buffer.isEmpty()) {
				result = buffer;
				buffer = new ArrayList<>();
			}
		}

		return result;
	}

	private final char[] substituteCharacters(char[] originalArray, Map<Character, Character> substitutionMap) {
		char[] substitutedArray = new char[originalArray.length];

		for (int i = 0; i < originalArray.length; i++) {
			Character substitute = substitutionMap.getOrDefault(originalArray[i], null);
			if (substitute == null) {
				substitutedArray[i] = originalArray[i];
			} else {
				substitutedArray[i] = substitute;
			}
		}

		return substitutedArray;
	}
}
