package com.darkyoddha;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.HappyGhast;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class HappyGhastInteractionListener implements Listener {

    private final BreedableHappyghast plugin;

    public HappyGhastInteractionListener(BreedableHappyghast plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        // Check if the clicked entity is a Happy Ghast
        if (!(event.getRightClicked() instanceof HappyGhast)) {
            return;
        }

        // Check if player is holding a snowball
        ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
        if (item == null || item.getType() != Material.SNOWBALL) {
            return;
        }

        HappyGhast happyGhast = (HappyGhast) event.getRightClicked();
        event.setCancelled(true); // Prevent default snowball throw behavior

        double maxHealth = happyGhast.getAttribute(Attribute.MAX_HEALTH).getValue();
        double currentHealth = happyGhast.getHealth();

        if (currentHealth < maxHealth) {
            double healAmount = plugin.getConfig().getDouble("breeding.heal-amount", 4.0);
            happyGhast.setHealth(Math.min(currentHealth + healAmount, maxHealth));

            if (!event.getPlayer().getGameMode().toString().equals("CREATIVE")) {
                item.setAmount(item.getAmount() - 1);
            }
        } else if (happyGhast.getAge() < 0) {
            // Consume the snowball (fixes the "getting snowballs back" bug)
            if (!event.getPlayer().getGameMode().toString().equals("CREATIVE")) {
                item.setAmount(item.getAmount() - 1);
            }
        } else {
            // Check if Happy Ghast is breedable (age 0 = adult)
            if (happyGhast.getAge() != 0) {
                return;
            }

            if (plugin.isOnCooldown(happyGhast.getUniqueId())) {
                return;
            }

            plugin.getLoveModeHappyGhasts().put(happyGhast.getUniqueId(), happyGhast.getUniqueId());

            if (plugin.getConfig().getBoolean("particles.show-hearts", true)) {
                // Spawning hearts immediately on interaction as feedback
                happyGhast.getWorld().spawnParticle(org.bukkit.Particle.HEART, happyGhast.getLocation().add(0, 3, 0), 3,
                        0.5, 0.5, 0.5);
            }

            if (!event.getPlayer().getGameMode().toString().equals("CREATIVE")) {
                item.setAmount(item.getAmount() - 1);
            }
        }
    }
}
