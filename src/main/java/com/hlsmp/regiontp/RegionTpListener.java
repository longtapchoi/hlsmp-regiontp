package com.hlsmp.regiontp;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class RegionTpListener implements Listener {

    private final RegionTpPlugin plugin;

    public RegionTpListener(RegionTpPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Chỉ check khi Y thay đổi (tối ưu performance)
        if (event.getFrom().getBlockY() == event.getTo().getBlockY()) return;

        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        WorldConfig worldConfig = plugin.getWorldConfig(worldName);
        if (worldConfig == null || !worldConfig.isEnabled()) return;

        double currentY = event.getTo().getY();
        if (currentY >= worldConfig.getYThreshold()) return;

        // Kiểm tra cooldown
        if (plugin.isOnCooldown(player.getUniqueId())) return;

        // Lấy điểm đích
        Location destination = plugin.getDestinationLocation(worldName, worldConfig.getDestination());
        if (destination == null) {
            plugin.getLogger().warning("Không tìm thấy điểm đích cho world: " + worldName);
            return;
        }

        plugin.setCooldown(player.getUniqueId());
        player.teleport(destination);
        player.sendMessage("§c[!] §fBạn đã rơi xuống vùng nguy hiểm và được đưa về điểm an toàn.");
    }
}
