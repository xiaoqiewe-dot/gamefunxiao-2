package org.yuyun.brickguard;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@SuppressWarnings({"deprecation", "removal"})
public final class YuYunBrickGuardPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private NamespacedKey actionKey;
    private NamespacedKey typeKey;
    private NamespacedKey roomKey;
    private ItemFactory items;
    private MapManager maps;
    private final Map<String, Room> rooms = new LinkedHashMap<>();
    private final Map<UUID, String> playerRoom = new HashMap<>();
    private final Map<UUID, OpenMenu> openMenus = new HashMap<>();
    private final Map<UUID, EditSession> editSessions = new HashMap<>();
    private final Map<UUID, InventorySnapshot> toolkitSnapshots = new HashMap<>();
    private final Map<UUID, Long> shoutCooldowns = new HashMap<>();
    private final Map<UUID, Long> clickCooldowns = new HashMap<>();
    private final Set<UUID> downingPlayers = new HashSet<>();
    private final Set<UUID> homeNightVisionPlayers = new HashSet<>();
    private final Set<UUID> respawnGhosts = new HashSet<>();
    private BukkitTask ticker;
    private int roomCounter;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        actionKey = new NamespacedKey(this, "action");
        typeKey = new NamespacedKey(this, "type");
        roomKey = new NamespacedKey(this, "room");
        items = new ItemFactory(actionKey, typeKey);
        maps = new MapManager(this);
        maps.createEditWorlds();
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("brickguard") != null) {
            Objects.requireNonNull(getCommand("brickguard")).setExecutor(this);
            Objects.requireNonNull(getCommand("brickguard")).setTabCompleter(this);
        }
        ticker = Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 20L);
        getLogger().info("板砖守卫战已启动。");
    }

    @Override
    public void onDisable() {
        if (ticker != null) ticker.cancel();
        for (Room room : new ArrayList<>(rooms.values())) {
            if (room.status == Room.Status.ENDED) cleanupRoom(room);
            else endRoom(room, null, false);
        }
        for (UUID uuid : new ArrayList<>(editSessions.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) exitEdit(player, false);
        }
        maps.clearAllPreviews();
    }

    private String prefix() {
        return "";
    }

    private void msg(CommandSender sender, String text) {
        sender.sendMessage(Text.c(prefix() + text));
        if (sender instanceof Player player) {
            playSound(player, messageSoundKey(text), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.45F, 1.35F);
        }
    }

    private void raw(CommandSender sender, String text) {
        sender.sendMessage(Text.c(text));
    }

    private String messageSoundKey(String text) {
        String raw = strip(text == null ? "" : text);
        if (raw.contains("没有") || raw.contains("不足") || raw.contains("不能") || raw.contains("失败") || raw.contains("缺少") || raw.contains("禁止")) {
            return "message.fail";
        }
        if (raw.contains("已") || raw.contains("成功") || raw.contains("进入") || raw.contains("选择")) {
            return "message.success";
        }
        return "message.info";
    }

    private int cfg(String path, int def) {
        return getConfig().getInt(path, def);
    }

    private boolean isAdmin(CommandSender sender) {
        return sender.hasPermission("brickguard.admin") || sender.hasPermission("yuyunbrickguard.admin");
    }

    private boolean hasUsePermission(CommandSender sender) {
        return sender.hasPermission("brickguard.use") || sender.hasPermission("yuyunbrickguard.use");
    }

    private boolean isPlayer(CommandSender sender) {
        if (sender instanceof Player) return true;
        raw(sender, "§c这个命令需要玩家执行。");
        return false;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!hasUsePermission(sender)) {
            msg(sender, "§x§F§F§8§8§5§5你没有权限进入这场守卫战。");
            return true;
        }
        switch (sub) {
            case "menu" -> {
                if (!isPlayer(sender)) return true;
                openMainMenu((Player) sender);
            }
            case "rooms", "roomlist" -> {
                if (!isPlayer(sender)) return true;
                openRooms((Player) sender);
            }
            case "rank", "leaderboard" -> {
                if (!isPlayer(sender)) return true;
                openRank((Player) sender);
            }
            case "quick" -> {
                if (!isPlayer(sender)) return true;
                quickJoin((Player) sender);
            }
            case "create" -> {
                if (!isPlayer(sender)) return true;
                createRoom((Player) sender);
            }
            case "leave", "quit" -> {
                if (!isPlayer(sender)) return true;
                leave((Player) sender, true);
            }
            case "team" -> {
                if (!isPlayer(sender)) return true;
                Player player = (Player) sender;
                if (args.length >= 2) selectTeam(player, Team.fromAction("team_" + args[1].toLowerCase(Locale.ROOT)));
                else openTeamMenu(player);
            }
            case "map" -> handleMapCommand(sender, args);
            case "reload" -> {
                if (!isAdmin(sender)) {
                    msg(sender, "§x§F§F§8§8§5§5你没有权限刷新配置。");
                    return true;
                }
                saveDefaultConfig();
                reloadConfig();
                msg(sender, "§x§7§D§F§F§C§8配置已重新读取。");
            }
            default -> msg(sender, "§x§F§F§8§8§5§5没有这个分支，输入 §f/" + label + " help §x§F§F§8§8§5§5查看命令。");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        raw(sender, "§x§f§f§7§c§0§0板砖守卫战 §7/ §f命令");
        raw(sender, "§8- §f/brickguard menu §7打开菜单");
        raw(sender, "§8- §f/brickguard rooms §7查看房间");
        raw(sender, "§8- §f/brickguard rank §7查看排行");
        raw(sender, "§8- §f/brickguard quick §7快速加入");
        raw(sender, "§8- §f/brickguard create §7创建房间");
        raw(sender, "§8- §f/brickguard leave §7离开房间");
        raw(sender, "§8- §f/brickguard team §7选择队伍");
        raw(sender, "§8- §f/teammsg <内容> §7队伍聊天");
        if (isAdmin(sender)) {
            raw(sender, "§x§7§D§F§F§C§8管理命令");
            raw(sender, "§8- §f/brickguard map create lobby|brick|nether|all §7创建编辑世界");
            raw(sender, "§8- §f/brickguard map edit lobby|brick|nether §7进入地图编辑");
            raw(sender, "§8- §f/brickguard map import lobby|brick|nether <世界名> §7加入可编辑地图");
            raw(sender, "§8- §f/brickguard map toolkit §7领取地图工具包");
            raw(sender, "§8- §f/brickguard map menu §7打开地图菜单");
            raw(sender, "§8- §f/brickguard map check §7检查地图元素");
            raw(sender, "§8- §f/brickguard map setenabled true|false §7设置地图是否加入游戏");
            raw(sender, "§8- §f/brickguard map leave §7退出地图编辑");
            raw(sender, "§8- §f/brickguard reload §7重载配置");
        }
    }

    private void handleMapCommand(CommandSender sender, String[] args) {
        if (!isAdmin(sender)) {
            msg(sender, "§x§F§F§8§8§5§5你没有权限编辑地图。");
            return;
        }
        if (args.length < 2) {
            msg(sender, "§x§F§F§B§B§6§6用法：§f/brickguard map menu");
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> {
                if (args.length >= 3 && !"all".equalsIgnoreCase(args[2])) {
                    MapSide side = MapSide.parse(args[2]);
                    maps.createEditWorld(side);
                    msg(sender, "§x§7§D§F§F§C§8已准备 §f" + side.display + "§x§7§D§F§F§C§8 编辑世界。");
                } else {
                    maps.createEditWorlds();
                    msg(sender, "§x§7§D§F§F§C§8编辑世界已准备。");
                }
            }
            case "edit" -> {
                if (!isPlayer(sender)) return;
                MapSide side = args.length >= 3 ? MapSide.parse(args[2]) : MapSide.LOBBY;
                enterEdit((Player) sender, side);
            }
            case "toolkit" -> {
                if (!isPlayer(sender)) return;
                giveToolkit((Player) sender);
            }
            case "menu" -> {
                if (!isPlayer(sender)) return;
                openMapMenu((Player) sender);
            }
            case "check" -> checkMap(sender);
            case "setenabled" -> {
                boolean enabled = args.length >= 3 && Boolean.parseBoolean(args[2]);
                MapData data = maps.read();
                List<String> missing = data.missing();
                if (enabled && !missing.isEmpty()) {
                    msg(sender, "§x§F§F§8§8§5§5还缺少：§f" + String.join("§7、§f", missing));
                    return;
                }
                maps.setEnabled(enabled);
                msg(sender, enabled ? "§x§7§D§F§F§C§8地图已允许加入游戏。" : "§x§F§F§B§B§6§6地图已从游戏池移除。");
            }
            case "import", "useworld" -> {
                if (args.length < 4) {
                    msg(sender, "§x§F§F§B§B§6§6用法：§f/brickguard map import lobby|brick|nether <世界名>");
                    return;
                }
                MapSide side = MapSide.parse(args[2]);
                World world = Bukkit.getWorld(args[3]);
                if (world == null) {
                    msg(sender, "§x§F§F§8§8§5§5没有找到这个世界。");
                    return;
                }
                String key = switch (side) {
                    case LOBBY -> "lobby_world";
                    case BRICK -> "brick_world";
                    case NETHER -> "nether_world";
                };
                getConfig().set(key, world.getName());
                saveConfig();
                msg(sender, "§x§7§D§F§F§C§8已加入 §f" + side.display + "§x§7§D§F§F§C§8 可编辑地图。");
            }
            case "leave", "exit" -> {
                if (!isPlayer(sender)) return;
                exitEdit((Player) sender, true);
            }
            default -> msg(sender, "§x§F§F§8§8§5§5没有这个地图分支。");
        }
    }

    private void checkMap(CommandSender sender) {
        MapData data = maps.read();
        List<String> missing = data.missing();
        if (missing.isEmpty()) {
            msg(sender, data.enabled ? "§x§7§D§F§F§C§8地图元素完整，已允许加入游戏。" : "§x§F§F§B§B§6§6地图元素完整，但还没有允许加入游戏。");
        } else {
            msg(sender, "§x§F§F§8§8§5§5还缺少：§f" + String.join("§7、§f", missing));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(List.of("help", "menu", "rooms", "rank", "quick", "create", "leave", "team"));
            if (isAdmin(sender)) list.addAll(List.of("map", "reload"));
            return filter(list, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("team")) return filter(List.of("brick", "nether"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("map") && isAdmin(sender)) return filter(List.of("create", "edit", "toolkit", "menu", "check", "setenabled", "import", "useworld", "leave"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("map") && args[1].equalsIgnoreCase("create") && isAdmin(sender)) return filter(List.of("lobby", "brick", "nether", "all"), args[2]);
        if (args.length == 3 && args[0].equalsIgnoreCase("map") && (args[1].equalsIgnoreCase("edit") || args[1].equalsIgnoreCase("import") || args[1].equalsIgnoreCase("useworld")) && isAdmin(sender)) return filter(List.of("lobby", "brick", "nether"), args[2]);
        if (args.length == 4 && args[0].equalsIgnoreCase("map") && (args[1].equalsIgnoreCase("import") || args[1].equalsIgnoreCase("useworld")) && isAdmin(sender)) return filter(Bukkit.getWorlds().stream().map(World::getName).toList(), args[3]);
        if (args.length == 3 && args[0].equalsIgnoreCase("map") && args[1].equalsIgnoreCase("setenabled") && isAdmin(sender)) return filter(List.of("true", "false"), args[2]);
        return List.of();
    }

    private List<String> filter(List<String> list, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return list.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(p)).toList();
    }

    private void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(player, 54, Text.c("§0§l板砖守卫战"));
        frame(inv, Material.ORANGE_STAINED_GLASS_PANE, "§x§F§F§8§8§2§2你看我干什么");
        inv.setItem(4, items.item(Material.BRICK, "§x§F§F§8§8§2§2[ 板砖守卫战 ]", List.of("§f- §a双队主题对抗")));
        inv.setItem(22, items.action(Material.NETHER_STAR, "§x§7§D§F§F§C§8[ 快速加入 ]", List.of("§f- §a进入等待中的房间"), "main_quick"));
        inv.setItem(0, items.action(Material.EMERALD, "§x§7§D§F§F§C§8[ 排行榜 ]", List.of("§f- §a查看本场积分"), "main_rank"));
        inv.setItem(8, items.action(Material.BOOK, "§x§7§D§F§F§C§8[ 房间列表 ]", List.of("§f- §a查看等待中的房间"), "main_rooms"));
        inv.setItem(45, items.action(Material.BARRIER, "§x§F§F§8§8§5§5[ 关闭 ]", List.of("§f- §7关闭菜单"), "close"));
        inv.setItem(53, items.action(Material.CRAFTING_TABLE, "§x§F§F§8§8§2§2[ 创建房间 ]", List.of("§f- §a创建等待房间"), "main_create"));
        player.openInventory(inv);

        openMenus.put(player.getUniqueId(), new OpenMenu(MenuType.MAIN, ""));
        menuSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 1.25F);
    }

    private void openRooms(Player player) {
        Inventory inv = Bukkit.createInventory(player, 45, Text.c("§0§l守卫战房间"));
        frame(inv, Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§x§7§D§F§F§C§8你看我干什么");
        inv.setItem(4, items.item(Material.COMPASS, "§x§7§D§F§F§C§8[ 房间列表 ]", List.of("§f- §a只显示等待中的房间")));
        List<Room> waiting = rooms.values().stream().filter(r -> r.status == Room.Status.WAITING).toList();
        if (waiting.isEmpty()) {
            inv.setItem(22, items.action(Material.GRAY_DYE, "§x§B§B§B§B§B§B[ 还没有房间 ]", List.of("§f- §7可以去创建一个"), "main_create"));
        } else {
            int[] slots = centeredSlots(27, waiting.size());
            for (int i = 0; i < Math.min(slots.length, waiting.size()); i++) {
                Room room = waiting.get(i);
                inv.setItem(slots[i], items.action(Material.OAK_DOOR, "§x§7§D§F§F§C§8[ 房间 " + room.id + " ]", List.of(
                        "§f- §a人数 §f" + room.players.size() + "§7/§f" + cfg("max_players", 128),
                        "§f- §x§f§f§7§c§0§0板砖 §f" + room.count(Team.BRICK) + " §7| §x§6§6§1§9§0§0下界 §f" + room.count(Team.NETHER)
                ), "join_" + room.id));
            }
        }
        inv.setItem(36, items.action(Material.ARROW, "§x§F§F§B§B§6§6[ 返回 ]", List.of("§f- §7回到主菜单"), "back_main"));
        player.openInventory(inv);

        openMenus.put(player.getUniqueId(), new OpenMenu(MenuType.MAIN, "rooms"));
        menuSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.8F, 1.4F);
    }

    private void openRank(Player player) {
        Inventory inv = Bukkit.createInventory(player, 45, Text.c("§0§l守卫战榜单"));
        frame(inv, Material.CYAN_STAINED_GLASS_PANE, "§x§7§D§F§F§C§8你看我干什么");
        inv.setItem(4, items.item(Material.EMERALD, "§x§7§D§F§F§C§8[ 本场积分 ]", List.of("§f- §a当前在线房间内统计")));
        List<Player> players = rooms.values().stream().flatMap(r -> r.onlinePlayers().stream()).distinct().toList();
        List<Player> sorted = players.stream().sorted(Comparator.comparingInt((Player p) -> activeRoom(p).map(r -> r.kills.getOrDefault(p.getUniqueId(), 0)).orElse(0)).reversed()).limit(7).toList();
        int[] slots = centeredSlots(27, Math.max(1, sorted.size()));
        if (sorted.isEmpty()) {
            inv.setItem(22, items.item(Material.PAPER, "§x§B§B§B§B§B§B[ 暂无数据 ]", List.of("§f- §7开局后会显示击杀数")));
        } else {
            for (int i = 0; i < sorted.size(); i++) {
                Player p = sorted.get(i);
                Room r = activeRoom(p).orElse(null);
                Team team = r == null ? Team.BRICK : r.team(p.getUniqueId());
                inv.setItem(slots[i], items.item(Material.PLAYER_HEAD, team.color + "[ " + p.getName() + " ]", List.of("§f- §a击杀 §f" + (r == null ? 0 : r.kills.getOrDefault(p.getUniqueId(), 0)))));
            }
        }
        inv.setItem(36, items.action(Material.ARROW, "§x§F§F§B§B§6§6[ 返回 ]", List.of("§f- §7回到主菜单"), "back_main"));
        player.openInventory(inv);

        openMenus.put(player.getUniqueId(), new OpenMenu(MenuType.MAIN, "rank"));
        menuSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9F, 1.5F);
    }

    private void openTeamMenu(Player player) {
        Optional<Room> optional = activeRoom(player);
        if (optional.isEmpty()) {
            msg(player, "§x§F§F§8§8§5§5你还没有进入房间。");
            return;
        }
        Room room = optional.get();
        Inventory inv = Bukkit.createInventory(player, 27, Text.c("§0§l选择队伍"));
        frame(inv, Material.BLACK_STAINED_GLASS_PANE, "§8你看我干什么");
        inv.setItem(4, items.item(Material.COMPASS, "§x§7§D§F§F§C§8[ 队伍选择 ]", List.of("§f- §a选择想加入的一方")));
        inv.setItem(11, teamButton(room, Team.BRICK));
        inv.setItem(15, teamButton(room, Team.NETHER));
        player.openInventory(inv);

        openMenus.put(player.getUniqueId(), new OpenMenu(MenuType.TEAM, room.id));
        menuSound(player, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8F, 1.2F);
    }

    private ItemStack teamButton(Room room, Team team) {
        List<String> lore = new ArrayList<>();
        lore.add("§f- §a已选择 §f" + room.count(team) + " §a人");
        List<String> names = room.players.stream().filter(uuid -> room.team(uuid) == team).map(Bukkit::getOfflinePlayer).map(offline -> offline.getName() == null ? "未知玩家" : offline.getName()).limit(5).toList();
        if (names.isEmpty()) lore.add("§f- §7还没有玩家选择");
        else names.forEach(name -> lore.add("§8- §f" + name));
        ItemStack item = items.action(team.icon, team.color + team.display, lore, "team_" + team.id);
        item.setAmount(Math.max(1, Math.min(99, room.count(team))));
        item.editMeta(meta -> meta.setMaxStackSize(99));
        return item;
    }

    private void openMapMenu(Player player) {
        Inventory inv = Bukkit.createInventory(player, 45, Text.c("§0§l守卫战地图"));
        frame(inv, Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§x§7§D§F§F§C§8你看我干什么");
        inv.setItem(4, items.item(Material.MAP, "§x§7§D§F§F§C§8[ 地图编辑 ]", List.of("§f- §a选择编辑大厅或双方地图")));
        inv.setItem(20, items.action(Material.COMPASS, "§x§7§D§F§F§C§8[ 等待大厅 ]", List.of("§f- §a设置玩家等待位置"), "map_edit_lobby"));
        inv.setItem(22, items.action(Material.BRICKS, "§x§f§f§7§c§0§0[ 板砖地图 ]", List.of("§f- §a设置核心、商人和矿点"), "map_edit_brick"));
            inv.setItem(24, items.action(Material.NETHER_BRICKS, "§x§6§6§1§9§0§0[ 下界砖地图 ]", List.of("§f- §a设置出生点、商人、矿点和黑曜石池"), "map_edit_nether"));
        inv.setItem(40, items.action(Material.SPYGLASS, "§x§7§D§F§F§C§8[ 检查元素 ]", List.of("§f- §a查看地图缺少什么"), "map_check"));
        inv.setItem(36, items.action(Material.BARRIER, "§x§F§F§8§8§5§5[ 关闭 ]", List.of("§f- §7关闭这个菜单"), "close"));
        player.openInventory(inv);

        openMenus.put(player.getUniqueId(), new OpenMenu(MenuType.MAP_MAIN, ""));
        menuSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 0.8F, 1.45F);
    }

    private void openEditorMenu(Player player) {
        EditSession session = editSessions.get(player.getUniqueId());
        if (session == null) {
            msg(player, "§x§F§F§8§8§5§5请先进入地图编辑。");
            return;
        }
        MapSide side = session.side();
        Inventory inv = Bukkit.createInventory(player, 54, Text.c("§0§l地图编辑 §8- §7" + side.display));
        frame(inv, side == MapSide.NETHER ? Material.RED_STAINED_GLASS_PANE : side == MapSide.BRICK ? Material.ORANGE_STAINED_GLASS_PANE : Material.LIGHT_BLUE_STAINED_GLASS_PANE, side.color + "你看我干什么");
        inv.setItem(4, items.item(side.icon, side.color + "[ " + side.display + " ]", List.of("§f- §a点击按钮会立刻保存当前位置", "§f- §7方块类点位会生成预览实体")));
        if (side == MapSide.LOBBY) {
            inv.setItem(22, items.action(Material.COMPASS, "§x§7§D§F§F§C§8[ 大厅出生点 ]", List.of("§f- §a保存当前位置"), "set_lobby_spawn"));
        } else if (side == MapSide.BRICK) {
            inv.setItem(19, items.action(Material.RESPAWN_ANCHOR, "§x§f§f§7§c§0§0[ 板砖出生点 ]", List.of("§f- §a保存当前位置"), "set_brick_spawn"));
            inv.setItem(21, items.action(Material.RED_GLAZED_TERRACOTTA, "§x§f§f§7§c§0§0[ 板砖核心 ]", List.of("§f- §a保存当前方块位置"), "set_brick_core"));
            inv.setItem(23, items.action(Material.OBSIDIAN, "§x§f§f§7§c§0§0[ 板砖门位置 ]", List.of("§f- §a保存门生成位置"), "set_brick_portal"));
            inv.setItem(25, items.action(Material.VILLAGER_SPAWN_EGG, "§x§f§f§7§c§0§0[ 板砖商人 ]", List.of("§f- §a添加当前位置"), "add_brick_trader"));
            inv.setItem(31, items.action(Material.REDSTONE_BLOCK, "§x§F§F§8§8§5§5[ 清空板砖商人 ]", List.of("§f- §c清空已添加的商人"), "clear_brick_points"));
        } else {
            inv.setItem(19, items.action(Material.RESPAWN_ANCHOR, "§x§6§6§1§9§0§0[ 下界出生点 ]", List.of("§f- §a保存当前位置"), "set_nether_spawn"));
            inv.setItem(21, items.action(Material.CRYING_OBSIDIAN, "§x§6§6§1§9§0§0[ 下界门位置 ]", List.of("§f- §a保存门展示位置"), "set_nether_portal"));
            inv.setItem(23, items.item(Material.CRYING_OBSIDIAN, "§x§6§6§1§9§0§0[ 黑曜石池自动识别 ]", List.of("§f- §a在地图放置哭泣的黑曜石即可", "§f- §7掉落物黑曜石落在上方会自动计数")));
            inv.setItem(25, items.action(Material.PIGLIN_SPAWN_EGG, "§x§6§6§1§9§0§0[ 下界商人 ]", List.of("§f- §a添加当前位置"), "add_nether_trader"));
            inv.setItem(31, items.action(Material.REDSTONE_BLOCK, "§x§F§F§8§8§5§5[ 清空下界商人 ]", List.of("§f- §c清空已添加的商人"), "clear_nether_points"));
        }
        inv.setItem(45, items.action(Material.SPYGLASS, "§x§7§D§F§F§C§8[ 检查元素 ]", List.of("§f- §a检查地图完整度"), "map_check"));
        inv.setItem(49, items.action(Material.PAPER, "§x§7§D§F§F§C§8[ 领取工具包 ]", List.of("§f- §a工具包会替换当前物品栏", "§f- §7可移动，关闭后恢复"), "give_toolkit"));
        inv.setItem(53, items.action(Material.ARROW, "§x§F§F§B§B§6§6[ 返回 ]", List.of("§f- §7回到地图选择"), "back_map"));
        player.openInventory(inv);

        openMenus.put(player.getUniqueId(), new OpenMenu(MenuType.MAP_EDITOR, side.name()));
        menuSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8F, 1.15F);
    }

    private void frame(Inventory inv, Material corner, String glassName) {
        ItemStack side = items.item(Material.GRAY_STAINED_GLASS_PANE, "§8你看我干什么", List.of("§8虽然你点我也没有用ewe"));
        ItemStack c = items.item(corner, glassName, List.of("§8虽然你点我也没有用ewe"));
        int size = inv.getSize();
        int rows = size / 9;
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) inv.setItem(i, side);
        }
        inv.setItem(0, c);
        inv.setItem(8, c);
        inv.setItem(size - 9, c);
        inv.setItem(size - 1, c);
    }

    private int[] centeredSlots(int areaSize, int count) {
        int capped = Math.max(1, Math.min(areaSize, count));
        List<Integer> slots = new ArrayList<>();
        int rows = Math.max(1, (int) Math.ceil(capped / 7.0D));
        int startRow = Math.max(1, 1 + (3 - rows) / 2);
        int left = capped;
        for (int row = 0; row < rows && left > 0; row++) {
            int rowCount = Math.min(7, left);
            int startCol = 4 - rowCount / 2;
            if (rowCount % 2 == 0) startCol++;
            for (int col = startCol; col < startCol + rowCount; col++) {
                slots.add((startRow + row) * 9 + col);
            }
            left -= rowCount;
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private void menuSound(Player player, Sound sound, float volume, float pitch) {
        playSound(player, "menu.open", sound, Math.min(volume, 0.75F), pitch);
    }

    private void playSound(Player player, String key, Sound fallback, float volume, float pitch) {
        if (player == null) return;
        playConfiguredSound(player.getLocation(), key, fallback, volume, pitch, List.of(player), new HashSet<>());
    }

    private void playRoomSound(Room room, String key, Sound fallback, float volume, float pitch) {
        if (room == null) return;
        List<Player> players = room.onlinePlayers();
        for (Player player : players) {
            playSound(player.getLocation(), key, fallback, volume, pitch, List.of(player));
        }
    }

    private void playWorldSound(Location location, String key, Sound fallback, float volume, float pitch) {
        playConfiguredSound(location, key, fallback, volume, pitch, null, new HashSet<>());
    }

    private void playSound(Location location, String key, Sound fallback, float volume, float pitch, List<Player> targets) {
        playConfiguredSound(location, key, fallback, volume, pitch, targets, new HashSet<>());
    }

    private void playConfiguredSound(Location location, String key, Sound fallback, float volume, float pitch, List<Player> targets, Set<String> stack) {
        if (location == null || location.getWorld() == null) return;
        if (key != null && !stack.add(key)) return;
        List<SoundSpec> specs = soundSpecs(key);
        if (specs.isEmpty() && fallback != null) specs = List.of(new SoundSpec(fallback, volume, pitch, 0L));
        for (SoundSpec spec : specs) {
            Runnable run = () -> {
                if (spec.aliasKey() != null) {
                    playConfiguredSound(location, spec.aliasKey(), fallback, volume, pitch, targets, new HashSet<>(stack));
                } else if (targets == null) location.getWorld().playSound(location, spec.sound(), spec.volume(), spec.pitch());
                else for (Player player : targets) if (player != null && player.isOnline()) player.playSound(player.getLocation(), spec.sound(), spec.volume(), spec.pitch());
            };
            if (spec.delay() <= 0L) run.run();
            else Bukkit.getScheduler().runTaskLater(this, run, spec.delay());
        }
    }

    private List<SoundSpec> soundSpecs(String key) {
        String path = "sounds." + key;
        if (!getConfig().contains(path)) return List.of();
        List<SoundSpec> out = new ArrayList<>();
        List<Map<?, ?>> maps = getConfig().getMapList(path);
        if (!maps.isEmpty()) {
            for (Map<?, ?> map : maps) {
                Object soundValue = map.get("sound");
                String rawSound = soundValue == null ? "" : String.valueOf(soundValue);
                Sound sound = parseSound(rawSound);
                String alias = sound == null && rawSound.startsWith("@") ? rawSound.substring(1) : null;
                if (sound == null && alias == null) continue;
                float volume = num(map.get("volume"), 0.8F);
                float pitch = num(map.get("pitch"), 1.0F);
                long delay = Math.max(0L, Math.round(num(map.get("delay"), 0.0F)));
                out.add(new SoundSpec(sound, volume, pitch, delay, alias));
            }
            return out;
        }
        for (String raw : getConfig().getStringList(path)) {
            String[] split = raw.trim().split("\\s+");
            if (split.length == 0) continue;
            Sound sound = parseSound(split[0]);
            String alias = sound == null && split[0].startsWith("@") ? split[0].substring(1) : null;
            if (sound == null && alias == null) continue;
            float vol = split.length >= 2 ? parseFloat(split[1], 0.8F) : 0.8F;
            float pit = split.length >= 3 ? parseFloat(split[2], 1.0F) : 1.0F;
            long delay = split.length >= 4 ? Math.max(0L, Math.round(parseFloat(split[3], 0.0F))) : 0L;
            out.add(new SoundSpec(sound, vol, pit, delay, alias));
        }
        return out;
    }

    private Sound parseSound(String name) {
        if (name == null || name.isBlank()) return null;
        String normalized = name.trim().toUpperCase(Locale.ROOT).replace('.', '_').replace("MINECRAFT:", "");
        try {
            return Sound.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            getLogger().warning("未知音效: " + name);
            return null;
        }
    }

    private float parseFloat(String raw, float def) {
        try {
            return Float.parseFloat(raw);
        } catch (Exception ignored) {
            return def;
        }
    }

    private float num(Object value, float def) {
        if (value instanceof Number number) return number.floatValue();
        if (value == null) return def;
        return parseFloat(String.valueOf(value), def);
    }

    private record SoundSpec(Sound sound, float volume, float pitch, long delay, String aliasKey) {
        SoundSpec(Sound sound, float volume, float pitch, long delay) {
            this(sound, volume, pitch, delay, null);
        }
    }

    private void createRoom(Player owner) {
        if (playerRoom.containsKey(owner.getUniqueId())) {
            msg(owner, "§x§F§F§B§B§6§6你已经在房间里。");
            return;
        }
        MapData data = maps.read();
        if (!data.ready()) {
            msg(owner, data.missing().isEmpty() ? "§x§F§F§B§B§6§6地图还没有允许加入游戏。" : "§x§F§F§8§8§5§5地图还缺少：§f" + String.join("§7、§f", data.missing()));
            return;
        }
        String id = nextRoomId();
        Room room = new Room(id, owner.getUniqueId(), cfg("waiting_seconds", 30), cfg("game_seconds", 3600), cfg("brick_core_health", 500));
        if (!prepareWaitingLobby(room)) {
            msg(owner, "§x§F§F§8§8§5§5等待大厅复制失败，房间没有创建。");
            return;
        }
        rooms.put(id, room);
        joinRoom(owner, room);
        msg(owner, "§x§7§D§F§F§C§8已创建房间 §f" + id + "§x§7§D§F§F§C§8。");
    }

    private String nextRoomId() {
        int id = 1;
        while (rooms.containsKey(String.valueOf(id))) id++;
        roomCounter = Math.max(roomCounter, id);
        return String.valueOf(id);
    }

    private void quickJoin(Player player) {
        if (playerRoom.containsKey(player.getUniqueId())) {
            msg(player, "§x§F§F§B§B§6§6你已经在房间里。");
            return;
        }
        Optional<Room> waiting = rooms.values().stream().filter(r -> r.status == Room.Status.WAITING && r.players.size() < cfg("max_players", 128)).findFirst();
        if (waiting.isPresent()) joinRoom(player, waiting.get());
        else createRoom(player);
    }

    private void joinRoom(Player player, Room room) {
        if (room.status != Room.Status.WAITING) {
            msg(player, "§x§F§F§8§8§5§5这个房间已经开局。");
            return;
        }
        if (room.players.size() >= cfg("max_players", 128)) {
            msg(player, "§x§F§F§8§8§5§5这个房间人数已满。");
            return;
        }
        if (room.lobbyWorld == null && !prepareWaitingLobby(room)) {
            msg(player, "§x§F§F§8§8§5§5等待大厅还没有准备好。");
            return;
        }
        room.snapshots.put(player.getUniqueId(), new InventorySnapshot(player));
        room.players.add(player.getUniqueId());
        playerRoom.put(player.getUniqueId(), room.id);
        autoAssignTeam(room, player);
        MapData data = maps.read();
        Location lobbySpawn = roomLoc(room, data.lobbySpawn, MapSide.LOBBY);
        if (lobbySpawn == null && room.lobbyWorld != null) lobbySpawn = room.lobbyWorld.getSpawnLocation().clone().add(0.5, 1.0, 0.5);
        player.teleport(lobbySpawn);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.getInventory().clear();
        clearExperience(player);
        ItemStack compass = items.actionModel(Material.PAPER, "§x§7§D§F§F§C§8[ 选择队伍 ]", List.of("§f- §a打开队伍选择菜单"), "open_team", Material.COMPASS);
        player.getInventory().setItem(0, compass);
        if (isAdmin(player)) {
            ItemStack force = items.actionModel(Material.PAPER, "§x§F§F§B§B§6§6[ 强制开始 ]", List.of("§f- §a管理员可直接启动当前房间", "§8- 只影响这个等待大厅"), "force_start", Material.REDSTONE_TORCH);
            player.getInventory().setItem(4, force);
        }
        ItemStack leave = items.actionModel(Material.PAPER, "§x§F§F§8§8§5§5[ 离开房间 ]", List.of("§f- §7离开当前房间"), "leave_room", Material.BARRIER);
        player.getInventory().setItem(8, leave);
        player.updateInventory();
        applyWaitingName(player, room.team(player.getUniqueId()));
        room.broadcast(prefix() + "§f" + player.getName() + " §x§7§D§F§F§C§8进入了房间。");
        playSound(player, "room.join", Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 1.2F);
    }

    private void autoAssignTeam(Room room, Player player) {
        int brick = room.count(Team.BRICK);
        int nether = room.count(Team.NETHER);
        Team team;
        if (brick <= nether) team = Team.BRICK;
        else team = Team.NETHER;
        room.teams.put(player.getUniqueId(), team);
    }

    private void selectTeam(Player player, Team team) {
        Optional<Room> optional = activeRoom(player);
        if (optional.isEmpty()) {
            msg(player, "§x§F§F§8§8§5§5你还没有进入房间。");
            return;
        }
        Room room = optional.get();
        if (room.status != Room.Status.WAITING) {
            msg(player, "§x§F§F§8§8§5§5开局后不能再切换队伍。");
            return;
        }
        Team old = room.team(player.getUniqueId());
        if (old == team) {
            openTeamMenu(player);
            return;
        }
        int other = room.count(team == Team.BRICK ? Team.NETHER : Team.BRICK);
        int target = room.count(team);
        if (target + 1 > other + 2) {
            msg(player, "§x§F§F§B§B§6§6这边人数太多了，先平衡一下。");
            return;
        }
        room.teams.put(player.getUniqueId(), team);
        applyWaitingName(player, team);
        msg(player, "§x§7§D§F§F§C§8你选择了 " + team.color + team.display + "§x§7§D§F§F§C§8。");
        openTeamMenu(player);
    }

    private Optional<Room> activeRoom(Player player) {
        String id = playerRoom.get(player.getUniqueId());
        return id == null ? Optional.empty() : Optional.ofNullable(rooms.get(id));
    }

    private void shout(Player player) {
        Optional<Room> optional = activeRoom(player);
        if (optional.isEmpty() || optional.get().status != Room.Status.RUNNING) {
            msg(player, "§x§F§F§8§8§5§5喊话只能在对局中使用。");
            return;
        }
        long now = System.currentTimeMillis();
        long next = shoutCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now < next) {
            player.sendActionBar(Text.c("§x§F§F§B§B§6§6喊话冷却 §f" + Math.max(1, (next - now + 999) / 1000) + "s"));
            return;
        }
        shoutCooldowns.put(player.getUniqueId(), now + 8000L);
        Room room = optional.get();
        Team team = room.team(player.getUniqueId());
        Location loc = player.getLocation();
        String text = prefix() + team.color + "队伍喊话 §8» §f" + player.getName() + " §7在 §f"
                + loc.getBlockX() + "§7, §f" + loc.getBlockY() + "§7, §f" + loc.getBlockZ()
                + " §7请求支援。";
        for (Player teammate : room.onlineTeam(team)) {
            teammate.sendMessage(Text.c(text));
            teammate.sendActionBar(Text.c(team.color + player.getName() + " §f正在请求支援"));
            playSound(teammate, "team.shout", Sound.ITEM_GOAT_HORN_SOUND_0, 0.75F, 1.15F);
        }
    }

    private void leave(Player player, boolean message) {
        Optional<Room> optional = activeRoom(player);
        if (optional.isEmpty()) {
            if (message) msg(player, "§x§F§F§B§B§6§6你不在房间里。");
            return;
        }
        Room room = optional.get();
        room.players.remove(player.getUniqueId());
        room.teams.remove(player.getUniqueId());
        room.spectators.remove(player.getUniqueId());
        room.finalDead.remove(player.getUniqueId());
        room.dyingSeconds.remove(player.getUniqueId());
        room.respawnSeconds.remove(player.getUniqueId());
        playerRoom.remove(player.getUniqueId());
        resetPlayer(player, room);
        InventorySnapshot snapshot = room.snapshots.remove(player.getUniqueId());
        if (snapshot != null) snapshot.restore(player);
        else sendLobbyFallback(player);
        clearName(player);
        if (message) msg(player, "§x§7§D§F§F§C§8你离开了房间。");
        if (!room.players.isEmpty()) room.broadcast(prefix() + "§f" + player.getName() + " §x§F§F§B§B§6§6离开了房间。");
        if (room.players.isEmpty()) endRoom(room, null, false);
        else if (room.status == Room.Status.RUNNING) checkWin(room);
    }

    private void sendLobbyFallback(Player player) {
        World world = Bukkit.getWorld("world");
        if (world != null) player.teleport(world.getSpawnLocation());
    }

    private void tick() {
        for (Room room : new ArrayList<>(rooms.values())) {
            if (room.status == Room.Status.WAITING) tickWaiting(room);
            else if (room.status == Room.Status.RUNNING) tickRunning(room);
        }
    }

    private void tickWaiting(Room room) {
        for (Player player : room.onlinePlayers()) {
            protectWaitingPlayer(room, player);
        }
        if (room.players.size() < cfg("min_players", 2)) {
            room.waitingLeft = cfg("waiting_seconds", 30);
            return;
        }
        if (Math.abs(room.count(Team.BRICK) - room.count(Team.NETHER)) > 2) {
            room.actionBar("§x§F§F§B§B§6§6等待队伍平衡 §7| §x§f§f§7§c§0§0" + room.count(Team.BRICK) + " §8/ §x§6§6§1§9§0§0" + room.count(Team.NETHER));
            return;
        }
        room.waitingLeft--;
        if (room.waitingLeft <= 5 || room.waitingLeft == 10 || room.waitingLeft == 20 || room.waitingLeft == 30) {
            room.actionBar("§x§7§D§F§F§C§8开局倒计时 §f" + room.waitingLeft + "s");
            playRoomSound(room, "countdown", Sound.BLOCK_NOTE_BLOCK_BELL, 0.8F, 1.0F + room.waitingLeft * 0.03F);
        }
        if (room.waitingLeft <= 0) startGame(room);
    }

    private void forceStart(Player player) {
        Optional<Room> optional = activeRoom(player);
        if (optional.isEmpty()) {
            msg(player, "§x§F§F§8§8§5§5你还没有进入等待大厅。");
            return;
        }
        if (!isAdmin(player)) {
            msg(player, "§x§F§F§8§8§5§5你没有权限强制开始。");
            return;
        }
        Room room = optional.get();
        if (room.status != Room.Status.WAITING) {
            msg(player, "§x§F§F§B§B§6§6这个房间已经开始了。");
            return;
        }
        if (room.players.size() < cfg("min_players", 2)) {
            msg(player, "§x§F§F§8§8§5§5至少需要 §f" + cfg("min_players", 2) + " §x§F§F§8§8§5§5名玩家才能开始。");
            return;
        }
        room.broadcast(prefix() + "§x§F§F§B§B§6§6管理员 §f" + player.getName() + " §x§F§F§B§B§6§6直接开始了本局。");
        startGame(room);
    }

    private void startGame(Room room) {
        MapData data = maps.read();
        if (room.players.size() < cfg("min_players", 2)) {
            room.broadcast(prefix() + "§x§F§F§8§8§5§5人数不足，至少需要 §f" + cfg("min_players", 2) + " §x§F§F§8§8§5§5名玩家。");
            room.waitingLeft = cfg("waiting_seconds", 30);
            return;
        }
        if (!data.ready()) {
            room.broadcast(prefix() + "§x§F§F§8§8§5§5地图还没有准备好，本房间已关闭。");
            endRoom(room, null, true);
            return;
        }
        for (Player player : room.onlinePlayers()) {
            player.closeInventory();
            resetStartState(player);
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 120, 0, false, false, false));
            player.sendActionBar(Text.c("§x§7§D§F§F§C§8地图准备中"));
        }
        if (!createRuntimeWorlds(room)) {
            room.broadcast(prefix() + "§x§F§F§8§8§5§5地图复制失败，本房间已关闭。");
            endRoom(room, null, false);
            return;
        }
        room.status = Room.Status.RUNNING;
        room.brickCoreHealth = room.brickCoreMax;
        Location brickSpawn = roomLoc(room, data.brickSpawn, MapSide.BRICK);
        Location netherSpawn = roomLoc(room, data.netherSpawn, MapSide.NETHER);
        Location brickCore = roomLoc(room, data.brickCore, MapSide.BRICK);
        if (brickCore != null) brickCore.getBlock().setType(Material.RED_GLAZED_TERRACOTTA, false);
        room.title("§x§f§f§7§c§0§0板砖守卫战", "§f核心已经出现");
        List<Player> netherPlayers = room.onlineTeam(Team.NETHER);
        if (netherPlayers.isEmpty() && !room.onlinePlayers().isEmpty()) {
            room.teams.put(room.onlinePlayers().getLast().getUniqueId(), Team.NETHER);
            netherPlayers = room.onlineTeam(Team.NETHER);
        }
        if (room.onlineTeam(Team.BRICK).isEmpty() && !room.onlinePlayers().isEmpty()) room.teams.put(room.onlinePlayers().getFirst().getUniqueId(), Team.BRICK);
        if (!netherPlayers.isEmpty()) {
            Player core = netherPlayers.get(ThreadLocalRandom.current().nextInt(netherPlayers.size()));
            room.corePlayer = core.getUniqueId();
            room.coreTransferLeft = 60;
            room.coreTransferUsed = false;
            room.coreStartLocation = netherSpawn == null ? core.getLocation().clone() : netherSpawn.clone();
        }
        for (Player player : room.onlinePlayers()) {
            player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
            resetStartState(player);
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 0, false, false, false));
            room.originalMaxHealth.put(player.getUniqueId(), maxHealth(player));
            Team team = room.team(player.getUniqueId());
            if (team == Team.BRICK) setupBrickPlayer(player, brickSpawn);
            else setupNetherPlayer(player, netherSpawn, Objects.equals(room.corePlayer, player.getUniqueId()));
            applyGameName(player, room, team);
        }
        spawnShops(room, data);
        refreshObsidianPools(room);
        createBossBars(room);
        room.broadcast(prefix() + "§x§7§D§F§F§C§8核心已生成，双方开始进攻。");
        if (room.corePlayer != null) {
            Player core = Bukkit.getPlayer(room.corePlayer);
            if (core != null) room.broadcast(prefix() + "§x§6§6§1§9§0§0下界核心玩家 §f" + core.getName() + " §x§6§6§1§9§0§0可以在 §f60s §x§6§6§1§9§0§0内转交核心。");
        }
        updateScoreboards(room);
    }

    private void resetStartState(Player player) {
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.setFireTicks(0);
        player.setFreezeTicks(0);
        player.setFallDistance(0.0F);
        player.setFoodLevel(20);
        player.setSaturation(8.0F);
        player.setExhaustion(0.0F);
        player.setHealth(Math.min(maxHealth(player), maxHealth(player)));
        player.setRemainingAir(player.getMaximumAir());
        clearExperience(player);
    }

    private void setupBrickPlayer(Player player, Location spawn) {
        if (spawn != null) player.teleport(spawn);
        equipLeather(player, Team.BRICK);
        player.getInventory().addItem(items.pickaxe(1));
        player.getInventory().addItem(items.gameItem(Material.COOKED_BEEF, 8, "§x§F§F§B§B§6§6战场便当", List.of("§f- §a战场补给"), Team.BRICK, "food"));
        giveShoutItem(player, Team.BRICK);
    }

    private void setupNetherPlayer(Player player, Location spawn, boolean core) {
        if (spawn != null) player.teleport(spawn);
        if (core) equipNetherCore(player);
        else equipLeather(player, Team.NETHER);
        player.getInventory().addItem(items.gameItem(Material.STONE_SWORD, 1, "§x§6§6§1§9§0§0下界砖短剑", List.of("§f- §a基础近战武器"), Team.NETHER, "weapon"));
        player.getInventory().addItem(items.gameItem(Material.ROTTEN_FLESH, 2, "§x§6§6§1§9§0§0濒死补给", List.of("§f- §a濒死状态下食用会重置时间"), Team.NETHER, "nether_food"));
        giveShoutItem(player, Team.NETHER);
    }

    private void giveShoutItem(Player player, Team team) {
        ItemStack shout = items.actionModel(Material.PAPER, team.color + "[ 战术喊话 ]", List.of("§f- §a右键向队友发送当前位置", "§f- §7每 §f8s §7可使用一次"), "team_shout", Material.GOAT_HORN);
        player.getInventory().setItem(8, shout);
    }

    private void equipLeather(Player player, Team team) {
        Color color = team == Team.BRICK ? Color.fromRGB(0xef4d00) : Color.fromRGB(0x661900);
        PlayerInventory inv = player.getInventory();
        inv.setHelmet(coloredLeather(Material.LEATHER_HELMET, team.color + team.display + " 皮帽", color));
        inv.setChestplate(team == Team.BRICK ? new ItemStack(Material.IRON_CHESTPLATE) : coloredLeather(Material.LEATHER_CHESTPLATE, team.color + team.display + " 皮衣", color));
        inv.setLeggings(coloredLeather(Material.LEATHER_LEGGINGS, team.color + team.display + " 皮裤", color));
        inv.setBoots(coloredLeather(Material.LEATHER_BOOTS, team.color + team.display + " 皮靴", color));
    }

    private ItemStack coloredLeather(Material material, String name, Color color) {
        ItemStack item = items.item(material, name, List.of());
        item.editMeta(meta -> {
            if (meta instanceof LeatherArmorMeta leather) {
                leather.setColor(color);
                leather.addItemFlags(ItemFlag.HIDE_DYE);
            }
        });
        return item;
    }

    private void equipNetherCore(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack head = items.item(Material.RED_GLAZED_TERRACOTTA, "§x§6§6§1§9§0§0[ 下界核心 ]", List.of());
        ItemStack chest = items.enchanted(Material.NETHERITE_CHESTPLATE, 1, "§x§6§6§1§9§0§0下界核心胸甲", List.of(), Map.of(Enchantment.PROTECTION, 1), Team.NETHER, "armor");
        ItemStack legs = items.enchanted(Material.IRON_LEGGINGS, 1, "§x§6§6§1§9§0§0下界核心护腿", List.of(), Map.of(Enchantment.PROTECTION, 1), Team.NETHER, "armor");
        ItemStack boots = items.enchanted(Material.IRON_BOOTS, 1, "§x§6§6§1§9§0§0下界核心战靴", List.of(), Map.of(Enchantment.PROTECTION, 1), Team.NETHER, "armor");
        inv.setHelmet(head);
        inv.setChestplate(chest);
        inv.setLeggings(legs);
        inv.setBoots(boots);
        setAttr(player, Attribute.MAX_HEALTH, 80.0D);
        player.setHealth(Math.min(80.0D, maxHealth(player)));
        setAttr(player, Attribute.SCALE, 2.0D);
        setAttr(player, Attribute.ATTACK_DAMAGE, 2.4D);
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false, false));
    }

    private void tickRunning(Room room) {
        room.gameLeft--;
        if (room.gameLeft <= 0) {
            endRoom(room, null, true);
            return;
        }
        if (room.coreTransferLeft > 0) {
            room.coreTransferLeft--;
            if (room.coreTransferLeft == 0 && room.corePlayer != null) {
                Player core = Bukkit.getPlayer(room.corePlayer);
                if (core != null) msg(core, "§x§6§6§1§9§0§0核心已经固定在你身上。");
            }
        }
        for (Player player : room.onlinePlayers()) {
            handleVoidReturn(room, player);
            if (room.isActive(player)) {
                enforceBoundary(room, player);
                repairPickaxeNearCore(room, player);
                applyHomeNightVision(room, player);
                if (Objects.equals(room.corePlayer, player.getUniqueId())) player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false, false));
            }
        }
        updateShopFacing(room);
        updateBossBars(room);
        tickObsidianPools(room);
        tickDying(room);
        tickRespawns(room);
        updateScoreboards(room);
    }

    private void applyHomeNightVision(Room room, Player player) {
        UUID uuid = player.getUniqueId();
        if (room.team(player.getUniqueId()) == Team.NETHER && player.getWorld() == room.netherWorld) {
            PotionEffect current = player.getPotionEffect(PotionEffectType.NIGHT_VISION);
            if (current == null || current.getDuration() <= 260) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 260, 0, true, false, false));
                homeNightVisionPlayers.add(uuid);
            }
            return;
        }
        if (homeNightVisionPlayers.remove(uuid)) {
            PotionEffect effect = player.getPotionEffect(PotionEffectType.NIGHT_VISION);
            if (effect == null || effect.getDuration() > 260 || effect.getAmplifier() != 0) return;
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }

    private void createBossBars(Room room) {
        removeBossBars(room);
        room.brickBossBar = Bukkit.createBossBar("§x§f§f§7§c§0§0板砖核心", BarColor.YELLOW, BarStyle.SEGMENTED_20);
        room.netherBossBar = Bukkit.createBossBar("§x§6§6§1§9§0§0核心玩家", BarColor.RED, BarStyle.SEGMENTED_20);
        updateBossBars(room);
    }

    private void removeBossBars(Room room) {
        if (room.brickBossBar != null) {
            room.brickBossBar.removeAll();
            room.brickBossBar = null;
        }
        if (room.netherBossBar != null) {
            room.netherBossBar.removeAll();
            room.netherBossBar = null;
        }
    }

    private void updateBossBars(Room room) {
        if (room.status != Room.Status.RUNNING) return;
        if (room.brickBossBar == null || room.netherBossBar == null) createBossBars(room);
        Player core = room.corePlayer == null ? null : Bukkit.getPlayer(room.corePlayer);
        double coreHealth = core == null ? 0.0D : Math.max(0.0D, core.getHealth());
        double coreMax = core == null ? 1.0D : Math.max(1.0D, maxHealth(core));
        for (Player player : room.onlinePlayers()) {
            BossBar own = room.team(player.getUniqueId()) == Team.BRICK ? room.brickBossBar : room.netherBossBar;
            BossBar other = room.team(player.getUniqueId()) == Team.BRICK ? room.netherBossBar : room.brickBossBar;
            if (own != null && !own.getPlayers().contains(player)) own.addPlayer(player);
            if (other != null) other.removePlayer(player);
        }
        if (room.brickBossBar != null) {
            room.brickBossBar.setProgress(Math.max(0.0D, Math.min(1.0D, room.brickCoreHealth / (double) Math.max(1, room.brickCoreMax))));
            if (room.brickBossNoticeTicks > 0 && room.brickBossNotice != null) {
                room.brickBossBar.setTitle(room.brickBossNotice);
                room.brickBossNoticeTicks--;
            } else {
                room.brickBossBar.setTitle("§x§f§f§7§c§0§0板砖核心 §f" + room.brickCoreHealth + "§7/§f" + room.brickCoreMax);
            }
        }
        if (room.netherBossBar != null) {
            room.netherBossBar.setProgress(Math.max(0.0D, Math.min(1.0D, coreHealth / coreMax)));
            if (room.netherBossNoticeTicks > 0 && room.netherBossNotice != null) {
                room.netherBossBar.setTitle(room.netherBossNotice);
                room.netherBossNoticeTicks--;
            } else {
                String name = core == null ? "离线" : core.getName();
                room.netherBossBar.setTitle("§x§6§6§1§9§0§0核心玩家 §f" + name + " §c" + (int) Math.ceil(coreHealth) + "§7/§c" + (int) Math.ceil(coreMax));
            }
        }
    }

    private String bossAttackerText(Player attacker) {
        if (attacker == null || attacker.isSneaking() || attacker.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            return "";
        }
        return " §7被 §f" + attacker.getName();
    }

    private void showBrickCoreNotice(Room room, Player attacker, int amount) {
        String attackerText = bossAttackerText(attacker);
        room.brickBossNotice = attackerText.isEmpty()
                ? "§x§f§f§7§c§0§0板砖核心 §7被挖掘 §c-" + amount
                : "§x§f§f§7§c§0§0板砖核心" + attackerText + " §7挖掘 §c-" + amount;
        room.brickBossNoticeTicks = 4;
        updateBossBars(room);
    }

    private void showNetherCoreNotice(Room room, Player attacker, int amount) {
        String attackerText = bossAttackerText(attacker);
        room.netherBossNotice = attackerText.isEmpty()
                ? "§x§6§6§1§9§0§0核心玩家 §7受到攻击 §c-" + amount
                : "§x§6§6§1§9§0§0核心玩家" + attackerText + " §7攻击 §c-" + amount;
        room.netherBossNoticeTicks = 4;
        updateBossBars(room);
    }

    private void protectWaitingPlayer(Room room, Player player) {
        player.setFireTicks(0);
        if (player.getHealth() < maxHealth(player)) player.setHealth(Math.min(maxHealth(player), maxHealth(player)));
        clearExperience(player);
        handleVoidReturn(room, player);
    }

    private void handleVoidReturn(Room room, Player player) {
        if (player == null || player.getWorld() == null) return;
        int voidY = cfg("void_return_y", -16);
        if (player.getLocation().getY() > voidY) return;
        Location target = returnSpawn(room, player);
        if (target == null) return;
        player.setFallDistance(0.0F);
        player.setFireTicks(0);
        player.teleport(target);
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 4, false, false, false));
        player.sendActionBar(Text.c("§x§7§D§F§F§C§8已回到出生点"));
        playSound(player, "void.return", Sound.ENTITY_ENDERMAN_TELEPORT, 0.75F, 1.35F);
    }

    private Location returnSpawn(Room room, Player player) {
        MapData data = maps.read();
        if (room.status == Room.Status.WAITING) {
            Location lobby = roomLoc(room, data.lobbySpawn, MapSide.LOBBY);
            if (lobby != null) return lobby;
            return room.lobbyWorld == null ? null : room.lobbyWorld.getSpawnLocation().clone().add(0.5, 1.0, 0.5);
        }
        Team team = room.team(player.getUniqueId());
        if (team == Team.BRICK) {
            return roomLoc(room, data.brickSpawn, MapSide.BRICK);
        }
        Location nether = roomLoc(room, data.netherSpawn, MapSide.NETHER);
        if (nether != null) return nether;
        Player core = room.corePlayer == null ? null : Bukkit.getPlayer(room.corePlayer);
        return core == null ? null : core.getLocation();
    }

    private void updateShopFacing(Room room) {
        for (Entity entity : room.shopEntities) {
            if (!entity.isValid() || entity.getWorld() == null) continue;
            Player nearest = null;
            double nearestDistance = 16 * 16;
            for (Player player : room.onlinePlayers()) {
                if (player.getWorld() != entity.getWorld()) continue;
                double distance = player.getLocation().distanceSquared(entity.getLocation());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = player;
                }
            }
            if (nearest == null) continue;
            Location loc = entity.getLocation();
            Location target = nearest.getLocation();
            double dx = target.getX() - loc.getX();
            double dz = target.getZ() - loc.getZ();
            loc.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
            loc.setPitch(0.0F);
            entity.teleport(loc);
        }
    }

    private void tickObsidianPools(Room room) {
        consumeObsidianDrops(room);
        refreshObsidianPools(room);
    }

    private void consumeObsidianDrops(Room room) {
        if (room.netherWorld == null || room.portalOpened) return;
        for (Item item : room.netherWorld.getEntitiesByClass(Item.class)) {
            ItemStack stack = item.getItemStack();
            if (stack.getType() != Material.OBSIDIAN || stack.getAmount() <= 0) continue;
            Block pool = cryingObsidianBelow(item.getLocation());
            if (pool == null) continue;
            int need = cfg("obsidian_required", 10);
            int add = Math.min(stack.getAmount(), Math.max(0, need - room.obsidianDeposited));
            if (add <= 0) continue;
            room.obsidianDeposited += add;
            if (stack.getAmount() <= add) item.remove();
            else {
                stack.setAmount(stack.getAmount() - add);
                item.setItemStack(stack);
            }
            Location center = pool.getLocation().add(0.5, 1.1, 0.5);
            room.broadcast(prefix() + "§x§6§6§1§9§0§0黑曜石池 §f" + room.obsidianDeposited + "§7/§f" + need);
            pool.getWorld().spawnParticle(Particle.PORTAL, center, 48, 0.45, 0.55, 0.45, 0.12);
            pool.getWorld().spawnParticle(Particle.REVERSE_PORTAL, center, 18, 0.3, 0.45, 0.3, 0.06);
            playWorldSound(center, "pool.deposit", Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.85F, 1.25F);
            if (room.obsidianDeposited >= need && !room.portalOpened) buildPortal(room);
        }
    }

    private Block cryingObsidianBelow(Location location) {
        if (location == null || location.getWorld() == null) return null;
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int top = location.getBlockY();
        for (int y = top; y >= top - 2; y--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block block = world.getBlockAt(x + dx, y, z + dz);
                    if (block.getType() != Material.CRYING_OBSIDIAN || !block.getRelative(BlockFace.UP).getType().isAir()) continue;
                    Location center = block.getLocation().add(0.5, 1.0, 0.5);
                    if (center.distanceSquared(location) <= 2.25D) return block;
                }
            }
        }
        return null;
    }

    private void refreshObsidianPools(Room room) {
        if (room.netherWorld == null) return;
        Set<String> seen = new HashSet<>();
        for (Player player : room.onlinePlayers()) {
            if (player.getWorld() != room.netherWorld) continue;
            int radius = Math.max(8, cfg("obsidian_pool_scan_radius", 24));
            Location base = player.getLocation();
            for (int x = base.getBlockX() - radius; x <= base.getBlockX() + radius; x++) {
                for (int y = base.getBlockY() - 8; y <= base.getBlockY() + 8; y++) {
                    for (int z = base.getBlockZ() - radius; z <= base.getBlockZ() + radius; z++) {
                        Block block = room.netherWorld.getBlockAt(x, y, z);
                        if (block.getType() != Material.CRYING_OBSIDIAN || !block.getRelative(BlockFace.UP).getType().isAir()) continue;
                        String key = blockKey(block.getLocation());
                        seen.add(key);
                        Entity display = room.obsidianPoolDisplays.get(key);
                        Location loc = block.getLocation().add(0.5, 1.18, 0.5);
                        if (display == null || !display.isValid()) {
                            ItemDisplay itemDisplay = room.netherWorld.spawn(loc, ItemDisplay.class, spawned -> {
                                spawned.setItemStack(new ItemStack(Material.OBSIDIAN));
                                spawned.setGlowing(true);
                                spawned.setPersistent(false);
                                spawned.setBrightness(new Display.Brightness(15, 15));
                                spawned.setViewRange(48.0F);
                            });
                            room.obsidianPoolDisplays.put(key, itemDisplay);
                        } else {
                            Location now = display.getLocation();
                            loc.setYaw(now.getYaw() + 18.0F);
                            loc.setPitch(0.0F);
                            display.teleport(loc);
                        }
                    }
                }
            }
        }
        Iterator<Map.Entry<String, Entity>> iterator = room.obsidianPoolDisplays.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Entity> entry = iterator.next();
            Entity display = entry.getValue();
            if (display == null || !display.isValid() || !seen.contains(entry.getKey())) {
                if (display != null) display.remove();
                iterator.remove();
            }
        }
    }

    private void enforceBoundary(Room room, Player player) {
        int radius = cfg("pseudo_boundary_radius", 1500);
        Team team = room.team(player.getUniqueId());
        MapData data = maps.read();
        Location center = team == Team.BRICK ? roomLoc(room, data.brickSpawn, MapSide.BRICK) : roomLoc(room, data.netherSpawn, MapSide.NETHER);
        if (center == null || player.getWorld() != center.getWorld()) return;
        if (player.getLocation().distanceSquared(center) > radius * radius) {
            Location back = player.getLocation().clone().subtract(player.getLocation().toVector().subtract(center.toVector()).normalize().multiply(3));
            player.teleport(back);
            playSound(player, "boundary", Sound.BLOCK_ANVIL_USE, 0.6F, 1.6F);
            player.sendActionBar(Text.c("§x§F§F§8§8§5§5前面是边界"));
        }
    }

    private void repairPickaxeNearCore(Room room, Player player) {
        if (room.team(player.getUniqueId()) != Team.BRICK) return;
        Location core = roomLoc(room, maps.read().brickCore, MapSide.BRICK);
        if (core == null || player.getWorld() != core.getWorld() || player.getLocation().distanceSquared(core) > 100) return;
        for (ItemStack item : player.getInventory().getContents()) {
            if (items.pickaxeLevel(item) > 0 && item.getItemMeta() instanceof Damageable damageable && damageable.hasDamage()) {
                damageable.setDamage(Math.max(0, damageable.getDamage() - 3));
                item.setItemMeta(damageable);
            }
        }
    }

    private void spawnShops(Room room, MapData data) {
        for (Point point : data.brickTraders) spawnShop(room, point, MapSide.BRICK, Team.BRICK);
        for (Point point : data.netherTraders) spawnShop(room, point, MapSide.NETHER, Team.NETHER);
    }

    private void spawnShop(Room room, Point point, MapSide side, Team team) {
        Location loc = roomLoc(room, point, side);
        if (loc == null || loc.getWorld() == null) return;
        loc.setYaw(0.0F);
        loc.setPitch(0.0F);
        LivingEntity entity;
        if (team == Team.BRICK) {
            entity = loc.getWorld().spawn(loc, Villager.class, villager -> {
                villager.customName(Text.c("§x§f§f§7§c§0§0板砖商人"));
                villager.setCustomNameVisible(true);
                villager.setProfession(Villager.Profession.MASON);
                villager.setVillagerType(Villager.Type.SAVANNA);
                villager.setAI(false);
                villager.setInvulnerable(false);
                villager.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "shop_brick");
            });
        } else {
            entity = loc.getWorld().spawn(loc, Piglin.class, piglin -> {
                piglin.customName(Text.c("§x§6§6§1§9§0§0下界猪灵商人"));
                piglin.setCustomNameVisible(true);
                piglin.setImmuneToZombification(true);
                piglin.setAI(false);
                piglin.setInvulnerable(false);
                piglin.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "shop_nether");
            });
        }
        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(80.0D);
        entity.setHealth(Math.min(80.0D, maxHealth == null ? 80.0D : maxHealth.getValue()));
        room.shopEntities.add(entity);
        room.shopHealth.put(entity.getUniqueId(), 80.0D);
    }

    private void placeMines(Room room, MapData data) {
        data.brickMines.forEach(p -> {
            Location loc = roomLoc(room, p, MapSide.BRICK);
            if (loc != null) loc.getBlock().setType(Material.BRICKS, false);
        });
        data.netherMines.forEach(p -> {
            Location loc = roomLoc(room, p, MapSide.NETHER);
            if (loc != null) loc.getBlock().setType(Material.NETHER_BRICKS, false);
        });
    }

    private void openShop(Player player) {
        openShop(player, "quick");
    }

    private void openShop(Player player, String category) {
        Optional<Room> optional = activeRoom(player);
        if (optional.isEmpty() || optional.get().status != Room.Status.RUNNING || !optional.get().canFight(player)) {
            msg(player, "§x§F§F§8§8§5§5商店只会在开局后打开。");
            return;
        }
        Room room = optional.get();
        Team team = room.team(player.getUniqueId());
        category = normalizeShopCategory(category);
        Inventory inv = Bukkit.createInventory(player, 54, Text.c("§0" + (team == Team.BRICK ? "板砖商店" : "下界砖商店")));
        ShopCategory[] categories = shopCategories(team);
        for (int i = 0; i < categories.length; i++) {
            putShopCategory(inv, i, categories[i], category);
        }
        ItemStack line = items.item(Material.GRAY_STAINED_GLASS_PANE, "§8", List.of());
        for (int slot = 9; slot <= 17; slot++) inv.setItem(slot, line);
        inv.setItem(13, items.item(team.icon, team.color + "[ " + shopCategoryName(category) + " ]", List.of("§f- §a点击上方分类切换", "§f- §7下方背包可以正常整理物品")));
        List<Product> products = shopProducts(team, category);
        int[] slots = bedwarsShopSlots(products.size());
        for (int i = 0; i < Math.min(slots.length, products.size()); i++) {
            Product p = products.get(i);
            inv.setItem(slots[i], productButton(player, team, p, category, i));
        }
        if (products.isEmpty()) inv.setItem(31, items.item(Material.PAPER, "§x§B§B§B§B§B§B[ 这个分类没有商品 ]", List.of("§f- §7换个分类看看")));
        player.openInventory(inv);

        openMenus.put(player.getUniqueId(), new OpenMenu(MenuType.SHOP, team.id + ":" + category));
        menuSound(player, team == Team.BRICK ? Sound.ENTITY_VILLAGER_TRADE : Sound.ENTITY_PIGLIN_AMBIENT, 0.9F, 1.2F);
    }

    private String normalizeShopCategory(String category) {
        return switch ((category == null ? "quick" : category).toLowerCase(Locale.ROOT)) {
            case "blocks", "weapons", "armor", "tools", "food", "special" -> category.toLowerCase(Locale.ROOT);
            default -> "quick";
        };
    }

    private ShopCategory[] shopCategories(Team team) {
        Material blocks = team == Team.BRICK ? Material.BRICKS : Material.NETHER_BRICKS;
        Material food = team == Team.BRICK ? Material.COOKED_BEEF : Material.ROTTEN_FLESH;
        return new ShopCategory[]{
                new ShopCategory("quick", Material.NETHER_STAR, "快捷购买", "常用物资"),
                new ShopCategory("blocks", blocks, "方块", "推进防守"),
                new ShopCategory("weapons", Material.IRON_SWORD, "武器", "近战远程"),
                new ShopCategory("armor", Material.IRON_CHESTPLATE, "护甲", "直接升级"),
                new ShopCategory("tools", Material.IRON_PICKAXE, "工具", "稿子功能"),
                new ShopCategory("food", food, "补给", "食物恢复"),
                new ShopCategory("special", Material.BEACON, "特殊", "战术道具")
        };
    }

    private String shopCategoryName(String category) {
        return switch (normalizeShopCategory(category)) {
            case "blocks" -> "方块";
            case "weapons" -> "武器";
            case "armor" -> "护甲";
            case "tools" -> "工具";
            case "food" -> "补给";
            case "special" -> "特殊";
            default -> "快捷购买";
        };
    }

    private int[] bedwarsShopSlots(int count) {
        int capped = Math.max(1, Math.min(21, count));
        List<Integer> slots = new ArrayList<>();
        int rows = Math.max(1, (int) Math.ceil(capped / 7.0D));
        int startRow = 2;
        int left = capped;
        for (int row = 0; row < rows && left > 0; row++) {
            int rowCount = Math.min(7, left);
            int startCol = 4 - rowCount / 2;
            if (rowCount % 2 == 0) startCol++;
            for (int col = startCol; col < startCol + rowCount; col++) {
                slots.add((startRow + row) * 9 + col);
            }
            left -= rowCount;
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private void putShopCategory(Inventory inv, int slot, ShopCategory category, String selected) {
        boolean active = category.id().equalsIgnoreCase(selected);
        List<String> lore = new ArrayList<>();
        lore.add("§f- §a" + category.desc());
        lore.add(active ? "§8[ 当前分类 ]" : "§8[ 点击切换 ]");
        inv.setItem(slot, items.action(category.icon(),
                (active ? "§x§7§D§F§F§C§8" : "§f") + "[ " + category.name() + " ]",
                lore,
                "shopcat_" + category.id()));
    }

    private ItemStack productButton(Player player, Team team, Product p, String category, int index) {
        List<String> lore = new ArrayList<>(p.lore);
        Cost cost = costFor(player, team, p);
        boolean enough = countItem(player, cost.currency) >= cost.price;
        boolean killsEnough = activeRoom(player).map(room -> room.kills.getOrDefault(player.getUniqueId(), 0)).orElse(0) >= cost.killPrice;
        if (p.icon == Material.IRON_PICKAXE) {
            int current = currentPickaxeLevel(player);
            int next = Math.min(5, Math.max(1, current + 1));
            lore.add("§8[ 品质 ] §f" + current + " §7→ §f" + next);
        } else if (isSword(p.icon)) {
            lore.add("§8[ 升级 ] §f当前 " + currentSwordRank(player) + " §7→ §f" + swordRank(p.icon));
        } else if (isArmorPiece(p.icon)) {
            lore.add("§8[ 升级 ] §f当前 " + currentArmorRank(player, p.icon) + " §7→ §f" + armorRank(p.icon));
        }
        lore.add((enough ? "§8[ 价格 ] §f" : "§8[ 价格 ] §x§F§F§8§8§5§5") + cost.price + " §7" + cost.currencyName);
        if (cost.killPrice > 0) lore.add((killsEnough ? "§8[ 条件 ] §f" : "§8[ 条件 ] §x§F§F§8§8§5§5") + cost.killPrice + " §7击杀数");
        lore.add(enough && killsEnough ? "§f- §a点击购买" : "§f- §x§F§F§8§8§5§5材料不足");
        return items.action(p.icon, p.name, lore, "buy_" + category + "_" + index);
    }

    private List<Product> shopProducts(Team team, String category) {
        List<Product> all = products(team);
        if ("quick".equalsIgnoreCase(category)) {
            return all.stream().filter(p -> switch (p.icon) {
                case BRICKS, NETHER_BRICKS, IRON_SWORD, STONE_SWORD, IRON_PICKAXE, MACE, COOKED_BEEF, ROTTEN_FLESH, GOLDEN_APPLE, ARROW, POTION -> true;
                default -> false;
            }).limit(12).toList();
        }
        return all.stream().filter(p -> shopCategory(p).equalsIgnoreCase(category)).toList();
    }

    private String shopCategory(Product p) {
        return switch (p.icon) {
            case BRICKS, BRICK_STAIRS, BRICK_SLAB, NETHER_BRICKS, BLACKSTONE, OBSIDIAN -> "blocks";
            case IRON_SWORD, STONE_SWORD, NETHERITE_SWORD, IRON_AXE, CROSSBOW, ARROW, IRON_SPEAR, GOLDEN_SPEAR, MACE, WIND_CHARGE -> "weapons";
            case IRON_CHESTPLATE, DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, SHIELD -> "armor";
            case IRON_PICKAXE, STONE_PICKAXE, DIAMOND_PICKAXE -> "tools";
            case COOKED_BEEF, ROTTEN_FLESH, GOLDEN_APPLE -> "food";
            case POTION -> "special";
            default -> "special";
        };
    }

    private List<Product> products(Team team) {
        if (team == Team.BRICK) return List.of(
                new Product(Material.BRICKS, 32, "§x§f§f§7§c§0§0板砖包", List.of("§f- §a搭建防线"), 16, Material.BRICK, "板砖"),
                new Product(Material.BRICK_STAIRS, 16, "§x§f§f§7§c§0§0斜坡砖", List.of("§f- §a调整进攻角度"), 12, Material.BRICK, "板砖"),
                new Product(Material.BRICK_SLAB, 24, "§x§f§f§7§c§0§0轻量砖板", List.of("§f- §a补小缺口"), 10, Material.BRICK, "板砖"),
                new Product(Material.IRON_SWORD, 1, "§x§f§f§7§c§0§0板砖短剑", List.of("§f- §a稳定的近战选择"), 24, Material.BRICK, "板砖"),
                new Product(Material.IRON_AXE, 1, "§x§f§f§7§c§0§0断线斧", List.of("§f- §a爆发更高"), 28, Material.BRICK, "板砖"),
                new Product(Material.CROSSBOW, 1, "§x§f§f§7§c§0§0防御机弩", List.of("§f- §a远程压制"), 20, Material.BRICK, "板砖"),
                new Product(Material.ARROW, 16, "§x§f§f§7§c§0§0流量箭袋", List.of("§f- §a给弩使用"), 8, Material.BRICK, "板砖"),
                new Product(Material.IRON_SPEAR, 1, "§x§f§f§7§c§0§0战矛", List.of("§f- §a追击核心玩家"), 2, Material.DIAMOND, "钻石"),
                new Product(Material.MACE, 1, "§x§f§f§7§c§0§0裂砖重锤", List.of("§f- §a高处落击时爆发更高"), 5, Material.DIAMOND, "钻石", 2),
                new Product(Material.SHIELD, 1, "§x§f§f§7§c§0§0防御工单盾", List.of("§f- §a挡住一轮爆发"), 18, Material.BRICK, "板砖"),
                new Product(Material.IRON_CHESTPLATE, 1, "§x§f§f§7§c§0§0标准板甲", List.of("§f- §a提升生存"), 36, Material.BRICK, "板砖"),
                new Product(Material.DIAMOND_CHESTPLATE, 1, "§x§f§f§7§c§0§0加固胸甲", List.of("§f- §a很贵，但很硬"), 6, Material.DIAMOND, "钻石"),
                new Product(Material.COOKED_BEEF, 8, "§x§F§F§B§B§6§6热饭", List.of("§f- §a快速补给"), 10, Material.BRICK, "板砖"),
                new Product(Material.GOLDEN_APPLE, 1, "§x§F§F§B§B§6§6应急苹果", List.of("§f- §a关键时刻吃"), 2, Material.DIAMOND, "钻石"),
                new Product(Material.BEACON, 1, "§x§7§D§F§F§C§8战场信标", List.of("§f- §a放置后给附近队友发光提示"), 8, Material.DIAMOND, "钻石"),
                new Product(Material.IRON_PICKAXE, 1, "§x§e§f§4§d§0§0狐稿升级券", List.of("§f- §a升级手上的狐稿", "§f- §7最高可升到下界合金稿"), 3, Material.DIAMOND, "钻石", 1)
        );
        return List.of(
                new Product(Material.NETHER_BRICKS, 32, "§x§6§6§1§9§0§0下界砖包", List.of("§f- §a搭建突袭路线"), 16, Material.NETHER_BRICK, "下界砖"),
                new Product(Material.BLACKSTONE, 24, "§x§6§6§1§9§0§0黑石工单", List.of("§f- §a压迫式推进"), 12, Material.NETHER_BRICK, "下界砖"),
                new Product(Material.OBSIDIAN, 2, "§x§6§6§1§9§0§0黑曜石缓存", List.of("§f- §a用于补传送门进度"), 3, Material.GOLD_NUGGET, "金粒矿"),
                new Product(Material.STONE_SWORD, 1, "§x§6§6§1§9§0§0断剑", List.of("§f- §a廉价武器"), 12, Material.NETHER_BRICK, "下界砖"),
                new Product(Material.IRON_PICKAXE, 1, "§x§6§6§1§9§0§0下界开采稿", List.of("§f- §a更快挖开下界砖矿点", "§f- §7空手也能挖，这个只是更顺手"), 20, Material.NETHER_BRICK, "下界砖"),
                new Product(Material.IRON_AXE, 1, "§x§6§6§1§9§0§0宕机重启斧", List.of("§f- §a近战破口"), 2, Material.GOLD_NUGGET, "金粒矿"),
                new Product(Material.CROSSBOW, 1, "§x§6§6§1§9§0§0节点弩", List.of("§f- §a远程骚扰"), 24, Material.NETHER_BRICK, "下界砖"),
                new Product(Material.ARROW, 16, "§x§6§6§1§9§0§0带宽箭袋", List.of("§f- §a远程弹药"), 8, Material.NETHER_BRICK, "下界砖"),
                new Product(Material.GOLDEN_SPEAR, 1, "§x§6§6§1§9§0§0长矛", List.of("§f- §a适合突进"), 3, Material.GOLD_NUGGET, "金粒矿"),
                new Product(Material.MACE, 1, "§x§6§6§1§9§0§0熔岩重锤", List.of("§f- §a压制板砖队防线"), 4, Material.GOLD_NUGGET, "金粒矿", 2),
                new Product(Material.WIND_CHARGE, 4, "§x§7§D§F§F§C§8风弹", List.of("§f- §a打开距离"), 2, Material.GOLD_NUGGET, "金粒矿"),
                new Product(Material.IRON_CHESTPLATE, 1, "§x§6§6§1§9§0§0下界工单甲", List.of("§f- §a提高容错"), 28, Material.NETHER_BRICK, "下界砖"),
                new Product(Material.DIAMOND_LEGGINGS, 1, "§x§6§6§1§9§0§0无法计算的护腿", List.of("§f- §a随机纹饰款"), 5, Material.GOLD_NUGGET, "金粒矿"),
                new Product(Material.ROTTEN_FLESH, 3, "§x§6§6§1§9§0§0濒死补给", List.of("§f- §a濒死状态重置时间"), 10, Material.NETHER_BRICK, "下界砖"),
                new Product(Material.GOLDEN_APPLE, 1, "§x§F§F§B§B§6§6缓存苹果", List.of("§f- §a突围前吃"), 3, Material.GOLD_NUGGET, "金粒矿"),
                new Product(Material.POTION, 1, "§x§6§6§1§9§0§0雾隐药水", List.of("§f- §a短时间隐藏进攻痕迹", "§f- §7隐身或蹲下时挖核心不会显示名字"), 4, Material.GOLD_NUGGET, "金粒矿"),
                new Product(Material.BEACON, 1, "§x§7§D§F§F§C§8天气狐信标", List.of("§f- §a标记战线"), 6, Material.GOLD_NUGGET, "金粒矿"),
                new Product(Material.NETHERITE_SWORD, 1, "§x§6§6§1§9§0§0核心保护协议", List.of("§f- §a守护核心玩家"), 9, Material.GOLD_NUGGET, "金粒矿", 3)
        );
    }

    private void buy(Player player, String category, int index) {
        Optional<Room> optional = activeRoom(player);
        if (optional.isEmpty()) return;
        Team team = optional.get().team(player.getUniqueId());
        List<Product> products = shopProducts(team, category);
        if (index < 0 || index >= products.size()) return;
        Product p = products.get(index);
        if (p.icon == Material.IRON_PICKAXE && currentPickaxeLevel(player) >= 5) {
            msg(player, "§x§F§F§B§B§6§6你的矿稿已经是顶级品质。");
            playSound(player, "buy.fail", Sound.BLOCK_ANVIL_USE, 0.7F, 1.6F);
            return;
        }
        if (isSword(p.icon) && currentSwordRank(player) >= swordRank(p.icon)) {
            msg(player, "§x§F§F§B§B§6§6你的剑已经不低于这个品质。");
            playSound(player, "buy.fail", Sound.BLOCK_ANVIL_USE, 0.7F, 1.6F);
            return;
        }
        if (isArmorPiece(p.icon) && currentArmorRank(player, p.icon) >= armorRank(p.icon)) {
            msg(player, "§x§F§F§B§B§6§6这件装备已经不低于这个品质。");
            playSound(player, "buy.fail", Sound.BLOCK_ANVIL_USE, 0.7F, 1.6F);
            return;
        }
        Cost cost = costFor(player, team, p);
        int kills = optional.get().kills.getOrDefault(player.getUniqueId(), 0);
        if (kills < cost.killPrice) {
            msg(player, "§x§F§F§8§8§5§5击杀数不足，需要 §f" + cost.killPrice + " §x§F§F§8§8§5§5击杀。");
            playSound(player, "buy.fail", Sound.BLOCK_ANVIL_USE, 0.7F, 1.8F);
            return;
        }
        if (!take(player, cost.currency, cost.price)) {
            msg(player, "§x§F§F§8§8§5§5材料不足，需要 §f" + cost.price + " §x§F§F§8§8§5§5" + cost.currencyName + "。");
            playSound(player, "buy.fail", Sound.BLOCK_ANVIL_USE, 0.7F, 1.8F);
            return;
        }
        if (p.icon == Material.IRON_PICKAXE) {
            upgradePickaxe(player);
        } else if (isSword(p.icon)) {
            upgradeSword(player, productStack(p, team));
        } else if (isArmorPiece(p.icon)) {
            upgradeArmor(player, productStack(p, team));
        } else {
            ItemStack out = productStack(p, team);
            Map<Integer, ItemStack> left = player.getInventory().addItem(out);
            left.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        msg(player, "§x§7§D§F§F§C§8已购买 §f" + strip(p.name) + "§x§7§D§F§F§C§8。");
        playSound(player, p.icon == Material.IRON_PICKAXE || isSword(p.icon) || isArmorPiece(p.icon) ? "buy.upgrade" : "buy.success", Sound.ENTITY_ITEM_PICKUP, 0.9F, 1.35F);
        openShop(player, category);
    }

    private Cost costFor(Player player, Team team, Product product) {
        if (product.icon != Material.IRON_PICKAXE) {
            return new Cost(product.price, product.currency, product.currencyName, product.killPrice);
        }
        int current = currentPickaxeLevel(player);
        if (current >= 5) return new Cost(0, product.currency, product.currencyName, product.killPrice);
        int next = Math.max(1, Math.min(5, current + 1));
        if (team == Team.NETHER) {
            return switch (next) {
                case 1 -> new Cost(20, Material.NETHER_BRICK, "下界砖", 0);
                case 2 -> new Cost(36, Material.NETHER_BRICK, "下界砖", 0);
                case 3 -> new Cost(3, Material.GOLD_NUGGET, "金粒矿", 1);
                case 4 -> new Cost(5, Material.GOLD_NUGGET, "金粒矿", 2);
                default -> new Cost(8, Material.GOLD_NUGGET, "金粒矿", 3);
            };
        }
        return switch (next) {
            case 1 -> new Cost(16, Material.BRICK, "板砖", 0);
            case 2 -> new Cost(24, Material.BRICK, "板砖", 0);
            case 3 -> new Cost(48, Material.BRICK, "板砖", 1);
            case 4 -> new Cost(3, Material.DIAMOND, "钻石", 2);
            default -> new Cost(6, Material.DIAMOND, "钻石", 3);
        };
    }

    private ItemStack productStack(Product p, Team team) {
        ItemStack stack = items.gameItem(p.icon, p.amount, p.name, p.lore, team, "product");
        if (p.name.contains("无法计算") && stack.getItemMeta() instanceof org.bukkit.inventory.meta.ArmorMeta armor) {
            armor.setTrim(new ArmorTrim(team == Team.BRICK ? TrimMaterial.DIAMOND : TrimMaterial.GOLD, TrimPattern.BOLT));
            stack.setItemMeta(armor);
        }
        if (p.icon == Material.POTION && stack.getItemMeta() instanceof PotionMeta potion) {
            potion.setBasePotionType(PotionType.INVISIBILITY);
            potion.setColor(Color.fromRGB(0x2d1738));
            stack.setItemMeta(potion);
        }
        return stack;
    }

    private void upgradePickaxe(Player player) {
        Team team = activeRoom(player).map(room -> room.team(player.getUniqueId())).orElse(Team.BRICK);
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            int level = items.pickaxeLevel(item);
            if (level > 0) {
                inv.setItem(i, items.pickaxe(Math.min(5, level + 1), team));
                return;
            }
        }
        inv.addItem(items.pickaxe(1, team));
    }

    private int currentPickaxeLevel(Player player) {
        int best = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            best = Math.max(best, items.pickaxeLevel(item));
        }
        return best;
    }

    private boolean isSword(Material material) {
        return material == Material.WOODEN_SWORD || material == Material.STONE_SWORD || material == Material.IRON_SWORD
                || material == Material.DIAMOND_SWORD || material == Material.NETHERITE_SWORD || material == Material.GOLDEN_SWORD;
    }

    private int swordRank(Material material) {
        return switch (material) {
            case WOODEN_SWORD -> 1;
            case GOLDEN_SWORD, STONE_SWORD -> 2;
            case IRON_SWORD -> 3;
            case DIAMOND_SWORD -> 4;
            case NETHERITE_SWORD -> 5;
            default -> 0;
        };
    }

    private int currentSwordRank(Player player) {
        int best = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) best = Math.max(best, swordRank(item.getType()));
        }
        return best;
    }

    private void upgradeSword(Player player, ItemStack sword) {
        PlayerInventory inv = player.getInventory();
        int targetRank = swordRank(sword.getType());
        int slot = -1;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || !isSword(item.getType())) continue;
            if (slot < 0 || swordRank(item.getType()) < swordRank(inv.getItem(slot).getType())) slot = i;
        }
        if (slot >= 0 && swordRank(inv.getItem(slot).getType()) < targetRank) {
            inv.setItem(slot, sword);
        } else {
            inv.addItem(sword).values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
    }

    private boolean isArmorPiece(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    private int armorRank(Material material) {
        String name = material.name();
        if (name.startsWith("LEATHER_")) return 1;
        if (name.startsWith("CHAINMAIL_")) return 2;
        if (name.startsWith("IRON_")) return 3;
        if (name.startsWith("DIAMOND_")) return 4;
        if (name.startsWith("NETHERITE_")) return 5;
        if (name.startsWith("GOLDEN_")) return 2;
        return 0;
    }

    private int currentArmorRank(Player player, Material target) {
        ItemStack current = armorSlot(player.getInventory(), target);
        return current == null ? 0 : armorRank(current.getType());
    }

    private ItemStack armorSlot(PlayerInventory inv, Material material) {
        String name = material.name();
        if (name.endsWith("_HELMET")) return inv.getHelmet();
        if (name.endsWith("_CHESTPLATE")) return inv.getChestplate();
        if (name.endsWith("_LEGGINGS")) return inv.getLeggings();
        if (name.endsWith("_BOOTS")) return inv.getBoots();
        return null;
    }

    private void upgradeArmor(Player player, ItemStack armor) {
        PlayerInventory inv = player.getInventory();
        String name = armor.getType().name();
        if (name.endsWith("_HELMET")) inv.setHelmet(armor);
        else if (name.endsWith("_CHESTPLATE")) inv.setChestplate(armor);
        else if (name.endsWith("_LEGGINGS")) inv.setLeggings(armor);
        else if (name.endsWith("_BOOTS")) inv.setBoots(armor);
    }

    private boolean take(Player player, Material material, int amount) {
        int has = 0;
        for (ItemStack item : player.getInventory().getContents()) if (item != null && item.getType() == material) has += item.getAmount();
        if (has < amount) return false;
        int left = amount;
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize() && left > 0; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() != material) continue;
            int remove = Math.min(left, item.getAmount());
            item.setAmount(item.getAmount() - remove);
            if (item.getAmount() <= 0) inv.setItem(i, null);
            left -= remove;
        }
        return true;
    }

    private int countItem(Player player, Material material) {
        int amount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) amount += item.getAmount();
        }
        return amount;
    }

    private String strip(String legacy) {
        return legacy.replaceAll("§x(§[0-9A-Fa-f]){6}", "").replaceAll("§[0-9A-Fa-fK-Ok-oRr]", "");
    }

    private record ShopCategory(String id, Material icon, String name, String desc) {
    }

    private record Product(Material icon, int amount, String name, List<String> lore, int price, Material currency, String currencyName, int killPrice) {
        Product(Material icon, int amount, String name, List<String> lore, int price, Material currency, String currencyName) {
            this(icon, amount, name, lore, price, currency, currencyName, 0);
        }
    }

    private record Cost(int price, Material currency, String currencyName, int killPrice) {
    }

    private void enterEdit(Player player, MapSide side) {
        if (!editSessions.containsKey(player.getUniqueId())) editSessions.put(player.getUniqueId(), new EditSession(side, new InventorySnapshot(player)));
        else editSessions.put(player.getUniqueId(), new EditSession(side, editSessions.get(player.getUniqueId()).snapshot()));
        World world = maps.world(side);
        if (world == null) {
            maps.createEditWorlds();
            world = maps.world(side);
        }
        if (world != null) player.teleport(world.getSpawnLocation().clone().add(0.5, 1, 0.5));
        player.setGameMode(GameMode.CREATIVE);
        player.getInventory().clear();
        player.setAllowFlight(true);
        player.setFlying(true);
        maps.refreshPreview(player, side);
        msg(player, "§x§7§D§F§F§C§8已进入 §f" + side.display + "§x§7§D§F§F§C§8，工具包需要用命令领取。");
        playSound(player, "edit.enter", Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 1.3F);
    }

    private void exitEdit(Player player, boolean message) {
        EditSession session = editSessions.remove(player.getUniqueId());
        maps.clearPreview(player);
        toolkitSnapshots.remove(player.getUniqueId());
        if (session != null) session.snapshot().restore(player);
        if (message) msg(player, "§x§7§D§F§F§C§8已退出地图编辑。");
    }

    private void giveToolkit(Player player) {
        EditSession session = editSessions.get(player.getUniqueId());
        if (session == null) {
            msg(player, "§x§F§F§8§8§5§5请先进入地图编辑。");
            return;
        }
        ItemStack toolkit = items.tool(Material.PAPER, "§x§7§D§F§F§C§8[ 地图工具包 ]", List.of("§f- §a右键打开编辑器"), "toolkit_open", Material.BUNDLE);
        int slot = player.getInventory().getHeldItemSlot();
        ItemStack current = player.getInventory().getItem(slot);
        if (current == null || current.getType().isAir()) {
            player.getInventory().setItem(slot, toolkit);
        } else {
            player.getInventory().addItem(toolkit).values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
        player.updateInventory();
        msg(player, "§x§7§D§F§F§C§8已领取地图工具包。");
    }

    private void openToolkitRoot(Player player) {
        EditSession session = editSessions.get(player.getUniqueId());
        if (session == null) return;
        if (!toolkitSnapshots.containsKey(player.getUniqueId())) {
            toolkitSnapshots.put(player.getUniqueId(), new InventorySnapshot(player));
        }
        player.getInventory().clear();
        player.getInventory().setItem(1, items.tool(Material.PAPER, "§x§7§D§F§F§C§8[ 出生点 ]", List.of("§f- §a设置玩家进入位置"), "toolkit_page_spawn", Material.RESPAWN_ANCHOR));
        player.getInventory().setItem(3, items.tool(Material.PAPER, "§x§7§D§F§F§C§8[ 商人 ]", List.of("§f- §a放置交易实体"), "toolkit_page_trader", session.side() == MapSide.NETHER ? Material.PIGLIN_SPAWN_EGG : Material.VILLAGER_SPAWN_EGG));
        player.getInventory().setItem(5, items.tool(Material.PAPER, "§x§7§D§F§F§C§8[ 目标点 ]", List.of("§f- §a设置核心与门位置", "§f- §7矿物与黑曜石池由地图方块自动识别"), "toolkit_page_object", session.side() == MapSide.NETHER ? Material.CRYING_OBSIDIAN : Material.BRICKS));
        player.getInventory().setItem(7, items.tool(Material.PAPER, "§x§7§D§F§F§C§8[ 检查 ]", List.of("§f- §a检查地图元素"), "toolkit_page_check", Material.SPYGLASS));
        player.getInventory().setItem(8, items.tool(Material.PAPER, "§x§F§F§8§8§5§5[ 关闭 ]", List.of("§f- §7恢复打开前物品栏"), "toolkit_close", Material.BARRIER));
        player.updateInventory();
        playSound(player, "toolkit.open", Sound.ITEM_BOOK_PAGE_TURN, 0.65F, 1.25F);
    }

    private void openToolkitPage(Player player, String page) {
        EditSession session = editSessions.get(player.getUniqueId());
        if (session == null) return;
        MapSide side = session.side();
        player.getInventory().clear();
        if (page.equals("spawn")) {
            if (side == MapSide.LOBBY) {
                player.getInventory().setItem(4, items.tool(Material.PAPER, "§x§7§D§F§F§C§8[ 大厅出生点 ]", List.of("§f- §a右键保存当前位置"), "set_lobby_spawn", Material.COMPASS));
            } else if (side == MapSide.BRICK) {
                player.getInventory().setItem(4, items.tool(Material.PAPER, "§x§F§F§8§8§2§2[ 板砖出生点 ]", List.of("§f- §a右键保存当前位置"), "set_brick_spawn", Material.RESPAWN_ANCHOR));
            } else {
                player.getInventory().setItem(4, items.tool(Material.PAPER, "§x§6§6§1§9§0§0[ 下界出生点 ]", List.of("§f- §a右键保存当前位置"), "set_nether_spawn", Material.RESPAWN_ANCHOR));
            }
        } else if (page.equals("trader")) {
            if (side == MapSide.BRICK) {
                player.getInventory().setItem(4, items.tool(Material.PAPER, "§x§F§F§8§8§2§2[ 板砖商人 ]", List.of("§f- §a右键添加当前位置"), "add_brick_trader", Material.VILLAGER_SPAWN_EGG));
            } else if (side == MapSide.NETHER) {
                player.getInventory().setItem(4, items.tool(Material.PAPER, "§x§6§6§1§9§0§0[ 下界商人 ]", List.of("§f- §a右键添加当前位置"), "add_nether_trader", Material.PIGLIN_SPAWN_EGG));
            } else {
                player.getInventory().setItem(4, items.tool(Material.PAPER, "§x§B§B§B§B§B§B[ 大厅无商人 ]", List.of("§f- §7返回选择其他分类"), "toolkit_back", Material.GRAY_DYE));
            }
        } else if (page.equals("object")) {
            if (side == MapSide.BRICK) {
                player.getInventory().setItem(2, items.tool(Material.PAPER, "§x§F§F§8§8§2§2[ 板砖核心 ]", List.of("§f- §a右键方块保存"), "set_brick_core", Material.RED_GLAZED_TERRACOTTA));
                player.getInventory().setItem(4, items.tool(Material.PAPER, "§x§F§F§8§8§2§2[ 板砖门 ]", List.of("§f- §a右键保存门位置"), "set_brick_portal", Material.OBSIDIAN));
                player.getInventory().setItem(6, items.tool(Material.PAPER, "§x§F§F§8§8§5§5[ 清空商人 ]", List.of("§f- §c清空已添加的商人"), "clear_brick_points", Material.REDSTONE_BLOCK));
            } else if (side == MapSide.NETHER) {
                player.getInventory().setItem(2, items.tool(Material.PAPER, "§x§6§6§1§9§0§0[ 下界门 ]", List.of("§f- §a右键保存门位置"), "set_nether_portal", Material.CRYING_OBSIDIAN));
                player.getInventory().setItem(4, items.tool(Material.PAPER, "§x§6§6§1§9§0§0[ 黑曜石池提示 ]", List.of("§f- §a直接在地图放哭泣的黑曜石", "§f- §7不需要手动设置"), "map_check", Material.CRYING_OBSIDIAN));
                player.getInventory().setItem(6, items.tool(Material.PAPER, "§x§F§F§8§8§5§5[ 清空商人 ]", List.of("§f- §c清空已添加的商人"), "clear_nether_points", Material.REDSTONE_BLOCK));
            } else {
                player.getInventory().setItem(4, items.tool(Material.PAPER, "§x§B§B§B§B§B§B[ 大厅无目标点 ]", List.of("§f- §7返回选择其他分类"), "toolkit_back", Material.GRAY_DYE));
            }
        } else if (page.equals("check")) {
            player.getInventory().setItem(4, items.tool(Material.PAPER, "§x§7§D§F§F§C§8[ 检查元素 ]", List.of("§f- §a右键检查地图元素"), "map_check", Material.SPYGLASS));
        }
        player.getInventory().setItem(8, items.tool(Material.PAPER, "§x§F§F§B§B§6§6[ 返回 ]", List.of("§f- §7返回工具包分类"), "toolkit_back", Material.ARROW));
        player.updateInventory();
        playSound(player, "toolkit.page", Sound.ITEM_BOOK_PAGE_TURN, 0.65F, 1.45F);
    }

    private void restoreToolkit(Player player) {
        InventorySnapshot snapshot = toolkitSnapshots.remove(player.getUniqueId());
        if (snapshot != null) snapshot.restoreInventoryOnly(player);
        EditSession session = editSessions.get(player.getUniqueId());
        if (session != null) maps.refreshPreview(player, session.side());
    }

    private void handleEditAction(Player player, String action) {
        EditSession session = editSessions.get(player.getUniqueId());
        if (session == null) {
            msg(player, "§x§F§F§8§8§5§5请先进入地图编辑。");
            return;
        }
        if (action.equals("toolkit_open")) {
            openToolkitRoot(player);
            return;
        }
        if (action.startsWith("toolkit_page_")) {
            openToolkitPage(player, action.substring("toolkit_page_".length()));
            return;
        }
        if (action.equals("toolkit_back")) {
            openToolkitRoot(player);
            return;
        }
        if (action.equals("toolkit_close") || action.equals("toolkit_restore")) {
            restoreToolkit(player);
            msg(player, "§x§7§D§F§F§C§8工具包已收起。");
            return;
        }
        if (action.equals("give_toolkit")) {
            player.closeInventory();
            giveToolkit(player);
            return;
        }
        if (action.equals("map_check")) {
            checkMap(player);
            feedback(player, true);
            return;
        }
        if (action.equals("back_map")) {
            openMapMenu(player);
            return;
        }
        Location loc = player.getLocation().clone();
        Block target = player.getTargetBlockExact(6);
        if (action.contains("core") || action.contains("portal")) {
            if (target != null) loc = target.getLocation();
        }
        switch (action) {
            case "set_lobby_spawn" -> writePoint(player, "lobby.spawn", loc, "大厅出生点", session.side());
            case "set_brick_spawn" -> writePoint(player, "brick.spawn", player.getLocation(), "板砖出生点", session.side());
            case "set_nether_spawn" -> writePoint(player, "nether.spawn", player.getLocation(), "下界出生点", session.side());
            case "set_brick_core" -> writePoint(player, "brick.core", loc, "板砖核心", session.side());
            case "set_brick_portal" -> writePoint(player, "brick.portal", loc, "板砖门位置", session.side());
            case "set_nether_portal" -> writePoint(player, "nether.portal", loc, "下界门位置", session.side());
            case "set_obsidian_pool" -> writePoint(player, "nether.obsidian_pool", loc, "黑曜石池", session.side());
            case "add_brick_trader" -> addPoint(player, "brick.traders", player.getLocation(), "板砖商人", session.side());
            case "add_nether_trader" -> addPoint(player, "nether.traders", player.getLocation(), "下界商人", session.side());
            case "clear_brick_points" -> {
                maps.clear("brick.traders");
                maps.refreshPreview(player, session.side());
                msg(player, "§x§F§F§B§B§6§6已清空板砖商人。");
                feedback(player, false);
            }
            case "clear_nether_points" -> {
                maps.clear("nether.traders");
                maps.refreshPreview(player, session.side());
                msg(player, "§x§F§F§B§B§6§6已清空下界商人。");
                feedback(player, false);
            }
        }
    }

    private void writePoint(Player player, String path, Location loc, String name, MapSide side) {
        Location save = normalizeMapPoint(path, loc);
        maps.write(path, save);
        maps.refreshPreview(player, side);
        msg(player, "§x§7§D§F§F§C§8已设置 §f" + name + " §7{§f" + save.getBlockX() + "§7, §f" + save.getBlockY() + "§7, §f" + save.getBlockZ() + "§7}");
        feedback(player, true);
    }

    private void addPoint(Player player, String path, Location loc, String name, MapSide side) {
        Location save = normalizeMapPoint(path, loc);
        maps.append(path, save);
        maps.refreshPreview(player, side);
        msg(player, "§x§7§D§F§F§C§8已添加 §f" + name + " §7{§f" + save.getBlockX() + "§7, §f" + save.getBlockY() + "§7, §f" + save.getBlockZ() + "§7}");
        feedback(player, true);
    }

    private Location normalizeMapPoint(String path, Location loc) {
        Location save = loc.clone();
        if (path.endsWith(".spawn")) {
            return save;
        }
        save.setYaw(0.0F);
        save.setPitch(0.0F);
        return save;
    }

    private void feedback(Player player, boolean good) {
        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(good ? Particle.HAPPY_VILLAGER : Particle.WITCH, loc, 18, 0.45, 0.45, 0.45, 0.02);
        player.getWorld().spawnParticle(Particle.END_ROD, loc, 8, 0.3, 0.3, 0.3, 0.01);
        playSound(player, good ? "edit.success" : "edit.fail", good ? Sound.BLOCK_NOTE_BLOCK_CHIME : Sound.BLOCK_ANVIL_LAND, 0.65F, good ? 1.35F : 1.4F);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        OpenMenu open = openMenus.get(player.getUniqueId());
        if (open == null) {
            if (shouldCancelGameInventoryClick(player, event)) event.setCancelled(true);
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        boolean clickedTop = event.getRawSlot() >= 0 && event.getRawSlot() < topSize;
        if (open.type() == MenuType.SHOP && !clickedTop) {
            if (event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY || shouldCancelGameInventoryClick(player, event)) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
        if (!clickedTop) return;
        ItemStack clicked = event.getCurrentItem();
        String action = items.actionOf(clicked);
        if (action == null) return;
        long now = System.currentTimeMillis();
        long last = clickCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 180L) return;
        clickCooldowns.put(player.getUniqueId(), now);
        playSound(player, "menu.click", Sound.UI_BUTTON_CLICK, 0.5F, 1.7F);
        if (action.equals("close")) {
            player.closeInventory();
            return;
        }
        if (action.equals("back_main")) {
            openMainMenu(player);
            return;
        }
        if (action.equals("main_quick")) {
            player.closeInventory();
            quickJoin(player);
            return;
        }
        if (action.equals("main_create")) {
            player.closeInventory();
            createRoom(player);
            return;
        }
        if (action.equals("main_rooms")) { openRooms(player); return; }
        if (action.equals("main_rank")) { openRank(player); return; }
        if (action.startsWith("join_")) {
            player.closeInventory();
            Room room = rooms.get(action.substring(5));
            if (room != null) joinRoom(player, room);
            return;
        }
        if (action.startsWith("team_")) {
            selectTeam(player, Team.fromAction(action));
            return;
        }
        if (action.startsWith("map_edit_")) {
            player.closeInventory();
            enterEdit(player, MapSide.parse(action.substring(9)));
            return;
        }
        if (action.equals("back_map")) { openMapMenu(player); return; }
        if (action.equals("map_check")) { checkMap(player); return; }
        if (action.startsWith("set_") || action.startsWith("add_") || action.startsWith("clear_") || action.equals("give_toolkit")) {
            handleEditAction(player, action);
            return;
        }
        if (action.startsWith("buy_")) {
            String payload = action.substring(4);
            int split = payload.lastIndexOf('_');
            if (split > 0) {
                buy(player, payload.substring(0, split), Integer.parseInt(payload.substring(split + 1)));
            }
            return;
        }
        if (action.startsWith("shopcat_")) {
            openShop(player, action.substring(8));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTask(this, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !isTrackedMenu(player.getOpenInventory().getTopInventory())) {
                openMenus.remove(uuid);
            }
        });
    }

    private boolean isTrackedMenu(Inventory inventory) {
        if (inventory == null) return false;
        for (ItemStack item : inventory.getContents()) {
            if (items.actionOf(item) != null) return true;
        }
        return false;
    }

    private boolean shouldCancelGameInventoryClick(Player player, InventoryClickEvent event) {
        Optional<Room> optional = activeRoom(player);
        if (optional.isEmpty()) return false;
        Room room = optional.get();
        if (event.getSlotType() == InventoryType.SlotType.RESULT || event.getSlotType() == InventoryType.SlotType.CRAFTING) return true;
        if (room.status != Room.Status.RUNNING) return false;
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) return true;
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (isArmorStack(current) || isArmorStack(cursor)) return true;
        return switch (event.getAction()) {
            case MOVE_TO_OTHER_INVENTORY, HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> isArmorStack(current) || isArmorStack(cursor);
            default -> false;
        };
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Optional<Room> optional = activeRoom(player);
        if (optional.isEmpty()) return;
        OpenMenu open = openMenus.get(player.getUniqueId());
        if (open != null && open.type() == MenuType.SHOP) {
            int topSize = event.getView().getTopInventory().getSize();
            if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize) || isArmorStack(event.getOldCursor()) || isArmorStack(event.getCursor())) {
                event.setCancelled(true);
            }
            return;
        }
        if (isArmorStack(event.getOldCursor()) || isArmorStack(event.getCursor())) {
            event.setCancelled(true);
            return;
        }
        if (optional.get().status == Room.Status.RUNNING && event.getRawSlots().stream().anyMatch(slot -> slot >= 5 && slot <= 8)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player && activeRoom(player).isPresent()) {
            event.setCancelled(true);
            player.sendActionBar(Text.c("§x§F§F§B§B§6§6本局内不能合成物品"));
            playSound(player, "message.fail", Sound.BLOCK_ANVIL_USE, 0.45F, 1.6F);
        }
    }

    private boolean isArmorStack(ItemStack item) {
        return item != null && item.getType() != Material.AIR && isArmorPiece(item.getType());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) return;
        Optional<Room> active = activeRoom(player);
        if (active.isPresent() && active.get().status == Room.Status.RUNNING && !active.get().canFight(player)) {
            event.setCancelled(true);
            return;
        }
        ItemStack item = event.getItem();
        String action = items.actionOf(item);
        if (action == null) return;
        if (action.equals("open_team")) {
            event.setCancelled(true);
            openTeamMenu(player);
            return;
        }
        if (action.equals("leave_room")) {
            event.setCancelled(true);
            leave(player, true);
            return;
        }
        if (action.equals("force_start")) {
            event.setCancelled(true);
            forceStart(player);
            return;
        }
        if (action.equals("team_shout")) {
            event.setCancelled(true);
            shout(player);
            return;
        }
        if (items.isTool(item) && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
            handleEditAction(player, action);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Optional<Room> optional = activeRoom(player);
        if (optional.isEmpty()) return;
        Room room = optional.get();
        if (room.status == Room.Status.WAITING) {
            event.setCancelled(true);
            return;
        }
        if (room.status != Room.Status.RUNNING) return;
        if (!room.canFight(player)) {
            event.setCancelled(true);
            return;
        }
        ItemStack dropped = event.getItemDrop().getItemStack();
        int level = items.pickaxeLevel(dropped);
        if (level <= 0) return;
        if (level == 1) {
            event.setCancelled(true);
            player.sendActionBar(Text.c("§x§F§F§B§B§6§6一级狐稿不能丢弃"));
            playSound(player, "message.fail", Sound.BLOCK_ANVIL_USE, 0.45F, 1.7F);
            return;
        }
        Team team = room.team(player.getUniqueId());
        Bukkit.getScheduler().runTask(this, () -> ensureStarterPickaxe(player, team));
    }

    private void ensureStarterPickaxe(Player player, Team team) {
        if (player == null || !player.isOnline()) return;
        int best = currentPickaxeLevel(player);
        if (best <= 0) {
            player.getInventory().addItem(items.pickaxe(1, team == null ? Team.BRICK : team)).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
            player.sendActionBar(Text.c("§x§7§D§F§F§C§8已补发一级狐稿"));
            playSound(player, "pickaxe.refill", Sound.ENTITY_ITEM_PICKUP, 0.6F, 1.45F);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (editSessions.containsKey(player.getUniqueId())) exitEdit(player, false);
        leave(player, false);
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && activeRoom(player).isPresent()) event.setCancelled(false);
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        if (activeRoom(event.getPlayer()).isPresent() || editSessions.containsKey(event.getPlayer().getUniqueId())) {
            event.message(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTeamMessage(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage();
        String lower = raw.toLowerCase(Locale.ROOT);
        if (!lower.equals("/teammsg") && !lower.startsWith("/teammsg ") && !lower.equals("/tm") && !lower.startsWith("/tm ")) return;
        Player player = event.getPlayer();
        Optional<Room> optional = activeRoom(player);
        if (optional.isEmpty()) return;
        event.setCancelled(true);
        String message = raw.startsWith("/tm") ? raw.substring(Math.min(raw.length(), 3)).trim() : raw.substring(Math.min(raw.length(), 8)).trim();
        if (message.isBlank()) {
            msg(player, "§x§F§F§B§B§6§6用法：§f/teammsg <内容>");
            return;
        }
        sendTeamMessage(optional.get(), player, message);
    }

    private void sendTeamMessage(Room room, Player player, String message) {
        Team team = room.team(player.getUniqueId());
        if (team == null) {
            msg(player, "§x§F§F§8§8§5§5你还没有队伍。");
            return;
        }
        String clean = message.replace('§', ' ');
        String text = team.color + "[队伍] §f" + player.getName() + " §7» §f" + clean;
        for (Player teammate : room.onlineTeam(team)) {
            teammate.sendMessage(Text.c(text));
            playSound(teammate, "message.info", Sound.BLOCK_NOTE_BLOCK_CHIME, 0.35F, 1.45F);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player viewer = event.getPlayer();
        for (UUID uuid : respawnGhosts) {
            Player ghost = Bukkit.getPlayer(uuid);
            if (ghost != null && !ghost.getUniqueId().equals(viewer.getUniqueId())) viewer.hidePlayer(this, ghost);
        }
    }

    @EventHandler
    public void onExperience(PlayerExpChangeEvent event) {
        if (activeRoom(event.getPlayer()).isPresent()) {
            event.setAmount(0);
            clearExperience(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Optional<Room> optional = activeRoom(player);
        if (optional.isEmpty()) return;
        Room room = optional.get();
        event.deathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setNewExp(0);
        event.setNewLevel(0);
        event.setNewTotalExp(0);
        event.setKeepInventory(true);
        event.setKeepLevel(false);
        if (room.status != Room.Status.RUNNING) return;
        Player killer = validKiller(room, player, player.getKiller());
        if (killer == null && player.getLastDamageCause() instanceof EntityDamageByEntityEvent damage) {
            killer = validKiller(room, player, attacker(damage.getDamager()));
        }
        Player finalKiller = killer;
        Bukkit.getScheduler().runTask(this, () -> {
            if (!player.isOnline() || room.status != Room.Status.RUNNING) return;
            try {
                player.spigot().respawn();
            } catch (Throwable ignored) {
            }
            handlePlayerDownOnce(room, player, finalKiller);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Optional<Room> optional = activeRoom(player);
        if (optional.isEmpty()) return;
        Room room = optional.get();
        if (room.status != Room.Status.RUNNING) {
            event.setCancelled(true);
            return;
        }
        Team team = room.team(player.getUniqueId());
        Block block = event.getBlock();
        String blockKey = blockKey(block.getLocation());
        if (room.placedBlocks.remove(blockKey)) {
            event.setCancelled(true);
            event.setDropItems(false);
            ItemStack saved = room.placedBlockItems.remove(blockKey);
            BlockData original = block.getBlockData();
            block.setType(Material.AIR, false);
            if (saved != null && saved.getType() != Material.AIR) block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.35, 0.5), saved.clone());
            else block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.35, 0.5), new ItemStack(original.getMaterial(), 1));
            block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 18, 0.35, 0.35, 0.35, original);
            playWorldSound(block.getLocation(), "block.custom_break", Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.65F, 1.15F);
            return;
        }
        if (block.getType() == Material.RED_GLAZED_TERRACOTTA && sameBlock(block.getLocation(), roomLoc(room, maps.read().brickCore, MapSide.BRICK))) {
            if (team != Team.NETHER) {
                event.setCancelled(true);
                return;
            }
            event.setCancelled(true);
            damageBrickCore(room, player, 35);
            return;
        }
        if (block.getType() == Material.BRICKS || block.getType() == Material.NETHER_BRICKS || block.getType() == Material.OBSIDIAN) {
            handleMineBreak(room, player, block);
            event.setCancelled(true);
            event.setDropItems(false);
            return;
        }
        if (block.getType() == Material.CRYING_OBSIDIAN) {
            event.setCancelled(true);
            player.sendActionBar(Text.c("§x§6§6§1§9§0§0哭泣的黑曜石会自动接收黑曜石。"));
            playSound(player, "pool.protected", Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.55F, 1.2F);
            return;
        }
        if (block.getType().isAir()) return;
        event.setCancelled(true);
        event.setDropItems(false);
        BlockData original = block.getBlockData();
        block.setType(Material.AIR, false);
        block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 16, 0.35, 0.35, 0.35, original);
        playWorldSound(block.getLocation(), "block.normal_break", Sound.BLOCK_STONE_BREAK, 0.55F, 1.1F);
        restoreBlockLater(block.getLocation(), original, cfg("block_restore_seconds", 20));
    }

    private void handleMineBreak(Room room, Player player, Block block) {
        Material type = block.getType();
        BlockData original = block.getBlockData();
        Location loc = block.getLocation().clone();
        Location center = loc.clone().add(0.5, 0.5, 0.5);
        block.setType(Material.AIR, false);
        if (type == Material.BRICKS) {
            giveOrDrop(player, new ItemStack(Material.BRICK, 2));
            if (ThreadLocalRandom.current().nextInt(100) < 8) giveOrDrop(player, new ItemStack(Material.DIAMOND, 1));
            loc.getWorld().spawnParticle(Particle.BLOCK, center, 28, 0.38, 0.38, 0.38, original);
            loc.getWorld().spawnParticle(Particle.CRIT, center, 10, 0.35, 0.35, 0.35, 0.04);
            playSound(player, "mine.brick", Sound.BLOCK_GRINDSTONE_USE, 0.68F, 1.35F);
        } else if (type == Material.NETHER_BRICKS) {
            giveOrDrop(player, new ItemStack(Material.NETHER_BRICK, 2));
            if (ThreadLocalRandom.current().nextInt(100) < 18) giveOrDrop(player, new ItemStack(Material.GOLD_NUGGET, 1));
            loc.getWorld().spawnParticle(Particle.BLOCK, center, 28, 0.38, 0.38, 0.38, original);
            loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center, 8, 0.35, 0.35, 0.35, 0.02);
            playSound(player, "mine.nether_brick", Sound.BLOCK_NETHER_BRICKS_BREAK, 0.78F, 1.18F);
        } else {
            giveOrDrop(player, new ItemStack(Material.OBSIDIAN, 1));
            loc.getWorld().spawnParticle(Particle.BLOCK, center, 30, 0.35, 0.35, 0.35, original);
            loc.getWorld().spawnParticle(Particle.PORTAL, center, 16, 0.35, 0.35, 0.35, 0.08);
            playSound(player, "mine.obsidian", Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.72F, 1.45F);
        }
        restoreBlockLater(loc, original, cfg("block_restore_seconds", 20));
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        Map<Integer, ItemStack> left = player.getInventory().addItem(stack);
        left.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        playCollectSound(player, stack.getType());
    }

    private void playCollectSound(Player player, Material material) {
        switch (material) {
            case DIAMOND -> playSound(player, "mine.collect.diamond", Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.72F, 1.62F);
            case GOLD_NUGGET, GOLD_INGOT -> playSound(player, "mine.collect.gold", Sound.BLOCK_NOTE_BLOCK_BELL, 0.58F, 1.48F);
            case BRICK -> playSound(player, "mine.collect.brick", Sound.BLOCK_DECORATED_POT_HIT, 0.45F, 1.25F);
            case NETHER_BRICK -> playSound(player, "mine.collect.nether_brick", Sound.BLOCK_NETHER_BRICKS_HIT, 0.50F, 0.95F);
            case OBSIDIAN -> playSound(player, "mine.collect.obsidian", Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.55F, 1.38F);
            default -> playSound(player, "mine.collect.default", Sound.ENTITY_ITEM_PICKUP, 0.35F, 1.5F);
        }
    }

    private void restoreBlockLater(Location location, BlockData data, int seconds) {
        Location loc = location.clone();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            World world = loc.getWorld();
            if (world == null) return;
            Block block = world.getBlockAt(loc);
            if (block.getType().isAir()) {
                block.setBlockData(data, false);
                world.spawnParticle(Particle.CLOUD, loc.clone().add(0.5, 0.5, 0.5), 8, 0.25, 0.25, 0.25, 0.01);
            }
        }, Math.max(1, seconds) * 20L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Optional<Room> optional = activeRoom(player);
        if (optional.isEmpty()) return;
        Room room = optional.get();
        if (room.status != Room.Status.RUNNING) {
            event.setCancelled(true);
            return;
        }
        if (event.getBlock().getRelative(BlockFace.DOWN).getType() == Material.CRYING_OBSIDIAN) {
            event.setCancelled(true);
            player.sendActionBar(Text.c("§x§6§6§1§9§0§0哭泣的黑曜石上方要留给黑曜石掉落物。"));
            playSound(player, "pool.protected", Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.55F, 1.2F);
            return;
        }
        if (!isPlacedAllowed(event.getBlock().getType())) {
            event.setCancelled(true);
        } else {
            String key = blockKey(event.getBlock().getLocation());
            room.placedBlocks.add(key);
            ItemStack placed = event.getItemInHand().clone();
            placed.setAmount(1);
            room.placedBlockItems.put(key, placed);
            playSound(player, "block.place", Sound.BLOCK_WOOD_PLACE, 0.45F, 1.25F);
        }
    }

    private String blockKey(Location loc) {
        World world = loc.getWorld();
        return (world == null ? "" : world.getName()) + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private boolean isPlacedAllowed(Material material) {
        return material == Material.BRICKS || material == Material.BRICK_STAIRS || material == Material.BRICK_SLAB
                || material == Material.NETHER_BRICKS || material == Material.BLACKSTONE || material == Material.OBSIDIAN
                || material == Material.BEACON;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (isShopEntity(event.getEntity())) {
            handleShopDamage(event);
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) return;
        Optional<Room> optional = activeRoom(victim);
        if (optional.isPresent() && optional.get().status == Room.Status.WAITING) {
            event.setCancelled(true);
            victim.setFireTicks(0);
            victim.setFallDistance(0.0F);
            return;
        }
        Player attacker = attacker(event.getDamager());
        if (attacker == null) return;
        if (optional.isEmpty() || activeRoom(attacker).isEmpty() || optional.get() != activeRoom(attacker).get()) return;
        Room room = optional.get();
        if (room.status != Room.Status.RUNNING) {
            event.setCancelled(true);
            return;
        }
        Team vt = room.team(victim.getUniqueId());
        Team at = room.team(attacker.getUniqueId());
        if (!room.canFight(victim) || !room.canFight(attacker)) {
            event.setCancelled(true);
            return;
        }
        if (vt == at) {
            event.setCancelled(true);
            return;
        }
        double damage = event.getDamage();
        if (at == Team.BRICK && Objects.equals(room.corePlayer, victim.getUniqueId()) && items.pickaxeLevel(attacker.getInventory().getItemInMainHand()) > 0) damage *= 3.0D;
        if (room.dyingSeconds.containsKey(attacker.getUniqueId())) damage *= 1.4D;
        if (Objects.equals(room.corePlayer, attacker.getUniqueId())) damage *= 1.2D;
        event.setDamage(damage);
        if (Objects.equals(room.corePlayer, victim.getUniqueId())) {
            showNetherCoreNotice(room, attacker, Math.max(1, (int) Math.ceil(Math.min(victim.getHealth(), event.getFinalDamage()))));
        }
        victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, false, false, false));
        if (event.getFinalDamage() >= victim.getHealth()) {
            event.setCancelled(true);
            handlePlayerDownOnce(room, victim, attacker);
        }
    }

    private boolean isShopEntity(Entity entity) {
        String type = entity.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        return "shop_brick".equals(type) || "shop_nether".equals(type);
    }

    private void handleShopDamage(EntityDamageByEntityEvent event) {
        event.setCancelled(true);
        if (!(event.getEntity() instanceof LivingEntity shop)) return;
        Player attacker = attacker(event.getDamager());
        if (attacker == null) return;
        Optional<Room> optional = activeRoom(attacker);
        if (optional.isEmpty()) return;
        Room room = optional.get();
        String type = shop.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        Team shopTeam = "shop_brick".equals(type) ? Team.BRICK : Team.NETHER;
        Team attackerTeam = room.team(attacker.getUniqueId());
        String shopName = shopTeam == Team.BRICK ? "村民" : "猪灵";
        if (attackerTeam == shopTeam) {
            attacker.sendActionBar(Text.c("§x§F§F§B§B§6§6这是自己家的" + shopName + "，不能攻击。"));
            playSound(attacker, "shop.own_hit", Sound.ENTITY_VILLAGER_NO, 0.65F, 1.25F);
            return;
        }
        double before = room.shopHealth.getOrDefault(shop.getUniqueId(), 80.0D);
        double damage = Math.max(1.0D, event.getDamage());
        double after = Math.max(0.0D, before - damage);
        room.shopHealth.put(shop.getUniqueId(), after);
        AttributeInstance max = shop.getAttribute(Attribute.MAX_HEALTH);
        if (max != null) max.setBaseValue(80.0D);
        if (after > 0.0D) {
            shop.setHealth(Math.max(1.0D, Math.min(after, max == null ? 80.0D : max.getValue())));
        }
        attacker.sendActionBar(Text.c("§f" + shopName + " §x§F§F§8§8§5§5" + (int) Math.ceil(after) + " 血 §7-" + (int) Math.ceil(damage)));
        shop.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, shop.getLocation().add(0, 1.2, 0), 6, 0.25, 0.25, 0.25, 0.02);
        playSound(attacker, "shop.enemy_hit", Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.55F, 1.35F);
        if (after <= 0.0D) {
            room.shopEntities.remove(shop);
            room.shopHealth.remove(shop.getUniqueId());
            shop.getWorld().spawnParticle(Particle.CLOUD, shop.getLocation().add(0, 1.0, 0), 24, 0.45, 0.6, 0.45, 0.05);
            playWorldSound(shop.getLocation(), shopTeam == Team.BRICK ? "shop.death.brick" : "shop.death.nether", shopTeam == Team.BRICK ? Sound.ENTITY_VILLAGER_DEATH : Sound.ENTITY_PIGLIN_DEATH, 0.85F, 1.0F);
            shop.remove();
            room.broadcast(prefix() + "§f" + attacker.getName() + " §x§F§F§8§8§5§5击杀了 " + shopTeam.color + shopName + "§x§F§F§8§8§5§5。");
        }
    }

    private Player attacker(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFatal(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Optional<Room> optional = activeRoom(player);
        if (optional.isEmpty()) return;
        Room room = optional.get();
        if (room.status == Room.Status.WAITING) {
            event.setCancelled(true);
            player.setFireTicks(0);
            player.setFallDistance(0.0F);
            if (player.getHealth() <= 2.0D) player.setHealth(Math.min(maxHealth(player), maxHealth(player)));
            handleVoidReturn(room, player);
            return;
        }
        if (room.status != Room.Status.RUNNING) return;
        if (player.getLocation().getY() <= cfg("void_return_y", -16)) {
            event.setCancelled(true);
            handleVoidReturn(room, player);
            return;
        }
        if (event.getFinalDamage() < player.getHealth()) return;
        event.setCancelled(true);
        Player killer = event instanceof EntityDamageByEntityEvent entityDamage ? attacker(entityDamage.getDamager()) : null;
        handlePlayerDownOnce(room, player, validKiller(room, player, killer));
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        String type = event.getEntity().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        if ("shop_brick".equals(type) || "shop_nether".equals(type)) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        Optional<Room> optional = activeRoom(player);
        if (optional.isEmpty()) return;
        Room room = optional.get();
        if (!room.dyingSeconds.containsKey(player.getUniqueId())) return;
        if (event.getItem().getType() == Material.ROTTEN_FLESH && "nether_food".equals(items.typeOf(event.getItem()))) {
            room.dyingSeconds.put(player.getUniqueId(), 90);
            msg(player, "§x§6§6§1§9§0§0濒死补给稳住了你的气息。");
            playSound(player, "food.consume", Sound.ITEM_TOTEM_USE, 0.9F, 1.45F);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDropToPool(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Optional<Room> optional = activeRoom(player);
        if (optional.isEmpty()) return;
        Room room = optional.get();
        if (room.status != Room.Status.RUNNING) return;
        ItemStack stack = event.getItemDrop().getItemStack();
        if (stack.getType() != Material.OBSIDIAN) return;
        Bukkit.getScheduler().runTaskLater(this, () -> consumeObsidianDrops(room), 8L);
        Bukkit.getScheduler().runTaskLater(this, () -> consumeObsidianDrops(room), 20L);
    }

    private void damageBrickCore(Room room, Player attacker, int amount) {
        int actual = Math.min(amount, Math.max(0, room.brickCoreHealth));
        room.brickCoreHealth = Math.max(0, room.brickCoreHealth - amount);
        showBrickCoreNotice(room, attacker, actual);
        room.broadcast(prefix() + "§x§f§f§7§c§0§0板砖核心 §f" + room.brickCoreHealth + "§7/§f" + room.brickCoreMax);
        Location core = roomLoc(room, maps.read().brickCore, MapSide.BRICK);
        if (core != null) {
            core.getWorld().spawnParticle(Particle.FLAME, core.clone().add(0.5, 0.8, 0.5), 20, 0.4, 0.4, 0.4, 0.03);
            playWorldSound(core, "core.damage", Sound.BLOCK_ANVIL_USE, 0.7F, 1.2F);
        }
        if (room.brickCoreHealth <= 0) {
            rememberFinalKillSound(room, attacker);
            endRoom(room, Team.NETHER, true);
        }
    }

    private void buildPortal(Room room) {
        room.portalOpened = true;
        MapData data = maps.read();
        Location brickPortal = roomLoc(room, data.brickPortal, MapSide.BRICK);
        Location netherPortal = roomLoc(room, data.netherPortal, MapSide.NETHER);
        if (brickPortal == null && netherPortal == null) return;
        room.broadcast(prefix() + "§x§6§6§1§9§0§0黑曜石门正在搭建。");
        int seconds = cfg("portal_build_seconds", 8);
        for (int i = 0; i <= seconds; i++) {
            int step = i;
            if (brickPortal != null) Bukkit.getScheduler().runTaskLater(this, () -> animatePortalStep(room, brickPortal, step, seconds), i * 20L);
            if (netherPortal != null) Bukkit.getScheduler().runTaskLater(this, () -> animatePortalStep(room, netherPortal, step, seconds), i * 20L);
        }
    }

    private void animatePortalStep(Room room, Location base, int step, int total) {
        World world = base.getWorld();
        if (world == null) return;
        int x = base.getBlockX();
        int y = base.getBlockY();
        int z = base.getBlockZ();
        List<Location> blocks = new ArrayList<>();
        for (int dx = -1; dx <= 2; dx++) for (int dy = 0; dy <= 4; dy++) if (dx == -1 || dx == 2 || dy == 0 || dy == 4) blocks.add(new Location(world, x + dx, y + dy, z));
        int count = Math.min(blocks.size(), Math.max(1, blocks.size() * step / Math.max(1, total)));
        for (int i = 0; i < count; i++) blocks.get(i).getBlock().setType(Material.OBSIDIAN, false);
        world.spawnParticle(Particle.PORTAL, base.clone().add(0.5, 2, 0.5), 55, 1.2, 1.8, 0.4, 0.18);
        playWorldSound(base, "portal.build_step", Sound.BLOCK_PORTAL_TRIGGER, 0.8F, 0.8F + step * 0.06F);
        if (step >= total) {
            BlockData portalData = Material.NETHER_PORTAL.createBlockData();
            if (portalData instanceof Orientable orientable) orientable.setAxis(Axis.X);
            for (int dx = 0; dx <= 1; dx++) for (int dy = 1; dy <= 3; dy++) new Location(world, x + dx, y + dy, z).getBlock().setBlockData(portalData, false);
            room.broadcast(prefix() + "§x§6§6§1§9§0§0黑曜石门已经点燃。");
            playRoomSound(room, "portal.open", Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 1.0F);
        }
    }

    private void handlePlayerDownOnce(Room room, Player player, Player killer) {
        UUID uuid = player.getUniqueId();
        if (!downingPlayers.add(uuid)) return;
        Bukkit.getScheduler().runTaskLater(this, () -> downingPlayers.remove(uuid), 2L);
        handlePlayerDown(room, player, validKiller(room, player, killer));
    }

    private Player validKiller(Room room, Player victim, Player killer) {
        if (killer == null || victim == null || killer.getUniqueId().equals(victim.getUniqueId())) return null;
        if (activeRoom(killer).orElse(null) != room) return null;
        if (room.team(killer.getUniqueId()) == room.team(victim.getUniqueId())) return null;
        if (!room.canFight(killer)) return null;
        return killer;
    }

    private void handlePlayerDown(Room room, Player player, Player killer) {
        killer = validKiller(room, player, killer);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.setHealth(Math.max(1.0D, Math.min(maxHealth(player), maxHealth(player))));
        clearExperience(player);
        if (killer != null) room.kills.merge(killer.getUniqueId(), 1, Integer::sum);
        if (killer != null && room.dyingSeconds.containsKey(killer.getUniqueId())) killer.getInventory().addItem(new ItemStack(Material.GOLD_NUGGET, 1));
        if (killer != null) repairAllTools(killer);
        if (Objects.equals(room.corePlayer, player.getUniqueId())) {
            room.broadcast(prefix() + "§f" + player.getName() + " §x§f§f§7§c§0§0被击碎了核心。");
            triggerCoreDeath(room, killer);
            return;
        }
        if (room.dyingSeconds.containsKey(player.getUniqueId())) {
            if (!room.totemUsed.contains(player.getUniqueId())) {
                room.totemUsed.add(player.getUniqueId());
                room.dyingSeconds.remove(player.getUniqueId());
                player.setHealth(Math.min(2.0D, maxHealth(player)));
                if (room.coreStartLocation != null) player.teleport(room.coreStartLocation);
                setAttr(player, Attribute.MAX_HEALTH, Math.max(10.0D, maxHealth(player) * 0.5D));
                playSound(player, "totem.use", Sound.ITEM_TOTEM_USE, 1.0F, 1.0F);
                msg(player, "§x§F§F§B§B§6§6你用尽了最后一次不死效果。");
                return;
            }
            finalDeath(room, player, killer);
            return;
        }
        if (killer != null && items.pickaxeLevel(killer.getInventory().getItemInMainHand()) > 0) room.broadcast(prefix() + "§f" + player.getName() + " §x§f§f§7§c§0§0被 §f" + killer.getName() + " §x§f§f§7§c§0§0的狐稿挖碎了。");
        else if (killer != null) room.broadcast(prefix() + "§f" + player.getName() + " §x§F§F§B§B§6§6被 §f" + killer.getName() + " §x§F§F§B§B§6§6击倒了。");
        else room.broadcast(prefix() + "§f" + player.getName() + " §x§F§F§B§B§6§6倒下了。");
        startRespawn(room, player);
    }

    private void triggerCoreDeath(Room room, Player killer) {
        Player core = room.corePlayer == null ? null : Bukkit.getPlayer(room.corePlayer);
        if (core != null) finalDeath(room, core, killer);
        List<Player> candidates = room.onlineTeam(Team.NETHER).stream().filter(p -> !Objects.equals(p.getUniqueId(), room.corePlayer) && room.isActive(p)).toList();
        int count = (int) Math.ceil(candidates.size() * 0.5D);
        Collections.shuffle(candidates);
        for (int i = 0; i < count && i < candidates.size(); i++) enterDying(room, candidates.get(i));
        checkWin(room);
    }

    private void enterDying(Room room, Player player) {
        if (room.dyingSeconds.containsKey(player.getUniqueId()) || room.finalDead.contains(player.getUniqueId())) return;
        room.dyingSeconds.put(player.getUniqueId(), 90);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false, false));
        setAttr(player, Attribute.ATTACK_SPEED, 4.6D);
        setAttr(player, Attribute.MAX_HEALTH, Math.max(maxHealth(player), 220.0D));
        player.setHealth(Math.min(maxHealth(player), 220.0D));
        msg(player, "§x§6§6§1§9§0§0你进入濒死状态，吃濒死补给可以重置时间。");
    }

    private void tickDying(Room room) {
        Iterator<Map.Entry<UUID, Integer>> it = room.dyingSeconds.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            int left = entry.getValue() - 1;
            if (player == null || !room.players.contains(entry.getKey())) { it.remove(); continue; }
            if (left <= 0) {
                it.remove();
                if (!room.totemUsed.contains(entry.getKey())) {
                    room.totemUsed.add(entry.getKey());
                    if (room.coreStartLocation != null) player.teleport(room.coreStartLocation);
                    player.setHealth(Math.min(2.0D, maxHealth(player)));
                    setAttr(player, Attribute.MAX_HEALTH, Math.max(10.0D, maxHealth(player) * 0.5D));
                    playSound(player, "totem.use", Sound.ITEM_TOTEM_USE, 1.0F, 1.0F);
                    msg(player, "§x§F§F§B§B§6§6你用尽了最后一次不死效果。");
                } else {
                    finalDeath(room, player, null);
                }
            } else {
                entry.setValue(left);
                if (left <= 10 || left % 15 == 0) player.sendActionBar(Text.c("§x§6§6§1§9§0§0濒死 §f" + left + "s"));
            }
        }
    }

    private void startRespawn(Room room, Player player) {
        downgradePickaxes(room, player);
        room.respawnSeconds.put(player.getUniqueId(), ThreadLocalRandom.current().nextInt(10, 36));
        enterRespawnGhost(room, player);
        player.setHealth(Math.max(1.0D, maxHealth(player)));
        clearExperience(player);
        Location target = returnSpawn(room, player);
        if (target != null) player.teleport(target);
        player.sendActionBar(Text.c("§x§F§F§B§B§6§6等待复活"));
    }

    private void downgradePickaxes(Room room, Player player) {
        Team team = room.team(player.getUniqueId());
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            int level = items.pickaxeLevel(item);
            if (level > 1) inv.setItem(i, items.pickaxe(level - 1, team));
        }
    }

    private void tickRespawns(Room room) {
        Iterator<Map.Entry<UUID, Integer>> it = room.respawnSeconds.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            int left = entry.getValue() - 1;
            if (player == null || !room.players.contains(entry.getKey())) { it.remove(); continue; }
            if (left <= 0) {
                it.remove();
                respawn(room, player);
            } else {
                entry.setValue(left);
                player.sendActionBar(Text.c("§x§F§F§B§B§6§6复活 §f" + left + "s"));
            }
        }
    }

    private void respawn(Room room, Player player) {
        Team team = room.team(player.getUniqueId());
        leaveRespawnGhost(room, player);
        player.setGameMode(GameMode.SURVIVAL);
        resetStartState(player);
        player.setHealth(maxHealth(player));
        if (team == Team.BRICK) {
            Location spawn = roomLoc(room, maps.read().brickSpawn, MapSide.BRICK);
            if (spawn != null) player.teleport(spawn);
        } else {
            Location spawn = safeNetherRespawn(room);
            if (spawn != null) player.teleport(spawn);
        }
        applyGameName(player, room, team);
        playSound(player, "respawn", Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.9F, 1.3F);
    }

    private Location safeNetherRespawn(Room room) {
        Location base = roomLoc(room, maps.read().netherSpawn, MapSide.NETHER);
        if (base != null && isAirCube(base, 2)) return base;
        Player core = room.corePlayer == null ? null : Bukkit.getPlayer(room.corePlayer);
        if (core != null) {
            for (int r = 2; r <= 4; r++) {
                for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                    Location loc = core.getLocation().clone().add(dx, 0, dz);
                    if (loc.getBlock().getType().isAir() && loc.clone().add(0, 1, 0).getBlock().getType().isAir()) return loc;
                }
            }
            return core.getLocation();
        }
        return base;
    }

    private boolean isAirCube(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) return false;
        for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++) for (int z = -radius; z <= radius; z++) if (!world.getBlockAt(center.getBlockX()+x, center.getBlockY()+y, center.getBlockZ()+z).getType().isAir()) return false;
        return true;
    }

    private void finalDeath(Room room, Player player, Player killer) {
        leaveRespawnGhost(room, player);
        room.finalDead.add(player.getUniqueId());
        room.spectators.add(player.getUniqueId());
        room.dyingSeconds.remove(player.getUniqueId());
        room.respawnSeconds.remove(player.getUniqueId());
        player.setGameMode(GameMode.SPECTATOR);
        player.setHealth(Math.max(1.0D, maxHealth(player)));
        clearExperience(player);
        rememberFinalKillSound(room, killer);
        if (killer != null) room.broadcast(prefix() + "§f" + player.getName() + " §x§F§F§8§8§5§5被 §f" + killer.getName() + " §x§F§F§8§8§5§5终结了。");
        else room.broadcast(prefix() + "§f" + player.getName() + " §x§F§F§8§8§5§5被终结了。");
        checkWin(room);
    }

    private void rememberFinalKillSound(Room room, Player killer) {
        if (room == null) return;
        String key = "final.kill.crit";
        if (killer != null) {
            ItemStack hand = killer.getInventory().getItemInMainHand();
            Material type = hand == null ? Material.AIR : hand.getType();
            if (type == Material.MACE) key = "final.kill.mace";
            else if (type == Material.IRON_SPEAR || type == Material.GOLDEN_SPEAR) key = "final.kill.spear";
            else if (items.pickaxeLevel(hand) > 0) key = "final.kill.pickaxe";
        }
        room.lastFinalKillSoundKey = key;
    }

    private void checkWin(Room room) {
        if (room.status != Room.Status.RUNNING) return;
        if (room.brickCoreHealth <= 0) {
            endRoom(room, Team.NETHER, true);
            return;
        }
        boolean netherAlive = room.aliveCount(Team.NETHER) > 0;
        boolean brickAlive = room.aliveCount(Team.BRICK) > 0;
        if (!netherAlive) endRoom(room, Team.BRICK, true);
        else if (!brickAlive) endRoom(room, Team.NETHER, true);
    }

    private void repairAllTools(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getItemMeta() instanceof Damageable damageable) {
                damageable.setDamage(0);
                item.setItemMeta(damageable);
            }
        }
    }

    private boolean sameBlock(Location a, Location b) {
        return a != null && b != null && a.getWorld() == b.getWorld() && a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
    }

    private boolean prepareWaitingLobby(Room room) {
        if (room.lobbyWorld != null) return true;
        room.lobbyWorld = cloneWorld(maps.world(MapSide.LOBBY), runtimeWorldName(room, MapSide.LOBBY));
        return room.lobbyWorld != null;
    }

    private boolean createRuntimeWorlds(Room room) {
        if (room.lobbyWorld == null) {
            room.lobbyWorld = cloneWorld(maps.world(MapSide.LOBBY), runtimeWorldName(room, MapSide.LOBBY));
        }
        if (room.brickWorld == null) {
            room.brickWorld = cloneWorld(maps.world(MapSide.BRICK), runtimeWorldName(room, MapSide.BRICK));
        }
        if (room.netherWorld == null) {
            room.netherWorld = cloneWorld(maps.world(MapSide.NETHER), runtimeWorldName(room, MapSide.NETHER));
        }
        return room.lobbyWorld != null && room.brickWorld != null && room.netherWorld != null;
    }

    private String runtimeWorldName(Room room, MapSide side) {
        String suffix = switch (side) {
            case LOBBY -> "lobby";
            case BRICK -> "brick";
            case NETHER -> "nether";
        };
        return "brickguard_room_" + room.runtimeKey + "_" + suffix;
    }

    private World cloneWorld(World source, String targetName) {
        if (source == null) return null;
        source.save();
        File sourceFolder = source.getWorldFolder();
        File targetFolder = new File(Bukkit.getWorldContainer(), targetName);
        World loadedTarget = Bukkit.getWorld(targetName);
        if (loadedTarget != null) {
            Bukkit.unloadWorld(loadedTarget, false);
        }
        if (targetFolder.exists() && !safeDeleteRuntimeFolder(targetFolder)) {
            getLogger().warning("守卫战临时世界清理失败: " + targetFolder.getName());
            return null;
        }
        if (!copyFolder(sourceFolder.toPath(), targetFolder.toPath())) {
            safeDeleteRuntimeFolder(targetFolder);
            return null;
        }
        deleteFolder(new File(targetFolder, "uid.dat"));
        deleteFolder(new File(targetFolder, "session.lock"));
        WorldCreator creator = new WorldCreator(targetName);
        creator.generator(new org.bukkit.generator.ChunkGenerator() {});
        World world = creator.createWorld();
        if (world != null) {
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        }
        return world;
    }

    private boolean copyFolder(java.nio.file.Path source, java.nio.file.Path target) {
        try {
            java.nio.file.Files.walkFileTree(source, new java.nio.file.SimpleFileVisitor<>() {
                @Override
                public java.nio.file.FileVisitResult preVisitDirectory(java.nio.file.Path dir, java.nio.file.attribute.BasicFileAttributes attrs) throws java.io.IOException {
                    java.nio.file.Path relative = source.relativize(dir);
                    java.nio.file.Path dest = target.resolve(relative);
                    java.nio.file.Files.createDirectories(dest);
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
                @Override
                public java.nio.file.FileVisitResult visitFile(java.nio.file.Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws java.io.IOException {
                    String name = file.getFileName().toString();
                    if (name.equalsIgnoreCase("uid.dat") || name.equalsIgnoreCase("session.lock")) return java.nio.file.FileVisitResult.CONTINUE;
                    java.nio.file.Files.copy(file, target.resolve(source.relativize(file)), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.COPY_ATTRIBUTES);
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
            });
            return true;
        } catch (Exception exception) {
            getLogger().warning("复制守卫战世界失败: " + exception.getMessage());
            return false;
        }
    }

    private boolean safeDeleteRuntimeFolder(File folder) {
        if (folder == null || !folder.exists()) return true;
        try {
            java.nio.file.Path worldRoot = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
            java.nio.file.Path target = folder.toPath().toAbsolutePath().normalize();
            if (!target.startsWith(worldRoot) || !(folder.getName().startsWith("brickguard_room_") || folder.getName().startsWith("ybg_room_"))) {
                getLogger().warning("拒绝删除非守卫战临时世界目录: " + target);
                return false;
            }
            deleteFolder(folder);
            return !folder.exists();
        } catch (Exception exception) {
            getLogger().warning("删除守卫战临时世界失败: " + exception.getMessage());
            return false;
        }
    }

    private void deleteFolder(File file) {
        if (file == null || !file.exists()) return;
        try {
            java.nio.file.Path root = file.toPath().toAbsolutePath().normalize();
            java.nio.file.Files.walkFileTree(root, new java.nio.file.SimpleFileVisitor<>() {
                @Override
                public java.nio.file.FileVisitResult visitFile(java.nio.file.Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws java.io.IOException {
                    java.nio.file.Files.deleteIfExists(file);
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
                @Override
                public java.nio.file.FileVisitResult postVisitDirectory(java.nio.file.Path dir, java.io.IOException exc) throws java.io.IOException {
                    java.nio.file.Files.deleteIfExists(dir);
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception exception) {
            getLogger().warning("删除守卫战临时世界失败: " + exception.getMessage());
        }
    }

    private Location roomLoc(Room room, Point point, MapSide side) {
        if (point == null) return null;
        World world = switch (side) {
            case LOBBY -> room.lobbyWorld;
            case BRICK -> room.brickWorld;
            case NETHER -> room.netherWorld;
        };
        return world == null ? null : point.toLocation(world);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Optional<Room> optional = activeRoom(player);
        if (optional.isEmpty()) return;
        Room room = optional.get();
        String type = event.getRightClicked().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        if ("shop_brick".equals(type) || "shop_nether".equals(type)) {
            event.setCancelled(true);
            if (room.status != Room.Status.RUNNING || !room.canFight(player)) {
                msg(player, "§x§F§F§8§8§5§5现在不能交易。");
                return;
            }
            Team required = "shop_brick".equals(type) ? Team.BRICK : Team.NETHER;
            if (room.team(player.getUniqueId()) != required) {
                msg(player, "§x§F§F§8§8§5§5这个商人不会跟你交易。");
                return;
            }
            openShop(player);
            return;
        }
        if (event.getRightClicked() instanceof Player target && room.status == Room.Status.RUNNING && Objects.equals(room.corePlayer, player.getUniqueId()) && room.coreTransferLeft > 0 && !room.coreTransferUsed) {
            if (room.team(target.getUniqueId()) == Team.NETHER && !Objects.equals(target.getUniqueId(), player.getUniqueId())) {
                transferCore(room, player, target);
                event.setCancelled(true);
            }
        }
    }

    private void transferCore(Room room, Player oldCore, Player newCore) {
        if (room.coreTransferUsed) return;
        room.coreTransferUsed = true;
        room.coreTransferLeft = 0;
        resetCoreAttrs(oldCore, false);
        room.corePlayer = newCore.getUniqueId();
        equipNetherCore(newCore);
        room.broadcast(prefix() + "§x§6§6§1§9§0§0核心转交给了 §f" + newCore.getName() + "§x§6§6§1§9§0§0。");
        playRoomSound(room, "core.transfer", Sound.ENTITY_EVOKER_CAST_SPELL, 1.0F, 1.2F);
        applyGameName(oldCore, room, Team.NETHER);
        applyGameName(newCore, room, Team.NETHER);
    }

    private void endRoom(Room room, Team winner, boolean announce) {
        if (room.status == Room.Status.ENDED) return;
        room.status = Room.Status.ENDED;
        removeBossBars(room);
        if (announce) {
            if (winner == Team.BRICK) room.title("§x§f§f§7§c§0§0板砖队胜利", "§f下界核心已经破碎");
            else if (winner == Team.NETHER) room.title("§x§6§6§1§9§0§0下界砖队胜利", "§f板砖核心已经崩塌");
            else room.title("§x§7§D§F§F§C§8平局", "§f时间耗尽");
            if (room.lastFinalKillSoundKey != null) {
                playRoomSound(room, room.lastFinalKillSoundKey, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.95F, 1.15F);
            }
            playRoomSound(room, "game.end_dragon", Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0F, 1.0F);
            playRoomSound(room, winner == null ? "game.draw" : "game.win", winner == null ? Sound.BLOCK_AMETHYST_BLOCK_CHIME : Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
            showEndLeaderboard(room, winner);
            for (Player player : room.onlinePlayers()) {
                player.closeInventory();
                leaveRespawnGhost(room, player);
                player.setGameMode(GameMode.SPECTATOR);
            }
            room.task = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
                int left = 20;

                @Override
                public void run() {
                    if (left <= 0) {
                        if (room.task != null) room.task.cancel();
                        cleanupRoom(room);
                        return;
                    }
                    for (Player player : room.onlinePlayers()) {
                        player.sendActionBar(Text.c("§x§7§D§F§F§C§8结算中 §7| §f" + left + "s §7后返回"));
                        if (left <= 5) playSound(player, "game.return_countdown", Sound.BLOCK_NOTE_BLOCK_PLING, 0.65F, 1.1F);
                    }
                    left--;
                }
            }, 0L, 20L);
            return;
        }
        cleanupRoom(room);
    }

    private void cleanupRoom(Room room) {
        if (room.task != null) {
            room.task.cancel();
            room.task = null;
        }
        removeBossBars(room);
        for (Player player : room.onlinePlayers()) {
            resetPlayer(player, room);
            InventorySnapshot snapshot = room.snapshots.get(player.getUniqueId());
            if (snapshot != null) snapshot.restore(player);
            else sendLobbyFallback(player);
            clearName(player);
            playerRoom.remove(player.getUniqueId());
        }
        room.shopEntities.forEach(Entity::remove);
        room.shopEntities.clear();
        room.shopHealth.clear();
        room.placedBlockItems.clear();
        room.obsidianPoolDisplays.values().forEach(Entity::remove);
        room.obsidianPoolDisplays.clear();
        rooms.remove(room.id);
        unloadRuntimeWorld(room.lobbyWorld);
        unloadRuntimeWorld(room.brickWorld);
        unloadRuntimeWorld(room.netherWorld);
    }

    private void showEndLeaderboard(Room room, Team winner) {
        List<UUID> ranked = new ArrayList<>(room.players);
        ranked.sort((a, b) -> Integer.compare(room.kills.getOrDefault(b, 0), room.kills.getOrDefault(a, 0)));
        StringBuilder sb = new StringBuilder();
        sb.append("\n§x§F§F§D§7§0§0· · · · · §x§F§F§B§B§3§3本局排行榜§x§F§F§D§7§0§0 · · · · ·\n");
        sb.append("§f胜利方: ").append(winner == null ? "§7平局" : winner.color + winner.display).append("\n");
        String[] icons = {"§x§F§F§D§7§0§0🥇", "§x§C§0§C§0§C§0🥈", "§x§C§D§7§F§3§2🥉"};
        int limit = Math.min(8, ranked.size());
        for (int i = 0; i < limit; i++) {
            UUID uuid = ranked.get(i);
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            String name = offline.getName() == null ? "未知玩家" : offline.getName();
            Team team = room.team(uuid);
            String rank = i < 3 ? icons[i] : "§7#" + (i + 1);
            sb.append("  ").append(rank).append(" ")
                    .append(team == null ? "§7[无队伍]" : team.color + "[" + team.display + "]")
                    .append(" §f").append(name)
                    .append("  §7击杀: §x§F§F§B§B§6§6").append(room.kills.getOrDefault(uuid, 0));
            if (Objects.equals(room.corePlayer, uuid)) sb.append(" §x§6§6§1§9§0§0核心");
            sb.append("\n");
        }
        sb.append("§x§F§F§D§7§0§0· · · · · · · · · · · · · · ·\n");
        room.broadcast(sb.toString());
    }

    private void unloadRuntimeWorld(World world) {
        if (world == null) return;
        File folder = world.getWorldFolder();
        Bukkit.unloadWorld(world, false);
        safeDeleteRuntimeFolder(folder);
    }

    private void clearExperience(Player player) {
        if (player == null) return;
        player.setExp(0.0F);
        player.setLevel(0);
        player.setTotalExperience(0);
    }

    private void enterRespawnGhost(Room room, Player player) {
        UUID uuid = player.getUniqueId();
        room.spectators.add(uuid);
        respawnGhosts.add(uuid);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setInvulnerable(true);
        player.setCollidable(false);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.getUniqueId().equals(uuid)) viewer.hidePlayer(this, player);
        }
    }

    private void leaveRespawnGhost(Room room, Player player) {
        UUID uuid = player.getUniqueId();
        respawnGhosts.remove(uuid);
        if (room != null && !room.finalDead.contains(uuid)) room.spectators.remove(uuid);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.showPlayer(this, player);
        }
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.setInvulnerable(false);
        player.setCollidable(true);
        player.setFlying(false);
        player.setAllowFlight(false);
    }

    private void resetPlayer(Player player, Room room) {
        leaveRespawnGhost(room, player);
        homeNightVisionPlayers.remove(player.getUniqueId());
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        clearExperience(player);
        player.setInvulnerable(false);
        player.setCollidable(true);
        resetCoreAttrs(player, true);
        Double original = room.originalMaxHealth.get(player.getUniqueId());
        if (original != null) setAttr(player, Attribute.MAX_HEALTH, original);
        player.setHealth(Math.min(maxHealth(player), Math.max(1.0D, maxHealth(player))));
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        downingPlayers.remove(player.getUniqueId());
    }

    private void resetCoreAttrs(Player player, boolean full) {
        setAttr(player, Attribute.SCALE, 1.0D);
        setAttr(player, Attribute.ATTACK_DAMAGE, 1.0D);
        setAttr(player, Attribute.ATTACK_SPEED, 4.0D);
        if (full) player.removePotionEffect(PotionEffectType.GLOWING);
    }

    private double maxHealth(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        return attr == null ? 20.0D : attr.getValue();
    }

    private void setAttr(Player player, Attribute attr, double value) {
        AttributeInstance instance = player.getAttribute(attr);
        if (instance != null) instance.setBaseValue(value);
    }

    private void applyWaitingName(Player player, Team team) {
        String prefix = team == null ? "§7" : team.color;
        player.displayName(Text.c(prefix + player.getName()));
        player.playerListName(Text.c(prefix + player.getName()));
        player.customName(Text.c(prefix + player.getName()));
        player.setCustomNameVisible(true);
    }

    private void applyGameName(Player player, Room room, Team team) {
        boolean core = Objects.equals(room.corePlayer, player.getUniqueId());
        String name = (core ? "§x§6§6§1§9§0§0[核心] " : team.color + "[" + (team == Team.BRICK ? "板砖" : "下界") + "] ") + team.color + player.getName();
        player.displayName(Text.c(name));
        player.playerListName(Text.c(name));
        player.customName(Text.c(name));
        player.setCustomNameVisible(true);
    }

    private void clearName(Player player) {
        Component name = Component.text(player.getName()).decoration(TextDecoration.ITALIC, false);
        player.displayName(name);
        player.playerListName(name);
        player.customName(null);
        player.setCustomNameVisible(false);
    }

    private void updateScoreboards(Room room) {
        for (Player player : room.onlinePlayers()) {
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            org.bukkit.scoreboard.Team brick = board.registerNewTeam("brick");
            brick.color(NamedTextColor.GOLD);
            brick.setOption(Option.NAME_TAG_VISIBILITY, OptionStatus.ALWAYS);
            org.bukkit.scoreboard.Team nether = board.registerNewTeam("nether");
            nether.color(NamedTextColor.DARK_RED);
            nether.setOption(Option.NAME_TAG_VISIBILITY, OptionStatus.ALWAYS);
            for (Player online : room.onlinePlayers()) {
                if (room.team(online.getUniqueId()) == Team.BRICK) brick.addEntry(online.getName());
                else nether.addEntry(online.getName());
            }
            Objective objective = board.registerNewObjective("ybg", Criteria.DUMMY, Text.c("§x§f§f§7§c§0§0§l板砖守卫战"));
            objective.numberFormat(NumberFormat.blank());
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            List<String> lines = scoreboardLines(room, player);
            int score = lines.size();
            Set<String> used = new HashSet<>();
            for (String line : lines) {
                String entry = uniqueLine(line, used);
                objective.getScore(entry).setScore(score--);
            }
            player.setScoreboard(board);
        }
    }

    private List<String> scoreboardLines(Room room, Player player) {
        Team team = room.team(player.getUniqueId());
        int minutes = Math.max(0, room.gameLeft) / 60;
        int seconds = Math.max(0, room.gameLeft) % 60;
        List<String> lines = new ArrayList<>();
        lines.add("§7状态 §f对战中");
        lines.add("§r");
        lines.add("§f剩余 §x§7§D§F§F§C§8" + minutes + ":" + (seconds < 10 ? "0" : "") + seconds);
        lines.add("§f队伍 " + (team == null ? "§7未选择" : team.color + team.display));
        lines.add("§r§r");
        lines.add("§x§f§f§7§c§0§0板砖核心 §c" + room.brickCoreHealth + "§7/§c" + room.brickCoreMax);
        lines.add("§x§6§6§1§9§0§0黑曜石池 §f" + room.obsidianDeposited + "§7/§f" + cfg("obsidian_required", 10));
        if (Objects.equals(room.corePlayer, player.getUniqueId())) lines.add("§x§6§6§1§9§0§0核心: §f你");
        else if (room.corePlayer != null) {
            Player core = Bukkit.getPlayer(room.corePlayer);
            lines.add("§x§6§6§1§9§0§0核心 §f" + (core == null ? "离线" : core.getName()));
        }
        lines.add("§r§r§r");
        if (room.dyingSeconds.containsKey(player.getUniqueId())) lines.add("§x§F§F§8§8§5§5濒死 §f" + room.dyingSeconds.get(player.getUniqueId()) + "s");
        lines.add("§f击杀 §x§F§F§B§B§6§6" + room.kills.getOrDefault(player.getUniqueId(), 0));
        return lines;
    }

    private String uniqueLine(String line, Set<String> used) {
        String out = line;
        while (used.contains(out)) out += "§r";
        used.add(out);
        return out;
    }

    private void applyCoreBonuses(Room room, Player player) {
        if (Objects.equals(room.corePlayer, player.getUniqueId())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false, false));
        }
    }
}



