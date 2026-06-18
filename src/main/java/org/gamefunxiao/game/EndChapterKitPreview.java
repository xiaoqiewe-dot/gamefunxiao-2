package org.gamefunxiao.game;

import org.bukkit.inventory.ItemStack;

public class EndChapterKitPreview {

    private final ItemStack[] storageContents;
    private final ItemStack[] armorPreview;
    private final ItemStack offHandPreview;

    public EndChapterKitPreview(ItemStack[] storageContents, ItemStack[] armorPreview, ItemStack offHandPreview) {
        this.storageContents = cloneArray(storageContents, 36);
        this.armorPreview = cloneArray(armorPreview, 4);
        this.offHandPreview = offHandPreview == null ? null : offHandPreview.clone();
    }

    public ItemStack[] getStorageContents() {
        return cloneArray(storageContents, 36);
    }

    public ItemStack[] getArmorPreview() {
        return cloneArray(armorPreview, 4);
    }

    public ItemStack getOffHandPreview() {
        return offHandPreview == null ? null : offHandPreview.clone();
    }

    private static ItemStack[] cloneArray(ItemStack[] source, int size) {
        ItemStack[] copy = new ItemStack[size];
        if (source == null) {
            return copy;
        }
        for (int i = 0; i < size && i < source.length; i++) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
    }
}
