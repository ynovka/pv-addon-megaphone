package ru.ynovka.pvAddonMegaphone

import su.plo.voice.api.audio.codec.CodecException
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.encryption.EncryptionException

class MegaphoneCodec(
    voiceServer: PlasmoVoiceServer
) : AutoCloseable {

    private val decoder = voiceServer.createOpusDecoder(false)
    private val encoder = voiceServer.createOpusEncoder(false)
    private val encryption = voiceServer.defaultEncryption

    @Synchronized
    fun process(encryptedFrame: ByteArray): ByteArray? {
        return try {
            val decrypted = encryption.decrypt(encryptedFrame)
            val decoded = decoder.decode(decrypted)

            if (decoded.size != 960) {
                return null
            }

            val distorted = distort(decoded)

            if (distorted.size != 960) {
                return null
            }

            val encoded = encoder.encode(distorted)
            encryption.encrypt(encoded)
        } catch (e: CodecException) {
            null
        } catch (e: EncryptionException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: IllegalStateException) {
            null
        }
    }

    private var hpPrevIn = 0.0
    private var hpPrevOut = 0.0
    private var lpPrevOut = 0.0

    private fun distort(input: ShortArray): ShortArray {
        val out = ShortArray(input.size)
        var held = 0

        for (i in input.indices) {
            var x = input[i].toDouble()

            val hp = 0.985 * (hpPrevOut + x - hpPrevIn)
            hpPrevIn = x
            hpPrevOut = hp

            lpPrevOut += 0.22 * (hp - lpPrevOut)
            x = lpPrevOut

            if (i % 2 == 0) {
                held = x.toInt()
            }

            var sample = (held * 1.15 * 1.25).toInt()
            sample = sample.coerceIn(-18000, 18000)
            sample = (sample / 192) * 192

            x = sample.toDouble()
            x = x.coerceIn(-18000.0, 18000.0)

            out[i] = x.toInt().toShort()
        }

        return out
    }

    @Synchronized
    override fun close() {
        decoder.close()
        encoder.close()
    }
}