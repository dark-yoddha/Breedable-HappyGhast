package com.darkyoddha;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class HappyGhastBreedingTask implements Runnable {

    private final BreedableHappyghast plugin;
    private final Set<UUID> processingBreeding = new HashSet<>();
    private final Map<UUID, Long> matchTimers = new HashMap<>();

    public HappyGhastBreedingTask(BreedableHappyghast plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        Map<UUID, UUID> loveModeHappyGhasts = plugin.getLoveModeHappyGhasts();
        if (loveModeHappyGhasts.isEmpty()) {
            return;
        }

        List<HappyGhast> activeHappyGhasts = new ArrayList<>();
        Iterator<Map.Entry<UUID, UUID>> iterator = loveModeHappyGhasts.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, UUID> entry = iterator.next();
            UUID happyghastUUID = entry.getKey();

            HappyGhast ghast = findHappyGhastByUUID(happyghastUUID);
            if (ghast == null || ghast.isDead()) {
                iterator.remove();
                continue;
            }

            activeHappyGhasts.add(ghast);

            if (plugin.getConfig().getBoolean("particles.show-hearts", true)) {
                int heartCount = plugin.getConfig().getInt("particles.heart-count", 5);
                ghast.getWorld().spawnParticle(
                        Particle.HEART,
                        ghast.getLocation().add(0, 3, 0),
                        heartCount,
                        0.5, 0.5, 0.5);
            }
        }

        Set<UUID> pairedInThisTick = new HashSet<>();

        for (int i = 0; i < activeHappyGhasts.size(); i++) {
            for (int j = i + 1; j < activeHappyGhasts.size(); j++) {
                HappyGhast happyghast1 = activeHappyGhasts.get(i);
                HappyGhast happyghast2 = activeHappyGhasts.get(j);

                if (processingBreeding.contains(happyghast1.getUniqueId())
                        || processingBreeding.contains(happyghast2.getUniqueId())
                        || pairedInThisTick.contains(happyghast1.getUniqueId())
                        || pairedInThisTick.contains(happyghast2.getUniqueId())) {
                    continue;
                }

                double distance = happyghast1.getLocation().distance(happyghast2.getLocation());

                if (distance <= 20.0) {
                    pairedInThisTick.add(happyghast1.getUniqueId());
                    pairedInThisTick.add(happyghast2.getUniqueId());

                    if (distance <= 8) {
                        if (!matchTimers.containsKey(happyghast1.getUniqueId())) {
                            matchTimers.put(happyghast1.getUniqueId(), System.currentTimeMillis());
                        }

                        if (System.currentTimeMillis() - matchTimers.get(happyghast1.getUniqueId()) >= 3000) {
                            breedGhasts(happyghast1, happyghast2);
                            matchTimers.remove(happyghast1.getUniqueId());
                        } else {
                            long elapsedTime = System.currentTimeMillis()
                                    - matchTimers.get(happyghast1.getUniqueId());
                            moveGhastsTowardsEachOther(happyghast1, happyghast2);
                        }
                    } else {
                        matchTimers.remove(happyghast1.getUniqueId());
                        moveGhastsTowardsEachOther(happyghast1, happyghast2);
                    }
                }
            }
        }
    }

    private void moveGhastsTowardsEachOther(HappyGhast happyghast1, HappyGhast happyghast2) {
        Vector direction1 = happyghast2.getLocation().toVector()
                .subtract(happyghast1.getLocation().toVector());
        if (direction1.length() > 0) {
            happyghast1.setVelocity(direction1.normalize().multiply(0.3));
        }

        Vector direction2 = happyghast1.getLocation().toVector()
                .subtract(happyghast2.getLocation().toVector());
        if (direction2.length() > 0) {
            happyghast2.setVelocity(direction2.normalize().multiply(0.3));
        }
    }

    private void breedGhasts(HappyGhast happyghast1, HappyGhast happyghast2) {

        UUID playerUUID = plugin.getLoveModeHappyGhasts().get(happyghast1.getUniqueId());
        if (playerUUID != null) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                Advancement advancement = Bukkit.getAdvancement(
                        NamespacedKey.minecraft("husbandry/breed_an_animal"));
                if (advancement != null) {
                    AdvancementProgress progress = player.getAdvancementProgress(advancement);
                    if (!progress.isDone()) {
                        for (String criteria : progress.getRemainingCriteria()) {
                            progress.awardCriteria(criteria);
                        }
                    }
                }
            }
        }

        processingBreeding.add(happyghast1.getUniqueId());
        processingBreeding.add(happyghast2.getUniqueId());

        Location breedLocation = happyghast1.getLocation().add(
                happyghast2.getLocation()
                        .subtract(happyghast1.getLocation())
                        .multiply(0.5));

        HappyGhast baby = breedLocation.getWorld().spawn(breedLocation, HappyGhast.class);
        baby.setAge(-400);

        // Spawn experience orbs (standard breeding gives 1-7 xp)
        int xp = new Random().nextInt(7) + 1;
        ExperienceOrb orb = (ExperienceOrb) breedLocation.getWorld().spawnEntity(breedLocation,
                EntityType.EXPERIENCE_ORB);
        orb.setExperience(xp);

        double driedGhastChance = plugin.getConfig()
                .getDouble("breeding.dried-ghast-spawn-chance", 0.3);

        if (Math.random() < driedGhastChance) {
            Location groundLocation = breedLocation.clone();
            World world = groundLocation.getWorld();

            while (groundLocation.getY() > world.getMinHeight()
                    && groundLocation.getBlock().getType() == Material.AIR) {
                groundLocation.subtract(0, 1, 0);
            }

            groundLocation.add(0, 1, 0);
            world.dropItem(
                    groundLocation,
                    new org.bukkit.inventory.ItemStack(Material.GHAST_TEAR));
        }

        plugin.getLoveModeHappyGhasts().remove(happyghast1.getUniqueId());
        plugin.getLoveModeHappyGhasts().remove(happyghast2.getUniqueId());

        plugin.setBreedingCooldown(happyghast1.getUniqueId());
        plugin.setBreedingCooldown(happyghast2.getUniqueId());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            processingBreeding.remove(happyghast1.getUniqueId());
            processingBreeding.remove(happyghast2.getUniqueId());
        }, 20L);
    }

    private HappyGhast findHappyGhastByUUID(UUID uuid) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getType() == EntityType.HAPPY_GHAST
                        && entity.getUniqueId().equals(uuid)) {
                    return (HappyGhast) entity;
                }
            }
        }
        return null;
    }
}