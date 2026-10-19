package com.scarlet.core;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Cmds implements TabExecutor {

    private static final Map<String, String> PERMS = new HashMap<>();

    private static void reg(String perm, String... names) {
        for (String n : names) PERMS.put(n, perm);
    }

    static {
        reg("scarletcore.fly", "fly");
        reg("scarletcore.heal", "heal");
        reg("scarletcore.feed", "feed");
        reg("scarletcore.god", "god");
        reg("scarletcore.speed", "speed");
        reg("scarletcore.nick", "nick");
        reg("scarletcore.hat", "hat");
        reg("scarletcore.ec", "ec");
        reg("scarletcore.craft", "craft");
        reg("scarletcore.vanish", "vanish");
        reg("scarletcore.invsee", "invsee");
        reg("scarletcore.endersee", "endersee");
        reg("scarletcore.tp", "tp");
        reg("scarletcore.tphere", "tphere");
        reg("scarletcore.tpall", "tpall");
        reg("scarletcore.freeze", "freeze");
        reg("scarletcore.staffchat", "staffchat");
        reg("scarletcore.kit", "kit");
        reg("scarletcore.reload", "scarletcore");
    }

    private final ScarletCore pl;
    private final Msg msg;

    public Cmds(ScarletCore pl) {
        this.pl = pl;
        this.msg = pl.msg();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {
        String name = cmd.getName().toLowerCase(Locale.ROOT);
        String perm = PERMS.get(name);
        if (perm != null && !sender.hasPermission(perm)) {
            msg.send(sender, "no-permission");
            return true;
        }
        switch (name) {
            case "fly" -> fly(sender, a);
            case "heal" -> heal(sender, a);
            case "feed" -> feed(sender, a);
            case "god" -> god(sender, a);
            case "speed" -> speed(sender, a);
            case "nick" -> nick(sender, a);
            case "hat" -> hat(sender);
            case "ec" -> ec(sender);
            case "craft" -> craft(sender);
            case "vanish" -> vanish(sender);
            case "invsee" -> see(sender, a, false);
            case "endersee" -> see(sender, a, true);
            case "tp" -> tp(sender, a);
            case "tphere" -> tphere(sender, a);
            case "tpall" -> tpall(sender);
            case "freeze" -> freeze(sender, a);
            case "staffchat" -> staffchat(sender, a);
            case "kit" -> kit(sender, a);
            case "scarletcore" -> admin(sender, a);
            default -> {
                return false;
            }
        }
        return true;
    }

    // ---------- helper ----------

    private Player player(CommandSender s) {
        if (s instanceof Player p) return p;
        msg.send(s, "only-player");
        return null;
    }

    /** Target = argumen ke-idx (butuh izin .others), atau diri sendiri kalau tidak ada argumen. */
    private Player resolveTarget(CommandSender s, String[] a, int idx, String othersPerm) {
        if (a.length > idx) {
            if (!s.hasPermission(othersPerm)) {
                msg.send(s, "no-permission");
                return null;
            }
            Player t = Bukkit.getPlayerExact(a[idx]);
            if (t == null) {
                msg.send(s, "player-not-found", "player", a[idx]);
                return null;
            }
            return t;
        }
        return player(s);
    }

    private Player findPlayer(CommandSender s, String name) {
        Player t = Bukkit.getPlayerExact(name);
        if (t == null) msg.send(s, "player-not-found", "player", name);
        return t;
    }

    private boolean cooldownOk(Player p, String key) {
        int secs = pl.getConfig().getInt("cooldowns." + key, 0);
        if (secs <= 0) return true;
        long left = pl.cooldownLeft(p, key, secs);
        if (left > 0) {
            msg.send(p, "cooldown", "time", ScarletCore.formatTime(left));
            return false;
        }
        pl.stampCooldown(p, key);
        return true;
    }

    private void usage(CommandSender s, String u) {
        msg.send(s, "usage", "usage", u);
    }

    // ---------- perk donatur ----------

    private void fly(CommandSender s, String[] a) {
        Player t = resolveTarget(s, a, 0, "scarletcore.fly.others");
        if (t == null) return;
        org.bukkit.GameMode gm = t.getGameMode();
        if (gm == org.bukkit.GameMode.CREATIVE || gm == org.bukkit.GameMode.SPECTATOR) {
            msg.send(s, "fly-gamemode", "player", t.getName());
            return;
        }
        boolean on = !t.getAllowFlight();
        t.setAllowFlight(on);
        if (!on) t.setFlying(false);
        msg.send(t, on ? "fly-on" : "fly-off");
        if (t != s) msg.send(s, on ? "fly-other-on" : "fly-other-off", "player", t.getName());
    }

    private void heal(CommandSender s, String[] a) {
        Player t = resolveTarget(s, a, 0, "scarletcore.heal.others");
        if (t == null) return;
        if (s instanceof Player p && p == t && !cooldownOk(p, "heal")) return;
        AttributeInstance max = t.getAttribute(Attribute.MAX_HEALTH);
        t.setHealth(max != null ? max.getValue() : 20.0);
        t.setFoodLevel(20);
        t.setSaturation(20f);
        t.setFireTicks(0);
        msg.send(t, "heal-done");
        if (t != s) msg.send(s, "heal-other", "player", t.getName());
    }

    private void feed(CommandSender s, String[] a) {
        Player t = resolveTarget(s, a, 0, "scarletcore.heal.others");
        if (t == null) return;
        if (s instanceof Player p && p == t && !cooldownOk(p, "feed")) return;
        t.setFoodLevel(20);
        t.setSaturation(20f);
        msg.send(t, "feed-done");
        if (t != s) msg.send(s, "feed-other", "player", t.getName());
    }

    private void god(CommandSender s, String[] a) {
        Player t = resolveTarget(s, a, 0, "scarletcore.god.others");
        if (t == null) return;
        UUID id = t.getUniqueId();
        boolean on = !pl.gods.contains(id);
        if (on) pl.gods.add(id);
        else pl.gods.remove(id);
        msg.send(t, on ? "god-on" : "god-off");
        if (t != s) msg.send(s, on ? "god-other-on" : "god-other-off", "player", t.getName());
    }

    private void speed(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return;
        if (a.length == 0) {
            usage(p, "/speed <1-10|reset>");
            return;
        }
        if (a[0].equalsIgnoreCase("reset")) {
            p.setWalkSpeed(0.2f);
            p.setFlySpeed(0.1f);
            msg.send(p, "speed-reset");
            return;
        }
        float v;
        try {
            v = Float.parseFloat(a[0]);
        } catch (NumberFormatException e) {
            usage(p, "/speed <1-10|reset>");
            return;
        }
        if (v < 1 || v > 10) {
            usage(p, "/speed <1-10|reset>");
            return;
        }
        if (p.isFlying()) {
            p.setFlySpeed(v / 10f);
            msg.send(p, "speed-fly", "speed", a[0]);
        } else {
            p.setWalkSpeed(v / 10f);
            msg.send(p, "speed-walk", "speed", a[0]);
        }
    }

    private void nick(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return;
        if (a.length == 0) {
            usage(p, "/nick <nama|reset>");
            return;
        }
        String key = "nicks." + p.getUniqueId();
        if (a[0].equalsIgnoreCase("reset")) {
            pl.data().set(key, null);
            pl.saveData();
            pl.applyNick(p);
            msg.send(p, "nick-reset");
            return;
        }
        String raw = a[0];
        String plain = PlainTextComponentSerializer.plainText()
                .serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(raw));
        if (plain.length() < 3 || plain.length() > 16 || !plain.matches("[A-Za-z0-9_]+")) {
            msg.send(p, "nick-invalid");
            return;
        }
        pl.data().set(key, raw);
        pl.saveData();
        pl.applyNick(p);
        msg.send(p, "nick-set", "nick", plain);
    }

    private void hat(CommandSender s) {
        Player p = player(s);
        if (p == null) return;
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            msg.send(p, "hat-empty");
            return;
        }
        ItemStack one = hand.clone();
        one.setAmount(1);
        ItemStack rest = hand.clone();
        rest.setAmount(hand.getAmount() - 1);
        p.getInventory().setItemInMainHand(rest.getAmount() > 0 ? rest : null);

        ItemStack old = p.getInventory().getHelmet();
        p.getInventory().setHelmet(one);
        if (old != null && !old.getType().isAir()) {
            for (ItemStack left : p.getInventory().addItem(old).values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), left);
            }
        }
        msg.send(p, "hat-done");
    }

    private void ec(CommandSender s) {
        Player p = player(s);
        if (p == null) return;
        p.openInventory(p.getEnderChest());
    }

    private void craft(CommandSender s) {
        Player p = player(s);
        if (p == null) return;
        p.openWorkbench(null, true);
    }

    // ---------- tools staff ----------

    private void vanish(CommandSender s) {
        Player p = player(s);
        if (p == null) return;
        UUID id = p.getUniqueId();
        boolean on = !pl.vanished.contains(id);
        if (on) pl.vanished.add(id);
        else pl.vanished.remove(id);
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (o.equals(p)) continue;
            if (on) {
                if (!o.hasPermission("scarletcore.vanish.see")) o.hidePlayer(pl, p);
            } else {
                o.showPlayer(pl, p);
            }
        }
        msg.send(p, on ? "vanish-on" : "vanish-off");
    }

    private void see(CommandSender s, String[] a, boolean ender) {
        Player p = player(s);
        if (p == null) return;
        if (a.length == 0) {
            usage(p, ender ? "/endersee <player>" : "/invsee <player>");
            return;
        }
        Player t = findPlayer(p, a[0]);
        if (t == null) return;
        p.openInventory(ender ? t.getEnderChest() : t.getInventory());
    }

    private void tp(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return;
        if (a.length == 0) {
            usage(p, "/tp <player>");
            return;
        }
        Player t = findPlayer(p, a[0]);
        if (t == null) return;
        p.teleport(t.getLocation());
        msg.send(p, "tp-done", "player", t.getName());
    }

    private void tphere(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return;
        if (a.length == 0) {
            usage(p, "/tphere <player>");
            return;
        }
        Player t = findPlayer(p, a[0]);
        if (t == null) return;
        t.teleport(p.getLocation());
        msg.send(p, "tphere-done", "player", t.getName());
        msg.send(t, "tphere-moved", "player", p.getName());
    }

    private void tpall(CommandSender s) {
        Player p = player(s);
        if (p == null) return;
        int n = 0;
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (o.equals(p)) continue;
            o.teleport(p.getLocation());
            msg.send(o, "tphere-moved", "player", p.getName());
            n++;
        }
        msg.send(p, "tpall-done", "count", String.valueOf(n));
    }

    private void freeze(CommandSender s, String[] a) {
        if (a.length == 0) {
            usage(s, "/freeze <player>");
            return;
        }
        Player t = findPlayer(s, a[0]);
        if (t == null) return;
        if (t.hasPermission("scarletcore.freeze.bypass")) {
            msg.send(s, "freeze-bypass", "player", t.getName());
            return;
        }
        UUID id = t.getUniqueId();
        if (pl.frozen.remove(id)) {
            msg.send(t, "unfrozen");
            msg.send(s, "freeze-off-other", "player", t.getName());
        } else {
            pl.frozen.add(id);
            msg.send(t, "frozen");
            msg.send(s, "freeze-on-other", "player", t.getName());
        }
    }

    private void staffchat(CommandSender s, String[] a) {
        if (a.length == 0) {
            Player p = player(s);
            if (p == null) return;
            UUID id = p.getUniqueId();
            boolean on = !pl.staffChat.contains(id);
            if (on) pl.staffChat.add(id);
            else pl.staffChat.remove(id);
            msg.send(p, on ? "staffchat-on" : "staffchat-off");
            return;
        }
        pl.broadcastStaff(s.getName(), String.join(" ", a));
    }

    // ---------- kit ----------

    private void kit(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return;
        ConfigurationSection kits = pl.getConfig().getConfigurationSection("kits");
        if (kits == null) {
            msg.send(p, "kit-none");
            return;
        }
        if (a.length == 0) {
            List<String> ok = new ArrayList<>();
            for (String k : kits.getKeys(false)) {
                if (p.hasPermission("scarletcore.kit." + k)) ok.add(k);
            }
            if (ok.isEmpty()) {
                msg.send(p, "kit-none");
            } else {
                msg.send(p, "kit-list", "kits", String.join(", ", ok));
            }
            return;
        }
        String name = a[0].toLowerCase(Locale.ROOT);
        ConfigurationSection k = kits.getConfigurationSection(name);
        if (k == null) {
            msg.send(p, "kit-unknown");
            return;
        }
        if (!p.hasPermission("scarletcore.kit." + name)) {
            msg.send(p, "no-permission");
            return;
        }
        int cd = k.getInt("cooldown", 0);
        if (cd > 0) {
            long left = pl.cooldownLeft(p, "kit_" + name, cd);
            if (left > 0) {
                msg.send(p, "cooldown", "time", ScarletCore.formatTime(left));
                return;
            }
        }
        for (String line : k.getStringList("items")) {
            String[] parts = line.split(":");
            Material m = Material.matchMaterial(parts[0].trim());
            if (m == null) {
                pl.getLogger().warning("Material tidak dikenal di kit " + name + ": " + line);
                continue;
            }
            int amt = 1;
            if (parts.length > 1) {
                try {
                    amt = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) {
                    // pakai 1
                }
            }
            for (ItemStack left : p.getInventory().addItem(new ItemStack(m, amt)).values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), left);
            }
        }
        if (cd > 0) pl.stampCooldown(p, "kit_" + name);
        msg.send(p, "kit-given", "kit", name);
    }

    // ---------- admin ----------

    private void admin(CommandSender s, String[] a) {
        if (a.length > 0 && a[0].equalsIgnoreCase("reload")) {
            pl.reloadAll();
            msg.send(s, "reloaded");
        } else {
            usage(s, "/scarletcore reload");
        }
    }

    // ---------- tab complete ----------

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length != 1) return out;
        String prefix = a[0].toLowerCase(Locale.ROOT);
        switch (c.getName().toLowerCase(Locale.ROOT)) {
            case "fly", "heal", "feed", "god", "tp", "tphere", "freeze", "invsee", "endersee" -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (pl.vanished.contains(p.getUniqueId()) && !s.hasPermission("scarletcore.vanish.see")) continue;
                    out.add(p.getName());
                }
            }
            case "kit" -> {
                ConfigurationSection kits = pl.getConfig().getConfigurationSection("kits");
                if (kits != null) {
                    for (String k : kits.getKeys(false)) {
                        if (s.hasPermission("scarletcore.kit." + k)) out.add(k);
                    }
                }
            }
            case "speed", "nick" -> out.add("reset");
            case "scarletcore" -> out.add("reload");
            default -> {
            }
        }
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(prefix));
        return out;
    }
}
