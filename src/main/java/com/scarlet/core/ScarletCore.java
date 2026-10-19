package com.scarlet.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScarletCore extends JavaPlugin {

    private static final String[] COMMANDS = {
            "fly", "heal", "feed", "god", "speed", "nick", "hat", "ec", "craft",
            "vanish", "invsee", "endersee", "tp", "tphere", "tpall", "freeze",
            "staffchat", "kit", "scarletcore"
    };

    public final Set<UUID> vanished = ConcurrentHashMap.newKeySet();
    public final Set<UUID> frozen = ConcurrentHashMap.newKeySet();
    public final Set<UUID> gods = ConcurrentHashMap.newKeySet();
    public final Set<UUID> staffChat = ConcurrentHashMap.newKeySet();

    private Msg msg;
    private File dataFile;
    private YamlConfiguration data;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        msg = new Msg(this);

        getDataFolder().mkdirs();
        dataFile = new File(getDataFolder(), "data.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);

        Cmds cmds = new Cmds(this);
        for (String name : COMMANDS) {
            PluginCommand c = getCommand(name);
            if (c == null) {
                getLogger().warning("Command tidak ada di plugin.yml: " + name);
                continue;
            }
            c.setExecutor(cmds);
            c.setTabCompleter(cmds);
        }

        getServer().getPluginManager().registerEvents(new Events(this), this);

        for (Player p : Bukkit.getOnlinePlayers()) {
            applyNick(p);
        }
        getLogger().info("ScarletCore aktif!");
    }

    @Override
    public void onDisable() {
        // Pastikan semua player terlihat lagi kalau plugin dimatikan
        for (UUID id : vanished) {
            Player v = Bukkit.getPlayer(id);
            if (v == null) continue;
            for (Player o : Bukkit.getOnlinePlayers()) {
                o.showPlayer(this, v);
            }
        }
        saveData();
    }

    public Msg msg() { return msg; }

    public YamlConfiguration data() { return data; }

    public void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Gagal menyimpan data.yml: " + e.getMessage());
        }
    }

    public void reloadAll() {
        reloadConfig();
        msg.reload();
    }

    // ---------- util ----------

    public void applyNick(Player p) {
        String raw = data.getString("nicks." + p.getUniqueId());
        if (raw == null) {
            p.displayName(null);
            p.playerListName(null);
            return;
        }
        Component c = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
        p.displayName(c);
        p.playerListName(c);
    }

    // ---------- cooldown (disimpan di data.yml, tahan restart) ----------

    public long cooldownLeft(Player p, String key, int seconds) {
        if (p.hasPermission("scarletcore.cooldown.bypass")) return 0;
        long last = data.getLong("cooldowns." + p.getUniqueId() + "." + key, 0L);
        long left = (last + seconds * 1000L - System.currentTimeMillis() + 999) / 1000;
        return Math.max(left, 0);
    }

    public void stampCooldown(Player p, String key) {
        data.set("cooldowns." + p.getUniqueId() + "." + key, System.currentTimeMillis());
        saveData();
    }

    public static String formatTime(long secs) {
        long h = secs / 3600;
        long m = (secs % 3600) / 60;
        long s = secs % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("j ");
        if (m > 0) sb.append(m).append("m ");
        if (s > 0 || sb.length() == 0) sb.append(s).append("d");
        return sb.toString().trim();
    }

    // ---------- staff chat & join/quit ----------

    public void broadcastStaff(String sender, String text) {
        Component c = msg.raw("staffchat-format", "player", sender, "message", text);
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (o.hasPermission("scarletcore.staffchat")) {
                o.sendMessage(c);
            }
        }
        Bukkit.getConsoleSender().sendMessage(c);
    }

    public Component joinQuitMessage(Player p, boolean join) {
        String tpl = getConfig().getString(join ? "join-quit.default-join" : "join-quit.default-quit", "");
        ConfigurationSection ranks = getConfig().getConfigurationSection("join-quit.ranks");
        if (ranks != null) {
            for (String rank : ranks.getKeys(false)) {
                if (p.hasPermission("group." + rank)) {
                    String t = ranks.getString(rank + (join ? ".join" : ".quit"));
                    if (t != null) tpl = t;
                    break;
                }
            }
        }
        if (tpl == null || tpl.isEmpty()) return null;
        return MiniMessage.miniMessage().deserialize(tpl, Placeholder.unparsed("player", p.getName()));
    }
}
