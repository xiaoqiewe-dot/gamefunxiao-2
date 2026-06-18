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

public class LuckyPillarsVictoryEffectSettingsMenu extends BaseMenu {

    private static final int[] EFFECT_SLOTS = {19, 20, 21, 22, 23, 24, 25, 31};

    public LuckyPillarsVictoryEffectSettingsMenu(GameFunXiao plugin, Player player) {
        super(plugin, player, "§0§l🍀 幸运之柱特效设置 🍀", 54);
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        fillMiFanBorder();

        String selected = plugin.getPlayerDataManager().getSelectedLuckyPillarsVictoryEffect(player.getUniqueId());
        inventory.setItem(4, createTitleItem(Material.COMPARATOR,
                "§x§8§8§D§D§F§F⚙ §x§9§9§E§E§F§F幸§x§A§A§F§F§F§F运§x§B§B§F§F§E§E之§x§C§C§F§F§D§D柱§x§D§D§F§F§C§C胜§x§E§E§F§F§B§B利§x§D§D§F§F§C§C特§x§B§B§F§F§D§D效§x§9§9§F§F§F§F设§x§7§7§E§E§F§F置",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b这里只能切换你已经拥有的特效",
                "§f- §a结算时会固定在场地中心播放",
                "§f- §d当前选择: §e" + LuckyPillarsVictoryEffect.byId(selected).getDisplayName(plugin),
                "§8· · · · · · · · · · · · · ·"));

        LuckyPillarsVictoryEffect[] effects = LuckyPillarsVictoryEffect.values();
        for (int i = 0; i < effects.length && i < EFFECT_SLOTS.length; i++) {
            inventory.setItem(EFFECT_SLOTS[i], createSettingItem(effects[i], selected));
        }

        inventory.setItem(45, createBackButton());
    }

    private ItemStack createSettingItem(LuckyPillarsVictoryEffect effect, String selected) {
        ItemStack item = new ItemStack(effect.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean owned = plugin.getPlayerDataManager().hasLuckyPillarsVictoryEffect(player.getUniqueId(), effect.getId());
            boolean isSelected = effect.getId().equalsIgnoreCase(selected);
            String state = isSelected ? "§a✔ 正在使用" : owned ? "§b可切换" : "§8未拥有";
            meta.setDisplayName("   §8[§r" + (isSelected ? "§a✔ " : owned ? "§b🍀 " : "§8🍀 ")
                    + effect.getDisplayName(plugin) + " §8| " + state + "§8]");

            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.addAll(effect.getDescription(plugin));
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §b状态: " + state);
            lore.add("§f- §7播放位置: 场地中心");
            if (isSelected) {
                lore.add("§f- §a这个特效已经在使用中");
            } else if (owned) {
                lore.add("§f- §b左键切换为这个特效");
            } else {
                lore.add("§f- §7你还没有这个特效");
                lore.add("§f- §e请前往商城购买");
            }
            lore.add("§8· · · · · · · · · · · · · ·");
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
                handleSettingClick(LuckyPillarsVictoryEffect.values()[i]);
                return;
            }
        }

        if (slot == 45) {
            playClickSound();
            plugin.getMenuManager().openLuckyPillarsEffectCategoryMenu(player);
        }
    }

    private void handleSettingClick(LuckyPillarsVictoryEffect effect) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("effect", effect.getDisplayName(plugin));

        String selected = plugin.getPlayerDataManager().getSelectedLuckyPillarsVictoryEffect(player.getUniqueId());
        if (effect.getId().equalsIgnoreCase(selected)) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("settings.effect_already_selected", placeholders));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.6f);
            return;
        }

        if (!plugin.getPlayerDataManager().hasLuckyPillarsVictoryEffect(player.getUniqueId(), effect.getId())) {
            player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("settings.effect_not_owned", placeholders));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.9f, 1.0f);
            return;
        }

        plugin.getPlayerDataManager().setSelectedLuckyPillarsVictoryEffect(player.getUniqueId(), effect.getId());
        player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("settings.effect_select_success", placeholders));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.7f);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.9f);
        setupItems();
    }
}
