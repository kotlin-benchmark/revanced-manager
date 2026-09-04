package app.revanced.manager.domain

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.File
import java.io.ObjectInputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Resolves auxiliary payloads referenced by an incoming `revanced-manager://` deep link:
 * a document to preview, an integrity signature and an encrypted blob.
 */
class DeepLinkPayloadHandler {

    fun loadDocument(path: String): ByteArray {
        val safePath = sanitizePath(path)
        val file = File(safePath)

        //CWE-22
        //SINK
        return file.readBytes()
    }

    /**
     * Removes parent-directory sequences before opening the file.
     */
    private fun sanitizePath(path: String): String {
        return path.replace("../", "")
    }

    fun restoreState(encoded: String): Any? {
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        if (!isTrustedPayload(bytes)) return null

        val stream = ObjectInputStream(ByteArrayInputStream(bytes))

        //CWE-502
        //SINK
        return stream.readObject()
    }

    /**
     * Verifies the payload is a serialized object before restoring it.
     */
    private fun isTrustedPayload(bytes: ByteArray): Boolean {
        // Java serialization stream header magic (0xACED).
        return bytes.size >= 2 &&
            bytes[0] == 0xAC.toByte() &&
            bytes[1] == 0xED.toByte()
    }

    fun verifyIntegrity(payload: String): ByteArray {
        val bytes = payload.toByteArray()

        //CWE-328
        //SINK
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(bytes)
    }

    fun decryptPayload(encoded: String): ByteArray {
        val raw = Base64.decode(encoded, Base64.DEFAULT)
        val keySpec = SecretKeySpec("8bytekey".toByteArray(), "DES")

        //CWE-327
        //SINK
        val cipher = Cipher.getInstance("DES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        return cipher.doFinal(raw)
    }
}
