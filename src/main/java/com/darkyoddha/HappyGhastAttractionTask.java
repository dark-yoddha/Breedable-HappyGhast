package com.darkyoddha;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class HappyGhastAttractionTask implements Runnable {
    private final BreedableHappyghast plugin;

    public HappyGhastAttractionTask(BreedableHappyghast plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Check if player is holding a snowball in main hand or off hand
            boolean holdingSnowball = (player.getInventory().getItemInMainHand() != null &&
                    player.getInventory().getItemInMainHand().getType() == Material.SNOWBALL) ||
                    (player.getInventory().getItemInOffHand() != null &&
                            player.getInventory().getItemInOffHand().getType() == Material.SNOWBALL);

            if (holdingSnowball) {
                // Get nearby entities within 20 blocks
                for (Entity entity : player.getNearbyEntities(20, 20, 20)) {
                    // Check if the entity is a Happy Ghast
                    if (entity instanceof HappyGhast) {
                        HappyGhast happyGhast = (HappyGhast) entity;

                        // Skip if ghast is in love mode (as per requirement)
                        if (plugin.getLoveModeHappyGhasts().containsKey(happyGhast.getUniqueId())) {
                            continue;
                        }

                        // Attraction logic: Move ghast towards player
                        double distance = player.getLocation().distance(happyGhast.getLocation());
                        if (distance > 5.0) {
                            Vector direction = player.getLocation().toVector()
                                    .subtract(happyGhast.getLocation().toVector());
                            if (direction.length() > 0) {
                                direction = direction.normalize().multiply(0.3); // Adjust speed

                                // Ground height restriction: stay at least 0.5 blocks above ground
                                Location ghastLoc = happyGhast.getLocation();
                                double groundY = -64; // Default for 1.18+ if no ground found
                                for (int y = ghastLoc.getBlockY(); y >= ghastLoc.getWorld().getMinHeight(); y--) {
                                    if (ghastLoc.getWorld().getBlockAt(ghastLoc.getBlockX(), y, ghastLoc.getBlockZ())
                                            .getType().isSolid()) {
                                        groundY = y + 1; // Top of the solid block
                                        break;
                                    }
                                }

                                double minHeight = groundY + 0.5;
                                if (ghastLoc.getY() + direction.getY() < minHeight) {
                                    // Adjust Y velocity to stay above minHeight
                                    double diff = minHeight - ghastLoc.getY();
                                    direction.setY(diff);
                                }

                                happyGhast.setVelocity(direction);
                            }
                        } else {
                            // Stop if too close to avoid collision
                            happyGhast.setVelocity(new Vector(0, 0, 0));
                        }
                    }
                }
            }
        }
    }
}
