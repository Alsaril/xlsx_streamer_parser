package ru.evotor.report

object TransliterationUtils {

    private val MAP = mapOf(
            'а' to "a",
            'б' to "b",
            'в' to "v",
            'г' to "g",
            'д' to "d",
            'е' to "e",
            'ё' to "yo",
            'ж' to "zh",
            'з' to "z",
            'и' to "i",
            'й' to "y",
            'к' to "k",
            'л' to "l",
            'м' to "m",
            'н' to "n",
            'о' to "o",
            'п' to "p",
            'р' to "r",
            'с' to "s",
            'т' to "t",
            'у' to "u",
            'ф' to "f",
            'х' to "h",
            'ц' to "ts",
            'ч' to "ch",
            'ш' to "sh",
            'щ' to "sch",
            'ъ' to "",
            'ы' to "yi",
            'ь' to "",
            'э' to "e",
            'ю' to "yu",
            'я' to "ya",
            ' ' to " ")

    fun transliterate(value: String) = value
            .trim()
            .toLowerCase()
            .asSequence()
            .map { MAP[it] ?: it.toString() }
            .joinToString { it }
}
