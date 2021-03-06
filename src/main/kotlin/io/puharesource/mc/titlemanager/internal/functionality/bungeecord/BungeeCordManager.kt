package io.puharesource.mc.titlemanager.internal.functionality.bungeecord

import com.google.common.io.ByteArrayDataInput
import com.google.common.io.ByteStreams
import io.puharesource.mc.titlemanager.internal.extensions.addTo
import io.puharesource.mc.titlemanager.internal.functionality.event.observePluginMessageReceived
import io.puharesource.mc.titlemanager.internal.pluginConfig
import io.puharesource.mc.titlemanager.internal.pluginInstance
import io.puharesource.mc.titlemanager.internal.scheduling.scheduleAsyncTimer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ConcurrentSkipListMap

class BungeeCordManager {
    private val servers: MutableMap<String, ServerInfo> = ConcurrentSkipListMap(String.CASE_INSENSITIVE_ORDER)
    private var currentServer: String? = null
    private val tasks: MutableSet<BukkitTask> = mutableSetOf()

    val onlinePlayers: Int
        get() = servers.values.map { it.playerCount }.sum()

    init {
        scheduleAsyncTimer(period = 200) {
            sendNetworkMessage(Messages.GET_SERVERS.message)
            sendNetworkMessage(Messages.GET_SERVER.message)
        }.addTo(tasks)

        observePluginMessageReceived {
            if (it.channel != Channels.BUNGEECORD.channel) return@observePluginMessageReceived

            try {
                val message = it.message
                val input = ByteStreams.newDataInput(message)

                when (input.readUTF()) {
                    Messages.GET_SERVERS.message -> onGetServers(input)
                    Messages.GET_SERVER.message -> onGetServer(input)
                    Messages.PLAYER_COUNT.message -> onPlayerCount(input)
                }
            } catch (e: Exception) {}
        }
    }

    fun getCurrentServer() = currentServer

    fun getServers() = servers

    fun sendNetworkMessage(vararg args: String, sender: Player? = Bukkit.getOnlinePlayers().firstOrNull()) {
        if (sender != null) {
            val output = ByteStreams.newDataOutput()

            args.forEach { output.writeUTF(it) }

            sender.sendPluginMessage(pluginInstance, Channels.BUNGEECORD.channel, output.toByteArray())
        }
    }

    private fun onGetServers(input: ByteArrayDataInput) {
        val newServers = input.readUTF().split(", ").toSet()

        servers
                .filterKeys { !newServers.contains(it) }
                .forEach { servers.remove(it.key) }

        newServers.filter { !servers.containsKey(it) }.forEach { servers[it] = ServerInfo(it, 0, -1) }

        servers.values.forEach { it.update(this) }
    }

    private fun onGetServer(input: ByteArrayDataInput) {
        val server = input.readUTF()
        currentServer = server

        if (!servers.containsKey(server)) {
            servers[server] = ServerInfo(server, Bukkit.getOnlinePlayers().size, Bukkit.getMaxPlayers())
        } else {
            servers[server]?.playerCount = Bukkit.getOnlinePlayers().size
        }
    }

    private fun onPlayerCount(input: ByteArrayDataInput) {
        val server = input.readUTF()
        val playerCount = input.readInt()

        if (server.equals("ALL", ignoreCase = true)) return

        if (!servers.containsKey(server)) {
            servers[server] = ServerInfo(server, playerCount, -1)
        } else {
            servers[server]?.playerCount = playerCount
        }
    }

    fun invalidate() {
        tasks.forEach { it.cancel() }
    }
}

data class ServerInfo(val name: String, var playerCount: Int = 0, var maxPlayers: Int = 0) {
    fun update(manager: BungeeCordManager) = manager.sendNetworkMessage("PlayerCount", name)
}

private enum class Channels(val channel: String) {
    BUNGEECORD("BungeeCord")
}

private enum class Messages(val message: String) {
    GET_SERVER("GetServer"),
    GET_SERVERS("GetServers"),
    PLAYER_COUNT("PlayerCount")
}