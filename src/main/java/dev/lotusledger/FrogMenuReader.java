package dev.lotusledger;

import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.component.ItemLore;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Menu-only polling, with immutable component references cached so unchanged lore is not re-parsed. */
public final class FrogMenuReader {
    private record Cached(Component name, ItemLore lore, Frog frog, FrogMenu.Entry entry) {}
    private Object menu;
    private final Map<Integer, Cached> slots = new HashMap<>();
    private int parseCount;
    public int parseCount() { return parseCount; }
    public void clear() { menu = null; slots.clear(); }
    public Map<Frog, FrogMenu.Entry> read(AbstractContainerMenu current, Container playerInventory) {
        if (menu != current) { clear(); menu = current; }
        Map<Frog, FrogMenu.Entry> result = new EnumMap<>(Frog.class);
        var duplicates = java.util.EnumSet.noneOf(Frog.class);
        for (int index = 0; index < current.slots.size(); index++) {
            var slot = current.slots.get(index);
            if (slot.container == playerInventory) continue;
            var item = slot.getItem();
            Component name = item.getCustomName();
            ItemLore lore = item.get(DataComponents.LORE);
            Cached cached = slots.get(index);
            if (cached == null || cached.name != name || cached.lore != lore) {
                Frog frog = name == null ? null : Frog.fromName(GameText.clean(name.getString())).orElse(null);
                FrogMenu.Entry entry = null;
                if (frog != null && lore != null) {
                    parseCount++;
                    entry = FrogMenu.parse(lore.lines().stream().map(Component::getString).toList()).orElse(null);
                }
                cached = new Cached(name, lore, frog, entry);
                slots.put(index, cached);
            }
            if (cached.entry != null && !duplicates.contains(cached.frog)) {
                if (result.putIfAbsent(cached.frog, cached.entry) != null) {
                    result.remove(cached.frog);
                    duplicates.add(cached.frog);
                }
            }
        }
        return result;
    }
}
