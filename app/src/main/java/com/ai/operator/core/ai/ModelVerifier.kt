package com.ai.operator.core.ai

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class ModelVerifier {

    fun verifyChecksum(file: File, expectedChecksum: String): Boolean {
        if (!file.exists()) return false
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            val fis = FileInputStream(file)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            fis.close()

            val sha256Bytes = digest.digest()
            val hexString = StringBuilder()
            for (byte in sha256Bytes) {
                val hex = Integer.toHexString(0xff and byte.toInt())
                if (hex.length == 1) hexString.append('0')
                hexString.append(hex)
            }

            hexString.toString().equals(expectedChecksum, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
}
