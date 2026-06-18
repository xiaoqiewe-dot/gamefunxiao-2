package org.gamefunxiao.tab;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.gamefunxiao.GameFunXiao;

import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TabHeaderFooterManager {

    private static final Pattern HEX_SECTION_PATTERN = Pattern.compile("§x(?:§([0-9a-fA-F])){6}");
    private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("(?i)[§&]([0-9a-fk-or])");

    private final GameFunXiao plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private BukkitTask task;

    public TabHeaderFooterManager(GameFunXiao plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        if (!plugin.getConfigManager().getConfig().getBoolean("header-footer.enabled", true)) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::applyToOnlinePlayers, 40L, 100L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void applyLater(Player player) {
        if (player == null) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> apply(player), 40L);
    }

    public void applyToOnlinePlayers() {
        if (!isCurrentServerGameing()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            apply(player);
        }
    }

    public void apply(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (!plugin.getConfigManager().getConfig().getBoolean("header-footer.enabled", true)) {
            return;
        }
        if (!isCurrentServerGameing()) {
            return;
        }

        List<String> headerLines = plugin.getConfigManager().getConfig().getStringList("header-footer.header");
        List<String> footerLines = plugin.getConfigManager().getConfig().getStringList("header-footer.footer");
        Component header = parse(String.join("\n", headerLines), player);
        Component footer = parse(String.join("\n", footerLines), player);
        player.sendPlayerListHeaderAndFooter(header, footer);
    }

    private boolean isCurrentServerGameing() {
        String serverName = applyPlaceholders(null, "%qichengmorebungeeapi_server%");
        if (serverName == null || serverName.isBlank() || serverName.contains("%qichengmorebungeeapi_server%")) {
            return false;
        }
        String targetServer = plugin.getConfigManager().getConfig().getString("header-footer.target_server", "gameing");
        return targetServer != null && targetServer.equalsIgnoreCase(serverName.trim());
    }

    private Component parse(String text, Player player) {
        String replaced = applyPlaceholders(player, text == null ? "" : text);
        String mini = legacyToMiniMessage(normalizeMiniMessage(replaced));
        try {
            return miniMessage.deserialize(mini);
        } catch (ParsingException exception) {
            plugin.getLogger().warning("TAB header-footer 解析失败，已使用纯文本降级: " + exception.getMessage());
            return LegacyComponentSerializer.legacySection().deserialize(replaced.replace('&', '§'));
        }
    }

    private String applyPlaceholders(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return text;
        }
        try {
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Method method = papi.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            Object result = method.invoke(null, player, text);
            return result == null ? text : result.toString();
        } catch (ReflectiveOperationException exception) {
            return text;
        }
    }

    private String legacyToMiniMessage(String text) {
        Matcher hexMatcher = HEX_SECTION_PATTERN.matcher(text);
        StringBuffer hexBuffer = new StringBuffer();
        while (hexMatcher.find()) {
            String matched = hexMatcher.group();
            String hex = matched.replace("§x", "").replace("§", "");
            hexMatcher.appendReplacement(hexBuffer, Matcher.quoteReplacement("<#" + hex + ">"));
        }
        hexMatcher.appendTail(hexBuffer);

        Matcher codeMatcher = LEGACY_CODE_PATTERN.matcher(hexBuffer.toString());
        StringBuffer codeBuffer = new StringBuffer();
        while (codeMatcher.find()) {
            codeMatcher.appendReplacement(codeBuffer, Matcher.quoteReplacement(toMiniTag(codeMatcher.group(1).charAt(0))));
        }
        codeMatcher.appendTail(codeBuffer);

        return codeBuffer.toString()
                .replace("<reset><gradient:", "<reset><gradient:");
    }

    private String normalizeMiniMessage(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text
                .replace("<b><<b/>", "<b>\\<</b>")
                .replace("<b/>", "</b>");
    }

    private String toMiniTag(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "";
            case 'r' -> "<reset>";
            default -> "";
        };
    }
}
