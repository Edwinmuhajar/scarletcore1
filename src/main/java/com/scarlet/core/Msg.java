package com.scarlet.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/** Pengambil pesan dari messages.yml (format MiniMessage). */
public final class Msg {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ScarletCore plugin;
    private YamlConfiguration messages;

    public Msg(ScarletCore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    private static TagResolver resolvers(String... kv) {
        TagResolver.Builder b = TagResolver.builder();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            b.resolver(Placeholder.unparsed(kv[i], kv[i + 1]));
        }
        return b.build();
    }

    /** Pesan tanpa prefix. */
    public Component raw(String key, String... kv) {
        String t = messages.getString(key, "<red>Pesan tidak ditemukan: " + key);
        return MM.deserialize(t, resolvers(kv));
    }

    /** Pesan dengan prefix. */
    public Component get(String key, String... kv) {
        String prefix = messages.getString("prefix", "");
        String t = messages.getString(key, "<red>Pesan tidak ditemukan: " + key);
        return MM.deserialize(prefix + t, resolvers(kv));
    }

    public void send(CommandSender to, String key, String... kv) {
        to.sendMessage(get(key, kv));
    }
}
