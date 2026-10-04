package io.github.moosasharwaan.appliedquartermaster.storage;

import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;

import java.util.Set;

/** The three storage blocks the tablet can browse, and which items each one takes. */
public enum StorageKind {
    LIBRARY("library"),
    ARMORY("armory"),
    TOOLS("tools");

    public static final int SLOTS = 8;

    public static final TagKey<Item> LIBRARY_BOOKS = tag("library_books");
    public static final TagKey<Item> LIBRARY_BLOCKED = tag("library_blocked");
    public static final TagKey<Item> ARMORY_ITEMS = tag("armory_items");
    public static final TagKey<Item> TOOL_RACK_ITEMS = tag("tool_rack_items");

    private static final Set<String> GUIDE_MODS = Set.of("patchouli", "modonomicon", "guideme");
    private static final String[] BOOK_WORDS = {"book", "guide", "manual", "journal", "codex", "lexicon",
            "encyclopedia", "almanac", "handbook", "grimoire", "notebook", "compendium", "tome", "bible", "diary"};

    private final String id;

    StorageKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public Component title() {
        return Component.translatable("gui.appliedquartermaster.tab." + id);
    }

    public static StorageKind byIndex(int index) {
        var values = values();
        return index >= 0 && index < values.length ? values[index] : null;
    }

    public boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return switch (this) {
            case LIBRARY -> isGuideBook(stack);
            case ARMORY -> isWeaponOrTool(stack);
            case TOOLS -> isUtilityTool(stack);
        };
    }

    /** Guide books from any mod and written books; never enchanted books, Apothic tomes or blank books. */
    public static boolean isGuideBook(ItemStack stack) {
        if (stack.is(LIBRARY_BLOCKED) || stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK)
                || stack.has(DataComponents.STORED_ENCHANTMENTS)) {
            return false;
        }
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key.getNamespace().startsWith("apothic") && key.getPath().endsWith("tome")) {
            return false;
        }
        if (stack.is(LIBRARY_BOOKS) || stack.has(DataComponents.WRITTEN_BOOK_CONTENT)
                || stack.has(DataComponents.WRITABLE_BOOK_CONTENT)) {
            return true;
        }
        if (GUIDE_MODS.contains(key.getNamespace())) {
            return true;
        }
        for (var type : stack.getComponents().keySet()) {
            var typeKey = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (typeKey != null && GUIDE_MODS.contains(typeKey.getNamespace())) {
                return true;
            }
        }
        var path = key.getPath();
        for (var word : BOOK_WORDS) {
            if (path.contains(word)) {
                return !path.contains("enchant") && !path.contains("shelf");
            }
        }
        return false;
    }

    /** Weapons and mining tools. */
    public static boolean isWeaponOrTool(ItemStack stack) {
        var item = stack.getItem();
        return stack.is(ARMORY_ITEMS) || stack.has(DataComponents.TOOL) || stack.has(DataComponents.WEAPON)
                || item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem
                || item instanceof MaceItem || item instanceof ShieldItem;
    }

    /** Wrenches, memory cards and other single-item tools; not armour and not food. */
    public static boolean isUtilityTool(ItemStack stack) {
        if (stack.is(TOOL_RACK_ITEMS)) {
            return true;
        }
        if (stack.getMaxStackSize() != 1 || stack.has(DataComponents.FOOD)) {
            return false;
        }
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable == null || equippable.slot() == EquipmentSlot.MAINHAND || equippable.slot() == EquipmentSlot.OFFHAND;
    }

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, AppliedQuartermaster.id(name));
    }
}
