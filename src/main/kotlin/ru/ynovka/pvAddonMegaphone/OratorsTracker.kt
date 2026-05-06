package ru.ynovka.pvAddonMegaphone

import ru.ynovka.pvAddonMegaphone.PvAddonMegaphone.Companion.plasmo
import ru.ynovka.pvAddonMegaphone.PvAddonMegaphone.Companion.inst
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.event.EventPriority
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.entity.Player
import org.bukkit.Material


object OratorsTracker : Listener {

    fun register() {
        inst.server.pluginManager.registerEvents(this, inst)

        for (player in inst.server.onlinePlayers) {
            refresh(player)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEquipmentChanged(event: EntityEquipmentChangedEvent) {
        val player = event.entity as? Player ?: return

        val changedSlots = event.equipmentChanges
        if (EquipmentSlot.HAND !in changedSlots && EquipmentSlot.OFF_HAND !in changedSlots) {
            return
        }

        refresh(player)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        refresh(event.player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        plasmo.orators.remove(event.player.uniqueId)
    }

    private fun refresh(player: Player) {
        val inventory = player.inventory
        val hasMegaphoneInHand =
            inventory.itemInMainHand.type == Material.IRON_INGOT ||
                    inventory.itemInOffHand.type == Material.IRON_INGOT

        if (hasMegaphoneInHand) {
            plasmo.orators.add(player.uniqueId)
        } else {
            plasmo.orators.remove(player.uniqueId)
        }
    }
}
