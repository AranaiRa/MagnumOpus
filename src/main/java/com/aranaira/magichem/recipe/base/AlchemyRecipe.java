package com.aranaira.magichem.recipe.base;

import javafx.util.Pair;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Random;
import java.util.function.Supplier;

public abstract class AlchemyRecipe<T extends Container> implements Recipe<T> {

    protected ResourceLocation id;
    protected NonNullList<Ingredient> inputs;
    protected NonNullList<Pair<Item, Integer>> outputs;
    protected Item catalyst;
    protected Item medium;

    private RecipeType<?> type;
    private RecipeSerializer<?> serializer;
    private Supplier<ItemStack> forcedResult;

    private static final Random r = new Random();

    public AlchemyRecipe (ResourceLocation pID, NonNullList<Ingredient> pInputs, NonNullList<Pair<Item, Integer>> pOutputs, Item pCatalyst, Item pMedium) {
        this.id = pID;
        this.inputs = pInputs;
        this.outputs = pOutputs;
        this.catalyst = pCatalyst;
        this.medium = pMedium;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return inputs;
    }

    public NonNullList<ItemStack> getRawOutputs() {
        NonNullList<ItemStack> outputItems = NonNullList.create();

        outputs.forEach(itemIntegerPair -> {
            outputItems.add(new ItemStack(itemIntegerPair.getKey(), itemIntegerPair.getValue()));
        });

        return outputItems;
    }

    public NonNullList<ItemStack> getOutputs(int efficiency) {
        if(efficiency < 100) {
            NonNullList<ItemStack> outputItems = NonNullList.create();

            outputs.forEach(itemIntegerPair -> {
                int count = 0;
                for (int i = 0; i < itemIntegerPair.getValue(); i++) {
                    if(r.nextInt(100) < efficiency) count++;
                }
                if (count > 0)
                    outputItems.add(new ItemStack(itemIntegerPair.getKey(), count));
            });

            return outputItems;
        } else return getRawOutputs();
    }

    // Unused, but necessary for IRecipe<>
    @Override
    public ItemStack assemble(T pContainer) {
        return getResultItem();
    }

    @Override
    public ItemStack getResultItem() {
        NonNullList<ItemStack> rawOutputs = getRawOutputs();
        return rawOutputs.isEmpty() ? ItemStack.EMPTY : rawOutputs.get(0);
    }

    @Override
    public RecipeType<?> getType() {
        return type;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    // No alchemy recipes should show up in the vanilla recipe book.
    @Override
    public String getGroup() {
        return "alchemical_processes";
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }
}
