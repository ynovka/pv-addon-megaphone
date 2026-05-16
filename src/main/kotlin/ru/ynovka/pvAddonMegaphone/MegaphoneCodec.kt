package ru.ynovka.pvAddonMegaphone

import su.plo.voice.api.audio.codec.CodecException
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.encryption.EncryptionException
import kotlin.math.abs

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

    private var noisePhase = 0
    private var noiseState = 0x12345678

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

            var sample = (held * 1.15).toInt()
            sample = sample.coerceIn(-15000, 15000)
            sample = (sample / 192) * 192

            x = sample.toDouble()

            val noise = nextNoise() * 1260.0

            noisePhase++

            val pulsePeriod = 504
            val pulsePos = noisePhase % pulsePeriod

            val pulse = if (pulsePos < pulsePeriod / 2) {
                1.0
            } else {
                0.15
            }

            val voiceGate = (abs(input[i].toDouble()) / 6000.0)
                .coerceIn(0.0, 1.0)

            x += noise * pulse * (0.25 + voiceGate)
            x = x.coerceIn(-14000.0, 14000.0)

            out[i] = x.toInt().toShort()
        }

        return out
    }

    private fun nextNoise(): Double {
        noiseState = noiseState xor (noiseState shl 13)
        noiseState = noiseState xor (noiseState ushr 17)
        noiseState = noiseState xor (noiseState shl 5)

        return ((noiseState and 0xFFFF) / 32768.0) - 1.0
    }

    @Synchronized
    override fun close() {
        decoder.close()
        encoder.close()
    }
}