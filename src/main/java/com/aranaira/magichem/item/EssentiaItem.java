package com.aranaira.magichem.item;

import com.aranaira.magichem.foundation.Essentia;
import com.aranaira.magichem.foundation.enums.EEssentiaHouse;
import com.aranaira.magichem.registry.CreativeModeTabs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class EssentiaItem extends Item implements Essentia {

    private final String name;
    private final String abbreviation;
    private final int color;
    private final EEssentiaHouse house;
    private final int wheel;

    public EssentiaItem(String essentiaName, String essentiaAbbreviation, String essentiaHouse, int essentiaWheel, String essentiaColor) {
        super(new Item.Properties().tab(CreativeModeTabs.MAGICHEM_TAB));
        this.name = essentiaName;
        this.abbreviation = essentiaAbbreviation;
        this.house = parseStringToHouse(essentiaHouse, essentiaName);
        this.wheel = essentiaWheel;
        this.color = Integer.parseInt(essentiaColor, 16) | 0xFF00000;
    }

    private EEssentiaHouse parseStringToHouse(String input, String nameForErrorHandling) {
        switch(input) {
            case "elements": return EEssentiaHouse.ELEMENTS;
            case "qualities": return EEssentiaHouse.QUALITIES;
            case "alchemy": return EEssentiaHouse.ALCHEMY;
            case "none": return EEssentiaHouse.NONE;
            default: {
                System.out.println("Essentia entry \""+nameForErrorHandling+"\" has an invalid House, defaulting to NONE. Was something misspelled?");
                return EEssentiaHouse.NONE;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(MutableComponent.create(
                new TranslatableContents("tooltip.magichem."+getMateriaName())).withStyle(ChatFormatting.DARK_GRAY)
        );
        tooltipComponents.add(MutableComponent.create(
                new LiteralContents("\""+getAbbreviation())).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)
        );
    }

    @Override
    public int getWheel() {
        return this.wheel;
    }

    @Override
    public EEssentiaHouse getHouse() {
        return this.house;
    }

    @Override
    public String getMateriaName() {
        return this.name;
    }

    @Override
    public String getAbbreviation() {
        return this.abbreviation;
    }

    @Override
    public int getColor() {
        return this.color;
    }
}