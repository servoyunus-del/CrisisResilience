package com.emre.crisisresilience.utils.helpers

object MorseCodeHelper {
    
    const val DOT_DURATION = 200L
    const val DASH_DURATION = 600L
    const val ELEMENT_SPACE = 200L // Aynı harf içindeki nokta/çizgiler arası
    const val LETTER_SPACE = 600L // Harfler arası boşluk
    const val WORD_SPACE = 1400L // Kelimeler arası boşluk

    private val MORSE_DICTIONARY = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
        'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
        'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
        'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
        'Y' to "-.--", 'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----."
    )

    fun textToMorse(text: String): String {
        return text.uppercase().map { char ->
            if (char == ' ') " " // Kelime boşluğu için özel işaret
            else MORSE_DICTIONARY[char] ?: ""
        }.filter { it.isNotEmpty() }.joinToString("|") 
        // "|" karakterini harfleri ayırmak için kullanacağız, " " karakterini kelimeleri ayırmak için
    }
}
