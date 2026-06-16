package com.hlsmp.regiontp;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegionTpPlugin extends JavaPlugin {

    private MultiverseCore multiverseCore;
    private final Map<String, WorldConfig> worldConfigs = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private static final long COOLDOWN_MS = 3000L;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new RegionTpListener(this), this);

        // Delay 1 tick để đảm bảo Multiverse-Core load xong hoàn toàn
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (Bukkit.getPluginManager().getPlugin("Multiverse-Core") instanceof MultiverseCore mv) {
                multiverseCore = mv;
                getLogger().info("Đã kết nối với Multiverse-Core.");
            } else {
                getLogger().warning("Không tìm thấy Multiverse-Core! Plugin sẽ không hoạt động.");
                return;
            }
            loadWorldConfigs();
            getLogger().info("HLSMP-RegionTp đã khởi động thành công!");
        }, 1L);
    }

    @Override
    public void onDisable() {
        getLogger().info("HLSMP-RegionTp đã tắt.");
    }

    public void loadWorldConfigs() {
        worldConfigs.clear();
        FileConfiguration config = getConfig();

        if (multiverseCore == null) return;

        MVWorldManager worldManager = multiverseCore.getMVWorldManager();
        Collection<MultiverseWorld> mvWorlds = worldManager.getMVWorlds();

        if (mvWorlds.isEmpty()) {
            getLogger().warning("Multiverse-Core không có world nào! Kiểm tra lại MV đã load world chưa.");
            return;
        }

        for (MultiverseWorld mvWorld : mvWorlds) {
            String worldName = mvWorld.getName();
            String path = "worlds." + worldName;

            // Nếu chưa có entry trong config, tạo mặc định
            if (!config.contains(path)) {
                config.set(path + ".enabled", false);
                config.set(path + ".y-threshold", 0);
                config.set(path + ".destination", "mv_spawn");
                getLogger().info("Tự động thêm world MV vào config: " + worldName);
            }

            boolean enabled = config.getBoolean(path + ".enabled", false);
            int yThreshold = config.getInt(path + ".y-threshold", 0);
            String destination = config.getString(path + ".destination", "mv_spawn");

            worldConfigs.put(worldName, new WorldConfig(enabled, yThreshold, destination));
        }

        saveConfig();
        getLogger().info("Đã load " + worldConfigs.size() + " world config.");
    }

    public WorldConfig getWorldConfig(String worldName) {
        return worldConfigs.get(worldName);
    }

    public Location getDestinationLocation(String worldName, String destination) {
        if (multiverseCore == null) return null;

        String targetWorldName = destination.equals("mv_spawn") ? worldName : destination;
        MVWorldManager worldManager = multiverseCore.getMVWorldManager();

        if (!worldManager.isMVWorld(targetWorldName)) {
            getLogger().warning("World đích không tồn tại: " + targetWorldName);
            return null;
        }

        MultiverseWorld mvWorld = worldManager.getMVWorld(targetWorldName);
        return mvWorld.getSpawnLocation();
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

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            loadWorldConfigs();
            sender.sendMessage("§a[HLSMP-RegionTp] §fĐã reload config thành công! Đã load " + worldConfigs.size() + " world.");
            return true;
        }

        sender.sendMessage("§e[HLSMP-RegionTp] §fSử dụng: /hlsmpregiontp reload");
        return true;
    }
}
