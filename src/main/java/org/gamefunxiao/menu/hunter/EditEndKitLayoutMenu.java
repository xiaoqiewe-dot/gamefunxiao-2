package org.gamefunxiao.menu.hunter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.EndChapterKit;
import org.gamefunxiao.game.EndChapterKitPreview;
import org.gamefunxiao.game.EndChapterKitRole;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.game.RoomState;
import org.gamefunxiao.menu.base.BaseMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EditEndKitLayoutMenu extends BaseMenu {

    private static final int[] EDITOR_SLOTS = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    private static final int[] PLAYER_SLOT_ORDER = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            0, 1, 2, 3, 4, 5, 6, 7, 8
    };

    private final GameRoom room;
    private final EndChapterKitRole role;
    private final EndChapterKit kit;
    private BukkitTask guardTask;
    private EndChapterKitPreview preview;
    private boolean customLayoutSaved;

    public EditEndKitLayoutMenu(GameFunXiao plugin, Player player, GameRoom room, EndChapterKitRole role, EndChapterKit kit) {
        super(plugin, player, "§0§l✦ Kit 摆放编辑 ✦", 54);
        this.room = room;
        this.role = role;
        this.kit = kit;
    }

    @Override
    public void open() {
        super.open();
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_STEP, 0.75f, 1.65f);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.85f, 1.2f);
        startGuardTask();
    }

    @Override
    protected void setupItems() {
        inventory.clear();
        preview = plugin.getGameManager().createEndChapterKitPreview(role, kit);

        ItemStack[] savedLayout = plugin.getPlayerDataManager().getEndChapterKitLayout(player.getUniqueId(), role, kit);
        customLayoutSaved = hasAnyLayout(savedLayout);

        renderLayout(customLayoutSaved ? savedLayout : preview.getStorageContents());
        renderStaticItems();
    }

    @Override
    public boolean isEditableTopInventorySlot(int rawSlot) {
        for (int slot : EDITOR_SLOTS) {
            if (slot == rawSlot) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        if (isEditableTopInventorySlot(slot)) {
            return;
        }

        if (slot == 45) {
            playPageTurnSound();
            cancelGuardTask();
            openParentMenu();
            return;
        }

        if (slot == 49) {
            if (hasCursorItem()) {
                playErrorSound();
                player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.end_chapter_kit_layout_cursor_blocked"));
                return;
            }

            plugin.getPlayerDataManager().saveEndChapterKitLayout(player.getUniqueId(), role, kit, collectEditedLayout());
            customLayoutSaved = true;
            renderStaticItems();
            playSuccessSound();

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("role", role.getDisplayName());
            placeholders.put("kit", kit.getDisplayName());
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.end_chapter_kit_layout_saved", placeholders));
            return;
        }

        if (slot == 53) {
            if (hasCursorItem()) {
                playErrorSound();
                player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.end_chapter_kit_layout_cursor_blocked"));
                return;
            }

            plugin.getPlayerDataManager().clearEndChapterKitLayout(player.getUniqueId(), role, kit);
            customLayoutSaved = false;
            renderLayout(preview.getStorageContents());
            renderStaticItems();
            playDeleteSound();

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("role", role.getDisplayName());
            placeholders.put("kit", kit.getDisplayName());
            player.sendMessage(plugin.getMessageManager().getHunterGameMessageWithPrefix("game.end_chapter_kit_layout_reset", placeholders));
            return;
        }

    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        cancelGuardTask();
        if (player.getItemOnCursor() != null && player.getItemOnCursor().getType() != Material.AIR) {
            Bukkit.getScheduler().runTask(plugin, () -> player.setItemOnCursor(new ItemStack(Material.AIR)));
        }
    }

    private void renderStaticItems() {
        ItemStack[] armor = preview.getArmorPreview();
        inventory.setItem(0, createEquipmentPreviewItem(armor.length > 0 ? armor[0] : null, "头盔预览", "正式开始时会自动穿上"));
        inventory.setItem(1, createEquipmentPreviewItem(armor.length > 1 ? armor[1] : null, "胸甲预览", "正式开始时会自动穿上"));
        inventory.setItem(2, createEquipmentPreviewItem(armor.length > 2 ? armor[2] : null, "护腿预览", "正式开始时会自动穿上"));
        inventory.setItem(3, createEquipmentPreviewItem(armor.length > 3 ? armor[3] : null, "靴子预览", "正式开始时会自动穿上"));

        inventory.setItem(4, createTitleItem(kit.getIcon(),
                "§x§D§D§8§8§F§F✦ §x§F§F§A§A§F§F" + role.getDisplayName() + " §x§F§F§D§D§9§9" + kit.getDisplayName() + " §x§B§B§F§F§E§E摆放编辑",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a拖动中间四行，直接调整你的物品位置",
                "§f- §b这里只预览 Kit 本体 + 通用补给",
                "§f- §d落点追加物资会在正式开始时自动补到空位",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(5, createEquipmentPreviewItem(preview.getOffHandPreview(), "副手预览", "正式开始时会自动放到副手"));
        inventory.setItem(6, createInfoItem(Material.BOOK,
                "   §8[§x§8§8§D§D§F§F摆§x§A§A§E§E§F§F放§x§C§C§F§F§F§F状§x§E§E§D§D§A§A态§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §e当前 Kit: §d" + kit.getDisplayName(),
                "§f- §6当前布局: " + (customLayoutSaved ? "§a已保存自定义" : "§7使用默认摆放"),
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(7, createInfoItem(Material.RECOVERY_COMPASS,
                "   §8[§x§F§F§B§B§6§6编§x§F§F§C§C§7§7辑§x§F§F§D§D§8§8说§x§F§F§E§E§9§9明§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a中间 4 行就是你的主背包布局",
                "§f- §b最后一行对应热键栏 1~9",
                "§f- §d保存后，被分到这个 Kit 就会按你的习惯发放",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(8, createInfoItem(kit.getIcon(),
                "   §8[§x§F§F§D§D§7§7K§x§F§F§C§C§8§8i§x§F§F§B§B§9§9t §x§F§F§A§A§A§A预§x§F§F§9§9§B§B览§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §7左上角展示自动穿戴的装备",
                "§f- §7副手如果有盾牌也会直接显示出来",
                "§8· · · · · · · · · · · · · ·"));

        inventory.setItem(45, createBackButton());
        inventory.setItem(46, createBorderPane(Material.PURPLE_STAINED_GLASS_PANE, "§d拖动布局区域"));
        inventory.setItem(47, createBorderPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§b这里已经有返回按钮，不再单独放关闭"));
        inventory.setItem(48, createBorderPane(Material.PINK_STAINED_GLASS_PANE, "§d保存前记得确认手上没有拿着预览物品"));
        inventory.setItem(49, createItem(Material.EMERALD,
                "   §8[§x§5§5§F§F§A§A保§x§7§7§F§F§C§C存§x§9§9§F§F§E§E摆§x§B§B§E§E§F§F放§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a保存当前中间四行的物品位置",
                "§f- §b正式被分到这个 Kit 时就会按这里发放",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(50, createBorderPane(Material.BLUE_STAINED_GLASS_PANE, "§b主背包顺序会被完整记住"));
        inventory.setItem(51, createInfoItem(Material.CHEST,
                "   §8[§x§8§8§D§D§F§F额§x§A§A§E§E§F§F外§x§C§C§F§F§F§F提§x§E§E§D§D§A§A示§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a你可以把武器、方块、补给按自己的手感重新排",
                "§f- §b没有保存就不会影响正式发放",
                "§8· · · · · · · · · · · · · ·"));
        inventory.setItem(52, createBorderPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§b重置会恢复成系统默认布局"));
        inventory.setItem(53, createItem(Material.BARRIER,
                "   §8[§x§F§F§8§8§5§5恢§x§F§F§9§9§6§6复§x§F§F§A§A§7§7默§x§F§F§B§B§8§8认§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §e清空这个 Kit 的自定义摆放记录",
                "§f- §7下次正式发放会重新使用系统默认布局",
                "§8· · · · · · · · · · · · · ·"));
    }

    private void renderLayout(ItemStack[] storageContents) {
        for (int i = 0; i < EDITOR_SLOTS.length; i++) {
            int editorSlot = EDITOR_SLOTS[i];
            int playerSlot = PLAYER_SLOT_ORDER[i];
            ItemStack item = storageContents != null && playerSlot < storageContents.length ? storageContents[playerSlot] : null;
            inventory.setItem(editorSlot, item == null ? null : item.clone());
        }
    }

    private ItemStack[] collectEditedLayout() {
        ItemStack[] contents = new ItemStack[36];
        for (int i = 0; i < EDITOR_SLOTS.length; i++) {
            int editorSlot = EDITOR_SLOTS[i];
            int playerSlot = PLAYER_SLOT_ORDER[i];
            ItemStack item = inventory.getItem(editorSlot);
            if (item != null && item.getType() != Material.AIR) {
                contents[playerSlot] = item.clone();
            }
        }
        return contents;
    }

    private ItemStack createEquipmentPreviewItem(ItemStack source, String label, String detail) {
        Material material = source == null || source.getType() == Material.AIR ? Material.GRAY_STAINED_GLASS_PANE : source.getType();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("   §8[§x§B§B§8§8§F§F" + label + "§8]");
            List<String> lore = new ArrayList<>();
            lore.add("§8· · · · · · · · · · · · · ·");
            if (source == null || source.getType() == Material.AIR) {
                lore.add("§f- §7这个 Kit 没有对应装备");
            } else {
                lore.add("§f- §a当前展示物品: §e" + beautifyMaterial(source.getType()));
                lore.add("§f- §7" + detail);
                List<Map.Entry<org.bukkit.enchantments.Enchantment, Integer>> enchantments = new ArrayList<>(source.getEnchantments().entrySet());
                enchantments.sort(java.util.Comparator.comparing(entry -> entry.getKey().getKey().getKey()));
                for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : enchantments) {
                    lore.add("§f- §b" + beautifyKeyName(entry.getKey().getKey().getKey()) + " " + entry.getValue());
                }
            }
            lore.add("§8· · · · · · · · · · · · · ·");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoItem(Material material, String name, String... lore) {
        return createItem(material, name, lore);
    }

    private ItemStack createBorderPane(Material material, String detail) {
        return createItem(material,
                "   §8[§7布局提示§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §7" + detail,
                "§8· · · · · · · · · · · · · ·");
    }

    private String beautifyMaterial(Material material) {
        return beautifyKeyName(material.name());
    }

    private String beautifyKeyName(String raw) {
        String lower = raw.toLowerCase().replace(':', ' ').replace('_', ' ');
        String[] parts = lower.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return builder.toString().trim();
    }

    private boolean hasCursorItem() {
        ItemStack cursor = player.getItemOnCursor();
        return cursor != null && cursor.getType() != Material.AIR;
    }

    private boolean hasAnyLayout(ItemStack[] layout) {
        if (layout == null) {
            return false;
        }
        for (ItemStack item : layout) {
            if (item != null && item.getType() != Material.AIR) {
                return true;
            }
        }
        return false;
    }

    private void openParentMenu() {
        if (role == EndChapterKitRole.PREY) {
            new EndPreyKitMenu(plugin, player, room).open();
        } else {
            new VoteEndHunterKitMenu(plugin, player, room).open();
        }
    }

    private void startGuardTask() {
        cancelGuardTask();
        guardTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cancelGuardTask();
                return;
            }
            if (room.getGameMode() != GameMode.END_CHAPTER
                    || room.getState() != RoomState.SELECTING
                    || !room.isEndChapterDivisionActive()
                    || !matchesCurrentRole(player.getUniqueId())) {
                cancelGuardTask();
                if (player.getOpenInventory().getTopInventory().getHolder() == this) {
                    player.closeInventory();
                }
                return;
            }
            if (player.getOpenInventory().getTopInventory().getHolder() != this) {
                cancelGuardTask();
            }
        }, 10L, 10L);
    }

    private boolean matchesCurrentRole(UUID uuid) {
        return role == EndChapterKitRole.PREY ? room.isPrey(uuid) : room.isHunter(uuid);
    }

    private void cancelGuardTask() {
        if (guardTask != null) {
            guardTask.cancel();
            guardTask = null;
        }
    }
}
