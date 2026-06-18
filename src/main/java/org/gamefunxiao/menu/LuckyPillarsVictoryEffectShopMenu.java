package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.cosmetics.LuckyPillarsVictoryEffect;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LuckyPillarsVictoryEffectShopMenu extends BaseMenu {

    private static final int[] EFFECT_SLOTS = {19, 20, 21, 22, 23, 24, 25, 31};

    public LuckyPillarsVictoryEffectShopMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l🍀 幸运之柱胜利特效 🍀", 54);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        int coins = plugin.getPlayerDataManager().getCoins(player.getUniqueId());
        String currency = plugin.getConfigManager().getMiniGameCurrencyName();
        String selected = plugin.getPlayerDataManager().getSelectedLuckyPillarsVictoryEffect(player.getUniqueId());

        inventory.setItem(4, createTitleItem(Material.FIREWORK_ROCKET,
                "§x§F§F§D§D§5§5🍀 §x§F§F§E§E§7§7幸§x§F§F§F§F§9§9运§x§D§D§F§F§B§B之§x§B§B§F§F§D§D柱§x§9§9§F§F§F§F胜§x§7§7§E§E§F§F利§x§5§5§D§D§F§F特§x§7§7§C§C§F§F效",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a余额: §e" + coins + " §6" + currency,
                "§f- §b当前选择: §d" + LuckyPillarsVictoryEffect.byId(selected).getDisplayName(plugin),
                "§f- §7播放位置: §f场地正中间",
                "§8· · · · · · · · · · · · · ·"));

        LuckyPillarsVictoryEffect[] effects = LuckyPillarsVictoryEffect.values();
        for (int i = 0; i < effects.length && i < EFFECT_SLOTS.length; i++) {
            inventory.setItem(EFFECT_SLOTS[i], createEffectItem(effects[i], coins, currency, selected));
        }

        inventory.setItem(45, createBackButton());
    }

    private ItemStack createEffectItem(LuckyPillarsVictoryEffect effect, int coins, String currency, String selected) {
        ItemStack item = new ItemStack(effect.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean owned = plugin.getPlayerDataManager().hasLuckyPillarsVictoryEffect(player.getUniqueId(), effect.getId());
            boolean isSelected = effect.getId().equalsIgnoreCase(selected);
            int price = effect.getPrice(plugin);

            String state = isSelected ? "§a✔ 已选择" : owned ? "§b已拥有" : coins >= price ? "§e可购买" : "§c余额不足";
            meta.setDisplayName("   §8[§r" + (isSelected ? "§a✔ " : "§d🍀 ") + effect.getDisplayName(plugin) + " §8| " + state + "§8]");

            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.addAll(effect.getDescription(plugin));
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §6价格: §e" + price + " §6" + currency);
            lore.add("§f- §a你的余额: §e" + coins + " §6" + currency);
            lore.add("§f- §b状态: " + state);
            lore.add("§f- §7结算时会在场地中心播放");
            lore.add("§8· · · · · · · · · · · · · ·");
            if (isSelected) {
                lore.add("§f- §a这个特效正在使用中");
            } else if (owned) {
                lore.add("§f- §b左键选择这个胜利特效");
            } else {
                lore.add("§f- §e左键购买并自动选择");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        for (int i = 0; i < EFFECT_SLOTS.length && i < LuckyPillarsVictoryEffect.values().length; i++) {
            if (slot == EFFECT_SLOTS[i]) {
                handleEffectClick(LuckyPillarsVictoryEffect.values()[i]);
                return;
            }
        }

        if (slot == 45) {
            playClickSound();
            plugin.getMenuManager().openLuckyPillarsShopCategoryMenu(player);
        }
    }

    private void handleEffectClick(LuckyPillarsVictoryEffect effect) {
        String effectId = effect.getId();
        String selected = plugin.getPlayerDataManager().getSelectedLuckyPillarsVictoryEffect(player.getUniqueId());
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("effect", effect.getDisplayName(plugin));
        placeholders.put("currency", plugin.getConfigManager().getMiniGameCurrencyName());
        placeholders.put("price", String.valueOf(effect.getPrice(plugin)));
        placeholders.put("balance", String.valueOf(plugin.getPlayerDataManager().getCoins(player.getUniqueId())));

        if (effectId.equalsIgnoreCase(selected)) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("shop.already_selected", placeholders));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.6f);
            return;
        }

        if (plugin.getPlayerDataManager().hasLuckyPillarsVictoryEffect(player.getUniqueId(), effectId)) {
            plugin.getPlayerDataManager().setSelectedLuckyPillarsVictoryEffect(player.getUniqueId(), effectId);
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("shop.select_success", placeholders));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.65f);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.8f);
            setupItems();
            return;
        }

        int price = effect.getPrice(plugin);
        if (!plugin.getPlayerDataManager().takeCoins(player.getUniqueId(), price)) {
            placeholders.put("balance", String.valueOf(plugin.getPlayerDataManager().getCoins(player.getUniqueId())));
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("shop.not_enough_currency", placeholders));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.9f, 1.0f);
            setupItems();
            return;
        }

        plugin.getPlayerDataManager().unlockLuckyPillarsVictoryEffect(player.getUniqueId(), effectId);
        plugin.getPlayerDataManager().setSelectedLuckyPillarsVictoryEffect(player.getUniqueId(), effectId);
        placeholders.put("balance", String.valueOf(plugin.getPlayerDataManager().getCoins(player.getUniqueId())));
        player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("shop.purchase_success", placeholders));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.82f, 1.18f);
        setupItems();
    }
}
