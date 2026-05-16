package ru.ynovka.pvAddonMegaphone

import su.plo.voice.api.server.event.audio.capture.PlayerServerActivationStartEvent
import su.plo.voice.api.server.event.audio.capture.PlayerServerActivationEndEvent
import su.plo.voice.api.server.event.audio.capture.PlayerServerActivationEvent
import su.plo.voice.api.server.audio.capture.PlayerActivationInfo
import su.plo.voice.api.server.audio.source.ServerPlayerSource
import su.plo.voice.api.server.audio.capture.ServerActivation
import su.plo.voice.api.server.player.VoiceServerPlayer
import java.util.concurrent.ConcurrentHashMap
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.event.EventPriority
import java.util.UUID


class MegaphoneRouter(
    private val addon: PlasmoAddon
) : AutoCloseable {

    private val sources = ConcurrentHashMap<UUID, ServerPlayerSource>()
    private val codecs = ConcurrentHashMap<UUID, MegaphoneCodec>()

    @EventSubscribe(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onStart(event: PlayerServerActivationStartEvent) {
        val player = event.player as? VoiceServerPlayer ?: return
        if (!shouldUseMegaphone(player) || event.activation != addon.proximityActivation) return

        sources.computeIfAbsent(player.instance.uuid) {
            addon.megaphoneLine.createPlayerSource(player, false).apply {
                setName(player.instance.name)
            }
        }
    }

    @EventSubscribe(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onFrame(event: PlayerServerActivationEvent) {
        val player = event.player as? VoiceServerPlayer ?: return
        if (!shouldUseMegaphone(player) || event.activation != addon.proximityActivation) return

        val source = sources.computeIfAbsent(player.instance.uuid) {
            addon.megaphoneLine.createPlayerSource(player, false)
        }

        val distance: Short = 92
        val rawFrame = event.packet.data

        val processedFrame = codecs
            .computeIfAbsent(player.instance.uuid) { MegaphoneCodec(addon.voiceServer) }
            .process(rawFrame)

        processedFrame?.let {
            source.sendAudioFrame(
                processedFrame,
                event.packet.sequenceNumber,
                distance,
                PlayerActivationInfo(event.player, event.packet)
            )

            event.result = ServerActivation.Result.HANDLED
        }
    }

    @EventSubscribe(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onEnd(event: PlayerServerActivationEndEvent) {
        val player = event.player as? VoiceServerPlayer ?: return
        if (!shouldUseMegaphone(player) || event.activation != addon.proximityActivation) return

        val distance: Short = 92

        sources.remove(player.instance.uuid)?.let { source ->
            source.sendAudioEnd(event.packet.sequenceNumber, distance)
            source.remove()
        }

        codecs.remove(player.instance.uuid)?.close()
        event.result = ServerActivation.Result.HANDLED
    }

    private fun shouldUseMegaphone(player: VoiceServerPlayer): Boolean {
        return player.instance.uuid in addon.orators
    }

    override fun close() {
        sources.values.forEach { it.remove() }
        sources.clear()
        codecs.values.forEach { it.close() }
        codecs.clear()
    }
}