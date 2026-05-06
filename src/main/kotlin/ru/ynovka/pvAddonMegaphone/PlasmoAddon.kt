package ru.ynovka.pvAddonMegaphone

import su.plo.voice.api.server.audio.capture.ServerActivation
import su.plo.voice.api.server.audio.line.ServerSourceLine
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.addon.InjectPlasmoVoice
import su.plo.voice.api.addon.AddonInitializer
import su.plo.voice.api.addon.annotation.Addon
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID


@Addon(
    id = "pv-addon-megaphone",
    name = "Megaphone Plasmo Voice Addon",
    version = "1.0.0",
    authors = [PlasmoAddon.AUTHOR]
)
class PlasmoAddon : AddonInitializer {

    val orators: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    @InjectPlasmoVoice
    lateinit var voiceServer: PlasmoVoiceServer

    lateinit var proximityActivation: ServerActivation
        private set

    lateinit var megaphoneLine: ServerSourceLine
        private set

    private lateinit var router: MegaphoneRouter

    override fun onAddonInitialize() {
        proximityActivation = voiceServer.activationManager
            .getActivationByName("proximity")
            .orElseThrow { IllegalStateException("Proximity activation not found") }

        megaphoneLine = voiceServer.sourceLineManager
            .createBuilder(this, "megaphone", "pv-addon-megaphone.line", "megaphone_icon", 100)
            .setDefaultVolume(1.0)
            .build()

        router = MegaphoneRouter(this)
        voiceServer.eventBus.register(this, router)
    }

    override fun onAddonShutdown() {
        router.close()
    }

    companion object {
        const val AUTHOR = "Ynovka"
    }
}
