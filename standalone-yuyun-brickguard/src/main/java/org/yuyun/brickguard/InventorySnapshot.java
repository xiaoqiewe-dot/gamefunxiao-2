package org.yuyun.brickguard;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;

final class InventorySnapshot {
    final Location location;
    final GameMode gameMode;
    final boolean allowFlight;
    final boolean flying;
    final ItemStack[] contents;
    final ItemStack[] armor;
    final ItemStack offhand;
    final int heldSlot;

    InventorySnapshot(Player player) {
        location = player.getLocation().clone();
        gameMode = player.getGameMode();
        allowFlight = player.getAllowFlight();
        flying = player.isFlying();
        PlayerInventory inv = player.getInventory();
        contents = cloneItems(inv.getContents());
        armor = cloneItems(inv.getArmorContents());
        offhand = inv.getItemInOffHand() == null ? null : inv.getItemInOffHand().clone();
        heldSlot = inv.getHeldItemSlot();
    }

    void restore(Player player) {
        restoreInventoryOnly(player);
        player.setGameMode(gameMode);
        player.setAllowFlight(allowFlight);
        player.setFlying(flying);
        if (location != null && location.getWorld() != null) {
            player.teleport(location);
        }
    }

    void restoreInventoryOnly(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setContents(cloneItems(contents));
        inv.setArmorContents(cloneItems(armor));
        inv.setItemInOffHand(offhand == null ? null : offhand.clone());
        inv.setHeldItemSlot(Math.max(0, Math.min(8, heldSlot)));
    }

    static ItemStack[] cloneItems(ItemStack[] source) {
        return Arrays.stream(source).map(item -> item == null ? null : item.clone()).toArray(ItemStack[]::new);
    }
}
