package io.github.malczuuu.redfox.gateway.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CookieCrypto(private val secretKey: SecretKey) {

  companion object {
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val ALGORITHM = "AES/GCM/NoPadding"
  }

  fun encrypt(plaintext: String): String {
    val iv = ByteArray(GCM_IV_LENGTH)
    SecureRandom().nextBytes(iv)
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
    val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    val combined = iv + ciphertext
    return Base64.getUrlEncoder().withoutPadding().encodeToString(combined)
  }

  fun decrypt(encrypted: String): String {
    val combined = Base64.getUrlDecoder().decode(encrypted)
    val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
    val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
    return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
  }
}
