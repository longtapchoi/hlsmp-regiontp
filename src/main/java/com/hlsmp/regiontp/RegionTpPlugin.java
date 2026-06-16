package com.hlsmp.regiontp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegionTpPlugin extends JavaPlugin {

    private final Map<String, WorldConfig> worldConfigs = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private static final long COOLDOWN_MS = 3000L;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadWorldConfigs();
        getServer().getPluginManager().registerEvents(new RegionTpListener(this), this);
        getLogger().info("HLSMP-RegionTp đã khởi động! Đã load " + worldConfigs.size() + " world.");
    }

    @Override
    public void onDisable() {
        getLogger().info("HLSMP-RegionTp đã tắt.");
    }

    public void loadWorldConfigs() {
        worldConfigs.clear();
        FileConfiguration config = getConfig();

        if (!config.contains("worlds") || config.getConfigurationSection("worlds") == null) return;

        for (String worldName : config.getConfigurationSection("worlds").getKeys(false)) {
            String path = "worlds." + worldName;
            boolean enabled = config.getBoolean(path + ".enabled", false);
            int yThreshold = config.getInt(path + ".y-threshold", 0);
            String destination = config.getString(path + ".destination", "self_spawn");
            worldConfigs.put(worldName, new WorldConfig(enabled, yThreshold, destination));
            getLogger().info("Load world: " + worldName + " (enabled=" + enabled + ", y<" + yThreshold + ", dest=" + destination + ")");
        }
    }

    public WorldConfig getWorldConfig(String worldName) {
        return worldConfigs.get(worldName);
    }

    // destination: "self_spawn" = spawn của world đó, hoặc tên world khác
    public Location getDestinationLocation(String currentWorldName, String destination) {
        String targetWorldName = destination.equals("self_spawn") ? currentWorldName : destination;
        World world = Bukkit.getWorld(targetWorldName);
        if (world == null) {
            getLogger().warning("Không tìm thấy world đích: " + targetWorldName);
            return null;
        }
        return world.getSpawnLocation();
    }

    public boolean isOnCooldown(UUID uuid) {
        if (!cooldowns.containsKey(uuid)) return false;
        long diff = System.currentTimeMillis() - cooldowns.get(uuid);
        if (diff >= COOLDOWN_MS) {
            cooldowns.remove(uuid);
            return false;
        }
        return true;
    }

    public void setCooldown(UUID uuid) {
        cooldowns.put(uuid, System.currentTimeMillis());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("hlsmpregiontp")) return false;

        if (!sender.hasPermission("hlsmpregiontp.admin")) {
            sender.sendMessage("§cBạn không có quyền dùng lệnh này.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                reloadConfig();
                loadWorldConfigs();
                sender.sendMessage("§a[HLSMP-RegionTp] §fReload xong! Đã load " + worldConfigs.size() + " world.");
            }
            case "list" -> {
                if (worldConfigs.isEmpty()) {
                    sender.sendMessage("§e[HLSMP-RegionTp] §fChưa có world nào được cấu hình.");
                } else {
                    sender.sendMessage("§a[HLSMP-RegionTp] §fDanh sách world:");
                    worldConfigs.forEach((name, cfg) ->
                        sender.sendMessage("§7 - §f" + name + " §7| enabled=§f" + cfg.isEnabled()
                            + " §7| y<§f" + cfg.getYThreshold()
                            + " §7| dest=§f" + cfg.getDestination()));
                }
            }
            case "addworld" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cSử dụng: /hlsmpregiontp addworld <world> <y-threshold> [destination]");
                    return true;
                }
                String worldName = args[1];
                int y;
                try {
                    y = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cY-threshold phải là số nguyên.");
                    return true;
                }
                String dest = args.length >= 4 ? args[3] : "self_spawn";
                String path = "worlds." + worldName;
                getConfig().set(path + ".enabled", true);
                getConfig().set(path + ".y-threshold", y);
                getConfig().set(path + ".destination", dest);
                saveConfig();
                loadWorldConfigs();
                sender.sendMessage("§a[HLSMP-RegionTp] §fĐã thêm world §e" + worldName + " §f(y<" + y + ", dest=" + dest + ")");
            }
            case "removeworld" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cSử dụng: /hlsmpregiontp removeworld <world>");
                    return true;
                }
                String worldName = args[1];
                getConfig().set("worlds." + worldName, null);
                saveConfig();
                loadWorldConfigs();
                sender.sendMessage("§a[HLSMP-RegionTp] §fĐã xoá world §e" + worldName);
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§e[HLSMP-RegionTp] §fLệnh:");
        sender.sendMessage("§7 /rtp reload §f- Reload config");
        sender.sendMessage("§7 /rtp list §f- Xem danh sách world");
        sender.sendMessage("§7 /rtp addworld <world> <y> [dest] §f- Thêm world");
        sender.sendMessage("§7 /rtp removeworld <world> §f- Xoá world");
    }
}
