package com.aranaira.magichem.recipe;

import com.aranaira.magichem.recipe.base.AlchemyRecipe;
import com.google.gson.JsonObject;
import javafx.util.Pair;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.Nullable;

public class DistillationRecipe extends AlchemyRecipe<RecipeWrapper> {
    public DistillationRecipe(ResourceLocation pID, NonNullList<Ingredient> pInputs, NonNullList<Pair<Item, Integer>> pOutputs, Item pCatalyst, Item pMedium) {
        super(pID, pInputs, pOutputs, pCatalyst, pMedium);
    }

    @Override
    public boolean matches(RecipeWrapper pContainer, Level pLevel) {
        return false;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return false;
    }

    public static class Serializer implements RecipeSerializer<DistillationRecipe> {

        @Override
        public DistillationRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
            return null;
        }

        @Override
        public @Nullable DistillationRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
            return null;
        }

        @Override
        public void toNetwork(FriendlyByteBuf pBuffer, DistillationRecipe pRecipe) {

        }
    }
}
