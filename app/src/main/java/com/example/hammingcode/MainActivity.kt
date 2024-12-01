package com.example.hammingcode

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val inputMessage = findViewById<EditText>(R.id.inputMessage)
        val errorPosition = findViewById<EditText>(R.id.errorPosition)
        val encodedResult = findViewById<TextView>(R.id.encodedResult)
        val errorResult = findViewById<TextView>(R.id.errorResult)
        val decodedResult = findViewById<TextView>(R.id.decodedResult)

        val btnEncode = findViewById<Button>(R.id.btnEncode)
        val btnAddError = findViewById<Button>(R.id.btnAddError)
        val btnDecode = findViewById<Button>(R.id.btnDecode)

        var encodedMessage: String = ""

        btnEncode.setOnClickListener {
            val message = inputMessage.text.toString()
            if (message.isNotEmpty()) {
                try {
                    encodedMessage = encodeHamming(message)
                    encodedResult.text = "Закодировано: $encodedMessage"
                    errorResult.text = ""
                    decodedResult.text = ""
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Введите сообщение!", Toast.LENGTH_SHORT).show()
            }
        }

        btnAddError.setOnClickListener {
            val position = errorPosition.text.toString().toIntOrNull()
            if (position != null && position in 1..encodedMessage.length) {
                encodedMessage = introduceError(encodedMessage, position)
                errorResult.text = "С ошибкой: $encodedMessage"
            } else {
                Toast.makeText(this, "Введите корректную позицию ошибки!", Toast.LENGTH_SHORT).show()
            }
        }

        btnDecode.setOnClickListener {
            if (encodedMessage.isNotEmpty()) {
                val (decodedMessage, errorCorrected) = decodeHamming(encodedMessage)
                decodedResult.text = if (errorCorrected) {
                    "Исправлено и декодировано: $decodedMessage"
                } else {
                    "Декодировано: $decodedMessage"
                }
            } else {
                Toast.makeText(this, "Сначала закодируйте сообщение!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun encodeHamming(data: String): String {
        if (data.isEmpty()) {
            throw IllegalArgumentException("Входная строка пуста")
        }

        val binaryData = try {
            data.toCharArray().joinToString("") {
                it.code.toString(2).padStart(8, '0')
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Ошибка при преобразовании символов в двоичный код")
        }

        val encoded = StringBuilder()
        var i = 0
        while (i < binaryData.length) {
            val endIndex = (i + 4).coerceAtMost(binaryData.length)
            val block = binaryData.substring(i, endIndex)
            encoded.append(addParityBits(block))
            i = endIndex
        }
        return encoded.toString()
    }

    private fun addParityBits(block: String): String {
        val dataBits = block.padEnd(4, '0')
        val p1 = (dataBits[0].digitToInt() xor dataBits[1].digitToInt() xor dataBits[3].digitToInt()) % 2
        val p2 = (dataBits[0].digitToInt() xor dataBits[2].digitToInt() xor dataBits[3].digitToInt()) % 2
        val p4 = (dataBits[1].digitToInt() xor dataBits[2].digitToInt() xor dataBits[3].digitToInt()) % 2
        return "$p1$p2${dataBits[0]}$p4${dataBits.substring(1)}"
    }

    private fun introduceError(encoded: String, position: Int): String {
        val charArray = encoded.toCharArray()
        val index = position - 1
        charArray[index] = if (charArray[index] == '0') '1' else '0'
        return String(charArray)
    }

    private fun decodeHamming(encoded: String): Pair<String, Boolean> {
        val decoded = StringBuilder()
        var errorCorrected = false
        for (i in encoded.indices step 7) {
            val block = encoded.substring(i, i + 7.coerceAtMost(encoded.length))
            val (correctedBlock, hasError) = correctBlock(block)
            decoded.append(correctedBlock)
            if (hasError) errorCorrected = true
        }
        val text = decoded.toString()
            .chunked(8)
            .map { it.toIntOrNull(2)?.toChar() ?: '?' }
            .joinToString("")
        return Pair(text, errorCorrected)
    }

    private fun correctBlock(block: String): Pair<String, Boolean> {
        val p1 = (block[0].digitToInt() xor block[2].digitToInt() xor block[4].digitToInt() xor block[6].digitToInt())
        val p2 = (block[1].digitToInt() xor block[2].digitToInt() xor block[5].digitToInt() xor block[6].digitToInt())
        val p4 = (block[3].digitToInt() xor block[4].digitToInt() xor block[5].digitToInt() xor block[6].digitToInt())
        val errorPosition = p1 * 1 + p2 * 2 + p4 * 4
        val correctedBlock = if (errorPosition > 0) {
            val index = errorPosition - 1
            val charArray = block.toCharArray()
            charArray[index] = if (charArray[index] == '0') '1' else '0'
            String(charArray)
        } else block
        return Pair(correctedBlock.substring(2, 3) + correctedBlock.substring(4), errorPosition > 0)
    }
}
