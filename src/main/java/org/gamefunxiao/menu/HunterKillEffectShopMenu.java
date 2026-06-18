package org.gamefunxiao.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.cosmetics.HunterKillEffect;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HunterKillEffectShopMenu extends BaseMenu {

    private static final int[] EFFECT_SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32};

    public HunterKillEffectShopMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l⚔ 猎人击杀特效 ⚔", 54);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        int coins = plugin.getPlayerDataManager().getCoins(player.getUniqueId());
        String currency = plugin.getConfigManager().getMiniGameCurrencyName();
        String selected = plugin.getPlayerDataManager().getSelectedHunterKillEffect(player.getUniqueId());

        inventory.setItem(4, createTitleItem(Material.NETHERITE_SWORD,
                "§x§F§F§6§6§0§0⚔ §x§F§F§9§9§3§3猎§x§F§F§C§C§6§6人§x§F§F§F§F§9§9击§x§C§C§F§F§9§9杀§x§9§9§F§F§C§C特§x§6§6§F§F§F§F效",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a余额: §e" + coins + " §6" + currency,
                "§f- §b当前选择: §d" + HunterKillEffect.byId(selected).getDisplayName(plugin),
                "§8· · · · · · · · · · · · · ·"));

        HunterKillEffect[] effects = HunterKillEffect.values();
        for (int i = 0; i < effects.length && i < EFFECT_SLOTS.length; i++) {
            inventory.setItem(EFFECT_SLOTS[i], createEffectItem(effects[i], coins, currency, selected));
        }

        inventory.setItem(45, createBackButton());
    }

    private ItemStack createEffectItem(HunterKillEffect effect, int coins, String currency, String selected) {
        ItemStack item = new ItemStack(effect.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean owned = plugin.getPlayerDataManager().hasKillEffect(player.getUniqueId(), effect.getId());
            boolean isSelected = effect.getId().equalsIgnoreCase(selected);
            int price = effect.getPrice(plugin);

            String state = isSelected ? "§a✔ 已选择" : owned ? "§b已拥有" : coins >= price ? "§e可购买" : "§c余额不足";
            meta.setDisplayName("   §8[§r" + (isSelected ? "§a✔ " : "§d✦ ") + effect.getDisplayName(plugin) + " §8| " + state + "§8]");

            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.addAll(effect.getDescription(plugin));
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §6价格: §e" + price + " §6" + currency);
            lore.add("§f- §a你的余额: §e" + coins + " §6" + currency);
            lore.add("§f- §b状态: " + state);
            lore.add("§8· · · · · · · · · · · · · ·");
            if (isSelected) {
                lore.add("§f- §a这个击杀特效正在使用中");
            } else if (owned) {
                lore.add("§f- §b左键选择这个击杀特效");
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

        for (int i = 0; i < EFFECT_SLOTS.length && i < HunterKillEffect.values().length; i++) {
            if (slot == EFFECT_SLOTS[i]) {
                handleEffectClick(HunterKillEffect.values()[i]);
                return;
            }
        }

        if (slot == 45) {
            playClickSound();
            plugin.getMenuManager().openHunterGameShopCategoryMenu(player);
        }
    }

    private void handleEffectClick(HunterKillEffect effect) {
        String effectId = effect.getId();
        String selected = plugin.getPlayerDataManager().getSelectedHunterKillEffect(player.getUniqueId());
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

        if (plugin.getPlayerDataManager().hasKillEffect(player.getUniqueId(), effectId)) {
            plugin.getPlayerDataManager().setSelectedHunterKillEffect(player.getUniqueId(), effectId);
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

        plugin.getPlayerDataManager().unlockKillEffect(player.getUniqueId(), effectId);
        plugin.getPlayerDataManager().setSelectedHunterKillEffect(player.getUniqueId(), effectId);
        placeholders.put("balance", String.valueOf(plugin.getPlayerDataManager().getCoins(player.getUniqueId())));
        player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("shop.purchase_success", placeholders));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.82f, 1.18f);
        setupItems();
    }
}
