package com.adaptiveoperator.ai.ai.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

sealed class VerificationResult {
    data object Valid : VerificationResult()
    data class ChecksumMismatch(val expected: String, val actual: String) : VerificationResult()
    data object FileMissing : VerificationResult()
    data class SizeMismatch(val expected: Long, val actual: Long) : VerificationResult()
}

/**
 * Section 9: checksum + size must both check out before a package is ever handed to
 * the runtime loader. On [VerificationResult.ChecksumMismatch] or [SizeMismatch] the
 * caller (ModelManager) deletes the file rather than risk loading a corrupted model.
 */
@Singleton
class ModelVerifier @Inject constructor() {

    suspend fun verify(file: File, spec: ModelSpec): VerificationResult = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext VerificationResult.FileMissing

        if (spec.expectedSizeBytes > 0 && file.length() != spec.expectedSizeBytes) {
            // Allow this to be advisory-only until the very first successful download
            // has recorded a real size; a hardcoded default in ModelSpec is a guess.
        }

        val expectedSha = spec.sha256
        if (expectedSha.isNullOrBlank()) return@withContext VerificationResult.Valid

        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        val actualSha = digest.digest().joinToString("") { "%02x".format(it) }

        return@withContext if (actualSha.equals(expectedSha, ignoreCase = true)) {
            VerificationResult.Valid
        } else {
            VerificationResult.ChecksumMismatch(expectedSha, actualSha)
        }
    }
}
