package com.aranaira.magichem.registry;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class CreativeModeTabs {
    public static final CreativeModeTab MAGICHEM_TAB = new CreativeModeTab("magichemtab") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(BlockRegistry.CIRCLE_POWER.get());
        }
    };

    public static final CreativeModeTab MAGICHEM_MATERIA_TAB = new CreativeModeTab("magichemtab.substances") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ItemRegistry.ADMIXTURES.getEntries().stream().findAny().get().get());
        }
    };
}
