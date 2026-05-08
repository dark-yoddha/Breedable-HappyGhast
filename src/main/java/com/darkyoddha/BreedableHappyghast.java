package com.darkyoddha;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BreedableHappyghast extends JavaPlugin {

    private static BreedableHappyghast instance;
    private Map<UUID, Long> breedingCooldowns;
    private Map<UUID, UUID> loveModeHappyGhasts;

    @Override
    public void onEnable() {
        instance = this;
        breedingCooldowns = new HashMap<>();
        loveModeHappyGhasts = new HashMap<>();

        saveDefaultConfig();

        Bukkit.getPluginManager().registerEvents(new HappyGhastInteractionListener(this), this);
        Bukkit.getScheduler().runTaskTimer(this, new HappyGhastBreedingTask(this), 0L, 10L);
        Bukkit.getScheduler().runTaskTimer(this, new HappyGhastAttractionTask(this), 0L, 10L);

        getLogger().info("HappyGhastBreeder plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("HappyGhastBreeder plugin has been disabled!");
    }

    public static BreedableHappyghast getInstance() {
        return instance;
    }

    public Map<UUID, Long> getBreedingCooldowns() {
        return breedingCooldowns;
    }

    public Map<UUID, UUID> getLoveModeHappyGhasts() {
        return loveModeHappyGhasts;
    }

    public boolean isOnCooldown(UUID ghastUUID) {
        if (!breedingCooldowns.containsKey(ghastUUID)) {
            return false;
        }
        long cooldownEnd = breedingCooldowns.get(ghastUUID);
        long currentTime = System.currentTimeMillis();
        if (currentTime >= cooldownEnd) {
            breedingCooldowns.remove(ghastUUID);
            return false;
        }
        return true;
    }

    public void setBreedingCooldown(UUID ghastUUID) {
        long cooldownSeconds = getConfig().getLong("breeding.cooldown", 300);
        long cooldownEnd = System.currentTimeMillis() + (cooldownSeconds * 1000);
        breedingCooldowns.put(ghastUUID, cooldownEnd);
    }
}
