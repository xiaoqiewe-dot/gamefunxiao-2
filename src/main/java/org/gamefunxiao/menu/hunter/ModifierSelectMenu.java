package org.gamefunxiao.menu.hunter;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.*;

public class ModifierSelectMenu extends BaseMenu {

    private final GameMode gameMode;
    private final Set<String> selectedModifiers;
    private final CreateRoomMenu parentMenu;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 21;

    // 所有修饰符
    private static final List<ModifierInfo> ALL_MODIFIERS = Arrays.asList(
        new ModifierInfo("ThunderStorm", "雷魔修饰符", Material.LIGHTNING_ROD,
            "全世界处于雷暴天", "被雨淋会减速", "攻击他人会触发雷击"),
        new ModifierInfo("RewardChest", "奖励箱修饰符", Material.CHEST,
            "开局获得指向奖励箱的指南针", "奖励箱在出生点50-100格处", "内含珍贵物品"),
        new ModifierInfo("NoWorld", "无世界修饰符", Material.BEDROCK,
            "猎物无法选择世界", "直接使用随机生成的世界"),
        new ModifierInfo("NoYChange", "禁止Y轴移动修饰符", Material.ELYTRA,
            "猎物在选择世界时", "不能上下移动", "只能固定位置四处查看"),
        new ModifierInfo("YesHunterSee", "猎人视野修饰符", Material.ENDER_EYE,
            "猎人开局不会获得致盲效果", "可以看到猎物的初始位置"),
        new ModifierInfo("NoLocatorBar", "关闭定位条修饰符", Material.SPYGLASS,
            "关闭原版定位条", "猎人和猎物都不会显示定位条", "更依赖指南针与观察"),
        new ModifierInfo("IncludeY", "Y轴追踪修饰符", Material.COMPASS,
            "启用后指南针与排行", "都不会计算Y轴距离"),
        new ModifierInfo("HunterDropItems", "猎人掉落修饰符", Material.CHEST_MINECART,
            "猎人死亡时会掉落装备", "而不是保留背包"),
        new ModifierInfo("PreyRespawn", "猎物复活修饰符", Material.TOTEM_OF_UNDYING,
            "猎物死亡后可复活一次", "保留装备", "之后再次死亡游戏结束"),
        new ModifierInfo("HunterTPOnDeath", "猎人死亡TP修饰符", Material.ENDER_PEARL,
            "猎人死亡复活后", "必须选择一位队友传送过去", "否则无法继续复活"),
        new ModifierInfo("InfiniteTP", "削减TP时间修饰符", Material.CHORUS_FRUIT,
            "指南针TP冷却时间", "减少为1分钟")
    );

    public ModifierSelectMenu(GameFunXiao plugin, Player player, GameMode gameMode,
                              Set<String> selectedModifiers, CreateRoomMenu parentMenu) {
        super(plugin, player, "§0§l⚡ 选择修饰符 ⚡", 54);
        this.gameMode = gameMode;
        this.selectedModifiers = selectedModifiers;
        this.parentMenu = parentMenu;
    }

    @Override
    protected void setupItems() {
        inventory.clear();

        // 米饭风格边框
        fillMiFanBorder();

        // 标题物品
        inventory.setItem(4, createTitleItem(Material.BLAZE_POWDER,
            "§x§F§F§A§A§5§5⚡ §x§F§F§C§C§7§7修§x§F§F§E§E§9§9饰§x§D§D§F§F§9§9符 §x§F§F§A§A§5§5⚡",
            "§8· · · · · · · · · · · · · ·",
            "§f已选择: §e" + selectedModifiers.size() + " §f个",
            "§f点击切换选中状态",
            "§8· · · · · · · · · · · · · ·"));

        // 显示修饰符
        int startIndex = page * ITEMS_PER_PAGE;
        int[] slots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
        };

        for (int i = 0; i < slots.length && startIndex + i < ALL_MODIFIERS.size(); i++) {
            ModifierInfo modifier = ALL_MODIFIERS.get(startIndex + i);
            inventory.setItem(slots[i], createModifierItem(modifier));
        }

        // 上一页
        if (page > 0) {
            inventory.setItem(48, createPreviousPageButton());
        }

        // 下一页
        if ((page + 1) * ITEMS_PER_PAGE < ALL_MODIFIERS.size()) {
            inventory.setItem(50, createNextPageButton());
        }

        // 确认按钮
        inventory.setItem(49, createItem(Material.EMERALD,
            "   §8[§x§5§5§F§F§5§5✓ §x§7§7§F§F§7§7确§x§9§9§F§F§9§9认§8]",
            "§8· · · · · · · · · · · · · ·",
            "§f- §a点击确认选择",
            "§f- §e已选: §b" + selectedModifiers.size() + " §e个",
            "§8· · · · · · · · · · · · · ·"));

        // 返回按钮
        inventory.setItem(45, createBackButton());

    }

    private ItemStack createModifierItem(ModifierInfo modifier) {
        boolean selected = selectedModifiers.contains(modifier.id);

        ItemStack item = new ItemStack(modifier.material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String prefix = selected ? "§a✓ " : "§7  ";
            meta.setDisplayName(prefix + "§8[§e" + modifier.displayName + "§8]");

            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            lore.add("§f- §7ID: §e" + modifier.id);
            for (String desc : modifier.description) {
                lore.add("§f- §b" + desc);
            }
            lore.add("§8· · · · · · · · · · · · · ·");
            if (selected) {
                lore.add("§a✓ 已选中 §7- 点击取消");
            } else {
                lore.add("§7未选中 §7- 点击选择");
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

        int[] slots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
        };

        // 检查是否点击了修饰符
        int slotIndex = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                slotIndex = i;
                break;
            }
        }

        if (slotIndex >= 0) {
            int modifierIndex = page * ITEMS_PER_PAGE + slotIndex;
            if (modifierIndex < ALL_MODIFIERS.size()) {
                ModifierInfo modifier = ALL_MODIFIERS.get(modifierIndex);

                if (selectedModifiers.contains(modifier.id)) {
                    selectedModifiers.remove(modifier.id);
                    playCancelSound();
                } else {
                    selectedModifiers.add(modifier.id);
                    playSelectSound();
                }

                setupItems();
            }
            return;
        }

        switch (slot) {
            case 48 -> {
                // 上一页
                if (page > 0) {
                    playPageTurnSound();
                    page--;
                    setupItems();
                }
            }
            case 49 -> {
                // 确认
                playConfirmSound();
                parentMenu.open();
            }
            case 50 -> {
                // 下一页
                if ((page + 1) * ITEMS_PER_PAGE < ALL_MODIFIERS.size()) {
                    playPageTurnSound();
                    page++;
                    setupItems();
                }
            }
            case 45 -> {
                // 返回
                playClickSound();
                parentMenu.open();
            }
        }
    }

    private static class ModifierInfo {
        final String id;
        final String displayName;
        final Material material;
        final String[] description;

        ModifierInfo(String id, String displayName, Material material, String... description) {
            this.id = id;
            this.displayName = displayName;
            this.material = material;
            this.description = description;
        }
    }
}
