package org.gamefunxiao.menu.hunter;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.EndFlashKitManager;
import org.gamefunxiao.menu.base.BaseMenu;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EndFlashKitRoleMenu extends BaseMenu {

    private static final int ITEMS_PER_PAGE = 45;
    private static final int[] KIT_SLOTS = buildKitSlots();
    private static final DateTimeFormatter NAME_TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");

    private final EndFlashKitManager.Role role;
    private int page;

    public EndFlashKitRoleMenu(GameFunXiao plugin, Player player, EndFlashKitManager.Role role, int page) {
        super(plugin, player, "§0§l" + role.displayName(), 54);
        this.role = role;
        this.page = Math.max(0, page);
    }

    private static int[] buildKitSlots() {
        int[] slots = new int[45];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    protected void setupItems() {
        inventory.clear();

        List<EndFlashKitManager.Kit> kits = plugin.getEndFlashKitManager().getKits(role);
        int maxPage = Math.max(0, (kits.size() - 1) / ITEMS_PER_PAGE);
        if (page > maxPage) {
            page = maxPage;
        }

        displayKits(kits);

        inventory.setItem(45, createBackButton());
        if (page > 0) {
            inventory.setItem(48, createPreviousPageButton());
        }
        inventory.setItem(49, createSaveInventoryButton());
        if ((page + 1) * ITEMS_PER_PAGE < kits.size()) {
            inventory.setItem(50, createNextPageButton());
        }
    }

    private void displayKits(List<EndFlashKitManager.Kit> kits) {
        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < KIT_SLOTS.length && start + i < kits.size(); i++) {
            EndFlashKitManager.Kit kit = kits.get(start + i);
            inventory.setItem(KIT_SLOTS[i], createKitButton(kit, kits));
        }

        if (kits.isEmpty()) {
            inventory.setItem(22, createItem(Material.STRUCTURE_VOID,
                    "   §8[§7" + role.displayName() + " Kit池为空§8]",
                    "§8· · · · · · · · · · · · · ·",
                    "§f- §c暂时没有可用 Kit",
                    "§f- §b点击底部箱子上传你的当前背包",
                    "§f- §7为空时开局使用代码里的保底 Kit",
                    "§8· · · · · · · · · · · · · ·"));
        }
    }

    private ItemStack createKitButton(EndFlashKitManager.Kit kit, List<EndFlashKitManager.Kit> sameRoleKits) {
        Material material = Material.CHEST;
        List<ItemStack> items = kit.items();
        if (!items.isEmpty()) {
            material = items.get(0).getType();
        } else if (!kit.enderChestItems().isEmpty()) {
            material = Material.ENDER_CHEST;
        }

        double total = sameRoleKits.stream().mapToDouble(EndFlashKitManager.Kit::chance).sum();
        double balanced = total <= 0
                ? 100.0D / Math.max(1, sameRoleKits.size())
                : kit.chance() * 100.0D / total;

        List<String> lore = new ArrayList<>();
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add("§f- §eID: §7" + kit.id());
        lore.add("§f- §e输入权重: §d" + EndFlashKitManager.formatChance(kit.chance()) + " §7/ §d50");
        lore.add("§f- §e实际权重: §b" + String.format(Locale.ROOT, "%.1f", balanced) + "%");
        lore.add("§f- §e背包物品: §a" + items.size() + " §7件");
        lore.add("§f- §e末影箱物品: §5" + kit.enderChestItems().size() + " §7件");
        lore.add("§f- §b开局经验: §e" + kit.startExpLevel() + " §7级");
        lore.add("§f- §b创建者: §a" + kit.creatorName() + " §8/ §7" + kit.createdAtText());
        lore.add("§f- §d最后编辑: §f" + kit.lastEditorName() + " §8/ §e" + kit.lastEditedAtText());
        lore.add("§8· · · · · · · · · · · · · ·");
        lore.add("§f- §a左键查看整背包和调权重");
        lore.add("§f- §b右键读取到自己背包");
        lore.add("§f- §c右键会覆盖你的快捷栏、背包、盔甲和副手");

        return createItem(material, "   §8[§x§D§D§A§A§F§F" + kit.displayName() + "§8]", lore);
    }

    private ItemStack createSaveInventoryButton() {
        return createItem(Material.CHEST,
                "   §8[§x§5§5§F§F§A§A上传当前背包§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §b职业: §e" + role.displayName(),
                "§f- §a把你当前背包里的物品保存成新 Kit",
                "§f- §7默认权重: §e10",
                "§f- §7创建后进详情页可左键/右键调权重",
                "§8· · · · · · · · · · · · · ·",
                "§f- §a点击上传");
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        int kitIndex = kitIndexBySlot(slot);
        if (kitIndex >= 0) {
            List<EndFlashKitManager.Kit> kits = plugin.getEndFlashKitManager().getKits(role);
            int index = page * ITEMS_PER_PAGE + kitIndex;
            if (index < kits.size()) {
                EndFlashKitManager.Kit kit = kits.get(index);
                if (event.isRightClick()) {
                    loadKitToPlayerInventory(kit);
                } else {
                    playClickSound();
                    new EndFlashKitDetailMenu(plugin, player, role, kit.id(), page).open();
                }
            }
            return;
        }

        switch (slot) {
            case 45 -> {
                playClickSound();
                new EndFlashKitAdminMenu(plugin, player).open();
            }
            case 48 -> {
                if (page > 0) {
                    playPageTurnSound();
                    page--;
                    setupItems();
                }
            }
            case 49 -> createKitFromPlayer();
            case 50 -> {
                List<EndFlashKitManager.Kit> kits = plugin.getEndFlashKitManager().getKits(role);
                if ((page + 1) * ITEMS_PER_PAGE < kits.size()) {
                    playPageTurnSound();
                    page++;
                    setupItems();
                }
            }
            default -> {
            }
        }
    }

    private int kitIndexBySlot(int slot) {
        for (int i = 0; i < KIT_SLOTS.length; i++) {
            if (KIT_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    private void loadKitToPlayerInventory(EndFlashKitManager.Kit kit) {
        if (kit == null) {
            playErrorSound();
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§F§F§8§8§8§8⚠ §c这个 Kit 已经不存在，无法读取。");
            return;
        }
        player.getInventory().clear();
        player.getInventory().setStorageContents(kit.storageContents());
        player.getInventory().setArmorContents(kit.armorContents());
        player.getInventory().setItemInOffHand(kit.offHandItem());
        player.updateInventory();
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§8§8§D§D§F§F✦ §b已读取 §e" + role.displayName() + " §bKit §d" + kit.displayName()
                + " §b到你的背包。§7末影箱 Kit 不会被覆盖。");
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 0.72f, 1.28f);
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_IRON, 0.58f, 1.12f);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.45f, 1.72f);
    }

    private void createKitFromPlayer() {
        List<ItemStack> items = plugin.getEndFlashKitManager().snapshotInventory(player);
        if (items.isEmpty()) {
            playErrorSound();
            player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                    + "§x§F§F§8§8§8§8⚠ §c没有可保存的物品。");
            return;
        }

        String name = role.displayName() + "背包Kit-" + LocalTime.now().format(NAME_TIME_FORMAT);
        EndFlashKitManager.Kit kit = plugin.getEndFlashKitManager().createKit(role, name, 10,
                plugin.getEndFlashKitManager().snapshotInventoryLayout(player), player);
        player.sendMessage(plugin.getConfigManager().getHunterGamePrefix()
                + "§x§5§5§F§F§A§A✔ §a已上传当前背包为 §e" + role.displayName() + " §aKit：§d" + kit.displayName()
                + " §7(ID: §f" + kit.id() + "§7)");
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 0.75f, 1.25f);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.55f, 1.75f);
        setupItems();
    }
}
