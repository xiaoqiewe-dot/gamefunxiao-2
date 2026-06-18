package org.gamefunxiao.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.gamefunxiao.cosmetics.HunterKillEffect;
import org.gamefunxiao.GameFunXiao;
import org.gamefunxiao.cosmetics.HunterVictoryEffect;
import org.gamefunxiao.cosmetics.LuckyPillarsVictoryEffect;

public class GameFunPlaceholderExpansion extends PlaceholderExpansion {

    private final GameFunXiao plugin;

    public GameFunPlaceholderExpansion(GameFunXiao plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "gamefun";
    }

    @Override
    public String getAuthor() {
        return "QiCheng";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) {
            return "";
        }

        String key = params.toLowerCase();
        if (key.equals("currency") || key.equals("currency_name")) {
            return plugin.getConfigManager().getMiniGameCurrencyName();
        }

        if (player == null || player.getUniqueId() == null) {
            return "";
        }

        return switch (key) {
            case "coins", "minigame_coins", "money", "balance" ->
                    String.valueOf(plugin.getPlayerDataManager().getCoins(player.getUniqueId()));
            case "hunter_victory_effect" ->
                    plugin.getPlayerDataManager().getSelectedHunterVictoryEffect(player.getUniqueId());
            case "hunter_victory_effect_name" -> {
                String effectId = plugin.getPlayerDataManager().getSelectedHunterVictoryEffect(player.getUniqueId());
                yield HunterVictoryEffect.byId(effectId).getDisplayName(plugin);
            }
            case "lucky_pillars_victory_effect" ->
                    plugin.getPlayerDataManager().getSelectedLuckyPillarsVictoryEffect(player.getUniqueId());
            case "lucky_pillars_victory_effect_name" -> {
                String effectId = plugin.getPlayerDataManager().getSelectedLuckyPillarsVictoryEffect(player.getUniqueId());
                yield LuckyPillarsVictoryEffect.byId(effectId).getDisplayName(plugin);
            }
            case "hunter_kill_effect" ->
                    plugin.getPlayerDataManager().getSelectedHunterKillEffect(player.getUniqueId());
            case "hunter_kill_effect_name" -> {
                String effectId = plugin.getPlayerDataManager().getSelectedHunterKillEffect(player.getUniqueId());
                yield HunterKillEffect.byId(effectId).getDisplayName(plugin);
            }
            default -> "";
        };
    }
}
