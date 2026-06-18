package org.gamefunxiao;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;
import org.gamefunxiao.flash.FlashModeKeys;

import java.util.List;

public final class GameFunBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.compose(), event -> {
            TypedKey<Enchantment> enchantmentKey = RegistryKey.ENCHANTMENT.typedKey(
                    Key.key(FlashModeKeys.ENCHANTMENT_NAMESPACE, FlashModeKeys.UPGRADE_ENCHANTMENT_ID)
            );
            RegistryKeySet<ItemType> swords = RegistrySet.keySetFromValues(RegistryKey.ITEM, List.of(
                    Material.WOODEN_SWORD.asItemType(),
                    Material.STONE_SWORD.asItemType(),
                    Material.IRON_SWORD.asItemType(),
                    Material.GOLDEN_SWORD.asItemType(),
                    Material.DIAMOND_SWORD.asItemType(),
                    Material.NETHERITE_SWORD.asItemType()
            ));

            event.registry().register(enchantmentKey, builder -> {
                builder.description(Component.text("附魔改装"));
                builder.supportedItems(swords);
                builder.primaryItems(swords);
                builder.weight(1);
                builder.maxLevel(1);
                builder.minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(18, 0));
                builder.maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(32, 0));
                builder.anvilCost(6);
                builder.activeSlots(EquipmentSlotGroup.MAINHAND);
                builder.exclusiveWith(RegistrySet.keySet(RegistryKey.ENCHANTMENT, List.<TypedKey<Enchantment>>of()));
            });
        });
    }
}
