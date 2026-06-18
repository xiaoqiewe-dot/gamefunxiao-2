package org.gamefunxiao.menu.hunter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.game.GameRoom;
import org.gamefunxiao.game.RoomManager;
import org.gamefunxiao.menu.base.BaseMenu;
import org.gamefunxiao.util.PlayerHeadUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InvitePlayerMenu extends BaseMenu {

    private static final Map<UUID, InvitePlayerMenu> WAITING_SEARCH_INPUTS = new ConcurrentHashMap<>();

    private final CreateRoomMenu parentMenu;
    private final Set<UUID> selectedPlayers = new HashSet<>();
    private int page = 0;
    private String searchKeyword = "";
    private static final int ITEMS_PER_PAGE = 21;

    public InvitePlayerMenu(GameFunXiao plugin, Player player, CreateRoomMenu parentMenu) {
        super(plugin, player, "§0§l👥 邀请玩家 👥", 54);
        this.parentMenu = parentMenu;
    }

    @Override
    protected void setupItems() {
        inventory.clear();

        // 米饭风格边框
        fillMiFanBorder();

        // 标题物品
        inventory.setItem(4, createTitleItem(Material.PLAYER_HEAD,
            "§x§5§5§F§F§D§D👥 §x§7§7§F§F§C§C邀§x§9§9§F§F§B§B请§x§B§B§F§F§A§A玩§x§D§D§F§F§9§9家 §x§5§5§F§F§D§D👥",
            "§8· · · · · · · · · · · · · ·",
            "§f已选择: §e" + selectedPlayers.size() + " §f位玩家",
            "§f点击玩家头颅选择/取消",
            "§8· · · · · · · · · · · · · ·"));

        // 显示在线玩家
        List<Player> onlinePlayers = getFilteredPlayers();
        int startIndex = page * ITEMS_PER_PAGE;

        int[] slots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
        };

        for (int i = 0; i < slots.length && startIndex + i < onlinePlayers.size(); i++) {
            Player p = onlinePlayers.get(startIndex + i);
            inventory.setItem(slots[i], createPlayerHead(p));
        }

        // 如果没有玩家
        if (onlinePlayers.isEmpty()) {
            inventory.setItem(22, createItem(Material.STRUCTURE_VOID,
                "   §8[§7没有玩家§8]",
                "§8· · · · · · · · · · · · · ·",
                "§f- §c没有符合条件的在线玩家",
                "§8· · · · · · · · · · · · · ·"));
        }

        // 上一页
        if (page > 0) {
            inventory.setItem(48, createPreviousPageButton());
        }

        // 搜索按钮
        inventory.setItem(49, createItem(Material.OAK_SIGN,
            "   §8[§x§F§F§E§E§5§5🔍 §x§F§F§D§D§7§7搜§x§F§F§C§C§9§9索§8]",
            "§8· · · · · · · · · · · · · ·",
            "§f- §e当前关键字: §b" + (searchKeyword.isEmpty() ? "无" : searchKeyword),
            "§f- §a点击输入搜索关键字",
            "§8· · · · · · · · · · · · · ·"));

        // 下一页
        if ((page + 1) * ITEMS_PER_PAGE < onlinePlayers.size()) {
            inventory.setItem(50, createNextPageButton());
        }

        // 确认按钮
        inventory.setItem(40, createItem(Material.EMERALD,
            "   §8[§x§5§5§F§F§5§5✓ §x§7§7§F§F§7§7确§x§9§9§F§F§9§9认§8]",
            "§8· · · · · · · · · · · · · ·",
            "§f- §a点击确认邀请",
            "§f- §e已选: §b" + selectedPlayers.size() + " §e位",
            "§8· · · · · · · · · · · · · ·"));

        // 返回按钮
        inventory.setItem(45, createBackButton());

    }

    private List<Player> getFilteredPlayers() {
        List<Player> players = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(player)) continue;
            if (plugin.getRoomManager().isInRoom(p.getUniqueId())) continue;

            if (searchKeyword.isEmpty() ||
                p.getName().toLowerCase().contains(searchKeyword.toLowerCase())) {
                players.add(p);
            }
        }
        return players;
    }

    private ItemStack createPlayerHead(Player p) {
        boolean selected = selectedPlayers.contains(p.getUniqueId());

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            PlayerHeadUtil.applyPlayerSkin(meta, p);

            String prefix = selected ? "§a✓ " : "§7  ";
            meta.setDisplayName(prefix + "§f" + p.getName());

            List<String> lore = new ArrayList<>();
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

        // 检查是否点击了玩家
        int slotIndex = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                slotIndex = i;
                break;
            }
        }

        if (slotIndex >= 0) {
            List<Player> onlinePlayers = getFilteredPlayers();
            int playerIndex = page * ITEMS_PER_PAGE + slotIndex;
            if (playerIndex < onlinePlayers.size()) {
                Player target = onlinePlayers.get(playerIndex);
                UUID targetUuid = target.getUniqueId();

                if (selectedPlayers.contains(targetUuid)) {
                    selectedPlayers.remove(targetUuid);
                    playCancelSound();
                } else {
                    selectedPlayers.add(targetUuid);
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
                // 搜索
                playClickSound();
                startSearchInput();
            }
            case 50 -> {
                // 下一页
                List<Player> onlinePlayers = getFilteredPlayers();
                if ((page + 1) * ITEMS_PER_PAGE < onlinePlayers.size()) {
                    playPageTurnSound();
                    page++;
                    setupItems();
                }
            }
            case 40 -> {
                // 确认
                playConfirmSound();
                inviteSelectedPlayers();
            }
            case 45 -> {
                // 返回
                playClickSound();
                parentMenu.open();
            }
        }
    }

    public void setSearchKeyword(String keyword) {
        this.searchKeyword = keyword;
        this.page = 0;
    }

    private void startSearchInput() {
        WAITING_SEARCH_INPUTS.put(player.getUniqueId(), this);
        player.closeInventory();
        applyInputState(player);
        player.sendTitle("§x§8§8§D§D§F§F✎ 请输入搜索关键字", "§7输入 §ecancel §7或蹲下取消，输入 §e清空 §7显示全部", 5, 80, 10);
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        player.sendMessage(room == null
                ? plugin.getMessageManager().getMessageWithPrefix("room.invite_search_prompt")
                : plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.invite_search_prompt"));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.65f, 1.45f);

        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            InvitePlayerMenu menu = WAITING_SEARCH_INPUTS.get(uuid);
            Player online = Bukkit.getPlayer(uuid);
            if (menu != this || online == null || !online.isOnline()) {
                return;
            }
            WAITING_SEARCH_INPUTS.remove(uuid);
            clearInputState(online);
            GameRoom onlineRoom = plugin.getRoomManager().getPlayerRoom(online.getUniqueId());
            online.sendMessage(onlineRoom == null
                    ? plugin.getMessageManager().getMessageWithPrefix("room.invite_search_timeout")
                    : plugin.getMessageManager().getRoomMessageWithPrefix(onlineRoom, "room.invite_search_timeout"));
            online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.75f);
            open();
        }, 30 * 20L);
    }

    private void inviteSelectedPlayers() {
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        if (selectedPlayers.isEmpty()) {
            playErrorSound();
            player.sendMessage(room == null
                    ? plugin.getMessageManager().getMessageWithPrefix("room.invite_menu_none_selected")
                    : plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.invite_menu_none_selected"));
            setupItems();
            return;
        }

        int success = 0;
        int skipped = 0;
        List<UUID> invitedNow = new ArrayList<>();
        for (UUID uuid : new ArrayList<>(selectedPlayers)) {
            Player target = Bukkit.getPlayer(uuid);
            if (target == null || !target.isOnline()) {
                skipped++;
                selectedPlayers.remove(uuid);
                continue;
            }

            RoomManager.InviteResult result = plugin.getRoomManager().invitePlayerToRoom(player, target);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", target.getName());

            switch (result) {
                case SUCCESS -> {
                    success++;
                    invitedNow.add(uuid);
                    if (room != null) {
                        placeholders.put("room_id", room.getRoomId());
                    }
                    player.sendMessage(room == null
                            ? plugin.getMessageManager().getMessageWithPrefix("room.invite_sent", placeholders)
                            : plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.invite_sent", placeholders));
                }
                case INVITER_NOT_IN_ROOM -> {
                    player.sendMessage(plugin.getMessageManager().getMessageWithPrefix("room.not_in_room"));
                    skipped++;
                }
                case NOT_OWNER -> {
                    player.sendMessage(room == null
                            ? plugin.getMessageManager().getMessageWithPrefix("room.invite_not_owner")
                            : plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.invite_not_owner"));
                    skipped++;
                }
                case ROOM_PUBLIC -> {
                    player.sendMessage(room == null
                            ? plugin.getMessageManager().getMessageWithPrefix("room.invite_public_room")
                            : plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.invite_public_room"));
                    skipped++;
                }
                case ROOM_UNAVAILABLE -> {
                    player.sendMessage(room == null
                            ? plugin.getMessageManager().getMessageWithPrefix("room.invite_room_unavailable")
                            : plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.invite_room_unavailable"));
                    skipped++;
                }
                case TARGET_SELF -> {
                    player.sendMessage(room == null
                            ? plugin.getMessageManager().getMessageWithPrefix("room.invite_target_self")
                            : plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.invite_target_self"));
                    skipped++;
                }
                case TARGET_ALREADY_IN_ROOM -> {
                    player.sendMessage(room == null
                            ? plugin.getMessageManager().getMessageWithPrefix("room.invite_target_in_room", placeholders)
                            : plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.invite_target_in_room", placeholders));
                    skipped++;
                }
                case TARGET_IN_OTHER_ROOM -> {
                    player.sendMessage(room == null
                            ? plugin.getMessageManager().getMessageWithPrefix("room.invite_target_busy", placeholders)
                            : plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.invite_target_busy", placeholders));
                    skipped++;
                }
                case TARGET_ALREADY_INVITED -> {
                    player.sendMessage(room == null
                            ? plugin.getMessageManager().getMessageWithPrefix("room.invite_target_already_invited", placeholders)
                            : plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.invite_target_already_invited", placeholders));
                    selectedPlayers.remove(uuid);
                    skipped++;
                }
            }
        }

        selectedPlayers.removeAll(invitedNow);
        Map<String, String> summary = new HashMap<>();
        summary.put("success", String.valueOf(success));
        summary.put("skipped", String.valueOf(skipped));
        player.sendMessage(room == null
                ? plugin.getMessageManager().getMessageWithPrefix("room.invite_menu_done", summary)
                : plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.invite_menu_done", summary));
        if (success > 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.9f, 1.25f);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.55f);
        } else {
            playErrorSound();
        }
        parentMenu.open();
    }

    public static boolean isWaitingSearchInput(UUID uuid) {
        return uuid != null && WAITING_SEARCH_INPUTS.containsKey(uuid);
    }

    public static boolean handleSearchChatInput(GameFunXiao plugin, Player player, String input) {
        InvitePlayerMenu menu = WAITING_SEARCH_INPUTS.remove(player.getUniqueId());
        if (menu == null) {
            return false;
        }

        clearInputState(player);
        String keyword = input == null ? "" : input.trim();
        if (keyword.equalsIgnoreCase("cancel") || keyword.equalsIgnoreCase("取消")) {
            GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
            player.sendMessage(room == null
                    ? plugin.getMessageManager().getMessageWithPrefix("room.invite_search_cancelled")
                    : plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.invite_search_cancelled"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
            menu.open();
            return true;
        }

        if (keyword.equalsIgnoreCase("clear") || keyword.equalsIgnoreCase("清空") || keyword.equalsIgnoreCase("全部")) {
            keyword = "";
        }
        menu.setSearchKeyword(keyword);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("keyword", keyword.isEmpty() ? "无" : keyword);
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        player.sendMessage(room == null
                ? plugin.getMessageManager().getMessageWithPrefix("room.invite_search_set", placeholders)
                : plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.invite_search_set", placeholders));
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.55f);
        menu.open();
        return true;
    }

    public static boolean cancelSearchInput(GameFunXiao plugin, Player player, boolean reopenMenu) {
        InvitePlayerMenu menu = WAITING_SEARCH_INPUTS.remove(player.getUniqueId());
        if (menu == null) {
            return false;
        }
        clearInputState(player);
        GameRoom room = plugin.getRoomManager().getPlayerRoom(player.getUniqueId());
        player.sendMessage(room == null
                ? plugin.getMessageManager().getMessageWithPrefix("room.invite_search_cancelled")
                : plugin.getMessageManager().getRoomMessageWithPrefix(room, "room.invite_search_cancelled"));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
        if (reopenMenu) {
            menu.open();
        }
        return true;
    }

    private static void applyInputState(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30 * 20, 6, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 30 * 20, 0, false, false, false));
    }

    private static void clearInputState(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.DARKNESS);
        player.resetTitle();
    }
}
