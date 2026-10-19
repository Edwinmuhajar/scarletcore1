package com.scarlet.core;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class Events implements Listener {

    private final ScarletCore pl;

    public Events(ScarletCore pl) {
        this.pl = pl;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        pl.applyNick(p);

        // Sembunyikan staff yang sedang vanish dari player yang baru masuk
        if (!p.hasPermission("scarletcore.vanish.see")) {
            for (UUID id : pl.vanished) {
                Player v = Bukkit.getPlayer(id);
                if (v != null) p.hidePlayer(pl, v);
            }
        }

        if (pl.getConfig().getBoolean("join-quit.enabled", true)) {
            e.joinMessage(pl.joinQuitMessage(p, true));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        if (pl.vanished.remove(id)) {
            e.quitMessage(null); // staff vanish keluar diam-diam
        } else if (pl.getConfig().getBoolean("join-quit.enabled", true)) {
            e.quitMessage(pl.joinQuitMessage(p, false));
        }

        pl.gods.remove(id);
        pl.staffChat.remove(id);
        // frozen sengaja TIDAK dihapus supaya tidak bisa kabur lewat relog
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && pl.gods.contains(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && pl.gods.contains(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (pl.frozen.contains(e.getPlayer().getUniqueId()) && e.hasChangedPosition()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        if (!pl.staffChat.contains(p.getUniqueId())) return;
        if (!p.hasPermission("scarletcore.staffchat")) {
            pl.staffChat.remove(p.getUniqueId());
            return;
        }
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message());
        pl.broadcastStaff(p.getName(), text);
    }
}
