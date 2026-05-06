package ru.ynovka.pvAddonMegaphone

import su.plo.voice.api.server.PlasmoVoiceServer
import org.bukkit.plugin.java.JavaPlugin


class PvAddonMegaphone : JavaPlugin() {
    companion object {
        lateinit var inst: PvAddonMegaphone
            private set
        val plasmo = PlasmoAddon()
    }

    override fun onLoad() {
        inst = this
        PlasmoVoiceServer.getAddonsLoader().load(plasmo)
    }

    override fun onEnable() {
        OratorsTracker.register()
    }
}
