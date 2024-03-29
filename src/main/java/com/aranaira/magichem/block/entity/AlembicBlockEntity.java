package com.aranaira.magichem.block.entity;

import com.aranaira.magichem.Config;
import com.aranaira.magichem.MagiChemMod;
import com.aranaira.magichem.block.entity.ext.BlockEntityWithEfficiency;
import com.aranaira.magichem.block.entity.interfaces.IMateriaProcessingDevice;
import com.aranaira.magichem.capabilities.grime.GrimeProvider;
import com.aranaira.magichem.capabilities.grime.IGrimeCapability;
import com.aranaira.magichem.gui.AlembicMenu;
import com.aranaira.magichem.item.MateriaItem;
import com.aranaira.magichem.recipe.AlchemicalCompositionRecipe;
import com.aranaira.magichem.registry.BlockEntitiesRegistry;
import com.aranaira.magichem.registry.ItemRegistry;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class AlembicBlockEntity extends BlockEntityWithEfficiency implements MenuProvider, IMateriaProcessingDevice {
    public static final int
        SLOT_COUNT = 13,
        SLOT_BOTTLES = 0,
        SLOT_INPUT_START = 1, SLOT_INPUT_COUNT = 3,
        SLOT_OUTPUT_START = 4, SLOT_OUTPUT_COUNT  = 8,
        PROGRESS_BAR_WIDTH = 24, GRIME_BAR_WIDTH = 50,
        DATA_COUNT = 2, DATA_PROGRESS = 0, DATA_GRIME = 1;


    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected ContainerData data;
    private int progress = 0;

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if(slot == SLOT_BOTTLES)
                return stack.getItem() == Items.GLASS_BOTTLE;
            if(slot >= SLOT_INPUT_START && slot < SLOT_INPUT_START + SLOT_INPUT_COUNT)
                return !(stack.getItem() instanceof MateriaItem);
            if(slot >= SLOT_OUTPUT_START && slot < SLOT_OUTPUT_START + SLOT_OUTPUT_COUNT)
                return false;

            return super.isItemValid(slot, stack);
        }
    };

    ////////////////////
    // CONSTRUCTOR
    ////////////////////

    public AlembicBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesRegistry.ALEMBIC_BE.get(), pos, Config.alembicEfficiency, state);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                switch(pIndex) {
                    case DATA_PROGRESS: {
                        return AlembicBlockEntity.this.progress;
                    }
                    case DATA_GRIME: {
                        IGrimeCapability grime = GrimeProvider.getCapability(AlembicBlockEntity.this);
                        return grime.getGrime();
                    }
                    default: return -1;
                }
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch(pIndex) {
                    case DATA_PROGRESS: {
                        AlembicBlockEntity.this.progress = pValue;
                        break;
                    }
                    case DATA_GRIME: {
                        IGrimeCapability grime = GrimeProvider.getCapability(AlembicBlockEntity.this);
                        grime.setGrime(pValue);
                        break;
                    }
                }
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    //////////
    // BOILERPLATE CODE
    //////////

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.magichem.alembic");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AlembicMenu(id, inventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.put("inventory", itemHandler.serializeNBT());
        nbt.putInt("craftingProgress", this.progress);
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        itemHandler.deserializeNBT(nbt.getCompound("inventory"));
        progress = nbt.getInt("craftingProgress");
    }

    public void dropInventoryToWorld() {
        //Drop items in input slots, bottle slot, and processing slot as-is
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots()+4);
        for (int i = 0; i < SLOT_INPUT_COUNT + 1; i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);


        //Convert items in the output slots to alchemical waste
        SimpleContainer waste = new SimpleContainer(itemHandler.getSlots()+4);
        for (int i = 0; i < SLOT_OUTPUT_COUNT; i++) {
            ItemStack stack = itemHandler.getStackInSlot(SLOT_INPUT_START + i);
            waste.setItem(i, new ItemStack(ItemRegistry.ALCHEMICAL_WASTE.get(), stack.getCount()));
        }

        Containers.dropContents(this.level, this.worldPosition, waste);
    }

    private static Pair<Integer, ItemStack> getProcessingItem(AlembicBlockEntity entity) {
        int processingSlot = SLOT_INPUT_START+SLOT_INPUT_COUNT-1;
        ItemStack processingItem = entity.itemHandler.getStackInSlot(processingSlot);

        if(processingItem == ItemStack.EMPTY) {
            processingSlot--;
            processingItem = entity.itemHandler.getStackInSlot(processingSlot);
        }

        if(processingItem == ItemStack.EMPTY) {
            processingSlot--;
            processingItem = entity.itemHandler.getStackInSlot(processingSlot);
        }

        return new Pair<>(processingSlot, processingItem);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AlembicBlockEntity entity) {
        //skip all of this if grime is full
        if(GrimeProvider.getCapability(entity).getGrime() >= Config.alembicMaximumGrime)
            return;

        //figure out what slot and stack to target
        Pair<Integer, ItemStack> processing = getProcessingItem(entity);
        int processingSlot = processing.getFirst();
        ItemStack processingItem = processing.getSecond();

        AlchemicalCompositionRecipe recipe = getRecipeInSlot(entity, processingSlot);
        if(processingItem != ItemStack.EMPTY && recipe != null) {
            if(canCraftItem(entity, recipe)) {
                if (entity.progress > getOperationTicks(GrimeProvider.getCapability(entity).getGrime())) {
                    if (!level.isClientSide()) {
                        craftItem(entity, recipe, processingSlot);
                        entity.pushData();
                    }
                    if (!entity.isStalled)
                        entity.resetProgress();
                } else
                    entity.incrementProgress();
            }
        }
        else if(processingItem == ItemStack.EMPTY)
            entity.resetProgress();
    }

    ////////////////////
    // DATA SLOT HANDLING
    ////////////////////

    public static int getOperationTicks(int grime) {
        return Math.round(Config.alembicOperationTime * getTimeScalar(grime));
    }

    public int getProgressFromData() {
        return data.get(DATA_PROGRESS);
    }

    public static int getScaledProgress(int progress, int grime) {
        return PROGRESS_BAR_WIDTH * progress / getOperationTicks(grime);
    }

    @Override
    public int getGrimeFromData() {
        return data.get(DATA_GRIME);
    }

    @Override
    public int getMaximumGrime() {
        return Config.alembicMaximumGrime;
    }

    @Override
    public int clean() {
        int grimeDetected = GrimeProvider.getCapability(this).getGrime();
        IGrimeCapability grimeCapability = GrimeProvider.getCapability(this);
        grimeCapability.setGrime(0);
        data.set(DATA_GRIME, 0);
        return grimeDetected / Config.grimePerWaste;
    }

    public static int getScaledGrime(int grime) {
        return (GRIME_BAR_WIDTH * grime) / Config.alembicMaximumGrime;
    }

    public static float getGrimePercent(int grime) {
        return (float)grime / (float)Config.alembicMaximumGrime;
    }

    public static int getActualEfficiency(int grime) {
        float grimeScalar = 1f - Math.min(Math.max(Math.min(Math.max(getGrimePercent(grime) - 0.5f, 0f), 1f) * 2f, 0f), 1f);
        return Math.round(baseEfficiency * grimeScalar);
    }

    public static float getTimeScalar(int grime) {
        float grimeScalar = Math.min(Math.max(Math.min(Math.max(getGrimePercent(grime) - 0.5f, 0f), 1f) * 2f, 0f), 1f);
        return 1f + grimeScalar * 3f;
    }

    private void pushData() {
        this.data.set(DATA_PROGRESS, progress);

        int a = GrimeProvider.getCapability(this).getGrime();

        this.data.set(DATA_GRIME, a);

        int b = GrimeProvider.getCapability(this).getGrime();
    }

    ////////////////////
    // RECIPE HANDLING
    ////////////////////

    private static AlchemicalCompositionRecipe getRecipeInSlot(AlembicBlockEntity entity, int slot) {
        Level level = entity.level;

        AlchemicalCompositionRecipe recipe = AlchemicalCompositionRecipe.getDistillingRecipe(level, entity.itemHandler.getStackInSlot(slot));

        if(recipe != null) {
            return recipe;
        }

        return null;
    }

    private static boolean canCraftItem(AlembicBlockEntity entity, AlchemicalCompositionRecipe recipe) {
        SimpleContainer cont = new SimpleContainer(SLOT_OUTPUT_COUNT);
        for(int i=SLOT_OUTPUT_START; i<SLOT_OUTPUT_START+SLOT_OUTPUT_COUNT; i++) {
            cont.setItem(i-SLOT_OUTPUT_START, entity.itemHandler.getStackInSlot(i).copy());
        }

        for(int i=0; i<recipe.getComponentMateria().size(); i++) {
            if(!cont.canAddItem(recipe.getComponentMateria().get(i).copy()))
                return false;
            cont.addItem(recipe.getComponentMateria().get(i).copy());
        }

        return true;
    }

    private static void craftItem(AlembicBlockEntity entity, AlchemicalCompositionRecipe recipe, int processingSlot) {
        SimpleContainer outputSlots = new SimpleContainer(9);
        for(int i=0; i<SLOT_OUTPUT_COUNT; i++) {
            outputSlots.setItem(i, entity.itemHandler.getStackInSlot(SLOT_OUTPUT_START+i));
        }

        Pair<Integer, NonNullList<ItemStack>> pair = applyEfficiencyToCraftingResult(recipe.getComponentMateria(), AlembicBlockEntity.getActualEfficiency(GrimeProvider.getCapability(entity).getGrime()), recipe.getOutputRate(), Config.alembicGrimeOnSuccess, Config.alembicGrimeOnFailure);
        int grimeToAdd = Math.round(pair.getFirst() * recipe.getOutputRate());
        NonNullList<ItemStack> componentMateria = pair.getSecond();

        for(ItemStack item : componentMateria) {
            if(outputSlots.canAddItem(item)) {
                outputSlots.addItem(item);
            }
            else {
                entity.isStalled = true;
                break;
            }
        }

        if(!entity.isStalled) {
            for(int i=0; i<9; i++) {
                entity.itemHandler.setStackInSlot(SLOT_OUTPUT_START + i, outputSlots.getItem(i));
            }
            ItemStack processingSlotContents = entity.itemHandler.getStackInSlot(processingSlot);
            processingSlotContents.shrink(1);
            if(processingSlotContents.getCount() == 0)
                entity.itemHandler.setStackInSlot(processingSlot, ItemStack.EMPTY);
        }

        IGrimeCapability grimeCapability = GrimeProvider.getCapability(entity);
        grimeCapability.setGrime(Math.min(Math.max(grimeCapability.getGrime() + grimeToAdd, 0), Config.alembicMaximumGrime));
    }

    private static int nextInputSlotWithItem(AlembicBlockEntity entity) {
        //Select items bottom-first
        if(!entity.itemHandler.getStackInSlot(SLOT_INPUT_START + 2).isEmpty())
            return SLOT_INPUT_START + 2;
        else if(!entity.itemHandler.getStackInSlot(SLOT_INPUT_START + 1).isEmpty())
            return SLOT_INPUT_START + 1;
        else if(!entity.itemHandler.getStackInSlot(SLOT_INPUT_START).isEmpty())
            return SLOT_INPUT_START;
        else
            return -1;
    }

    private void resetProgress() {
        progress = 0;
    }

    private void incrementProgress() {
        progress++;
    }

    @Override
    public SimpleContainer getContentsOfOutputSlots() {
        SimpleContainer output = new SimpleContainer(SLOT_OUTPUT_COUNT);

        for(int i=SLOT_OUTPUT_START; i<SLOT_OUTPUT_START+SLOT_OUTPUT_COUNT; i++) {
            output.setItem(i-SLOT_OUTPUT_START, itemHandler.getStackInSlot(i));
        }

        return output;
    }

    @Override
    public void setContentsOfOutputSlots(SimpleContainer replacementInventory) {
        for(int i=SLOT_OUTPUT_START; i<SLOT_OUTPUT_START+SLOT_OUTPUT_COUNT; i++) {
            itemHandler.setStackInSlot(i, replacementInventory.getItem(i-SLOT_OUTPUT_START));
        }
    }
}
