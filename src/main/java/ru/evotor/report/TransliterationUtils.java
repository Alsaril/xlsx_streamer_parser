package ru.evotor.report;

import java.util.HashMap;
import java.util.Map;

public class TransliterationUtils {

	private static final Map<String, String> MAP = new HashMap<>();
	static {
		MAP.put("а", "a");
		MAP.put("б", "b");
		MAP.put("в", "v");
		MAP.put("г", "g");
		MAP.put("д", "d");
		MAP.put("е", "e");
		MAP.put("ё", "yo");
		MAP.put("ж", "zh");
		MAP.put("з", "z");
		MAP.put("и", "i");
		MAP.put("й", "y");
		MAP.put("к", "k");
		MAP.put("л", "l");
		MAP.put("м", "m");
		MAP.put("н", "n");
		MAP.put("о", "o");
		MAP.put("п", "p");
		MAP.put("р", "r");
		MAP.put("с", "s");
		MAP.put("т", "t");
		MAP.put("у", "u");
		MAP.put("ф", "f");
		MAP.put("х", "h");
		MAP.put("ц", "ts");
		MAP.put("ч", "ch");
		MAP.put("ш", "sh");
		MAP.put("щ", "sch");
		MAP.put("ъ", "");
		MAP.put("ы", "");
		MAP.put("ь", "");
		MAP.put("э", "e");
		MAP.put("ю", "yu");
		MAP.put("я", "ya");
	}

	public static String transliterate(String value) {
		String formattedValue = value.trim()
				.replace(" ", "_")
				.toLowerCase();
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < formattedValue.length(); i++) {
			String wordStr = String.valueOf(formattedValue.charAt(i));
			String transliteratedValue = MAP.get(wordStr);
			if (transliteratedValue != null) {
				builder.append(transliteratedValue);
			} else {
				builder.append(wordStr);
			}
		}
		return builder.toString();
	}
}
