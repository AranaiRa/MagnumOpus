package com.aranaira.magichem.block.entity;

import com.aranaira.magichem.gui.CirclePowerMenu;
import com.aranaira.magichem.registry.ItemRegistry;
import com.aranaira.magichem.registry.BlockEntitiesRegistry;
import com.aranaira.magichem.util.IEnergyStoragePlus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CirclePowerBlockEntity extends BlockEntity implements MenuProvider {
    public static final String REGISTRY_NAME = "circle_power";

    private final ItemStackHandler itemHandler = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public CirclePowerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesRegistry.CIRCLE_POWER_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> CirclePowerBlockEntity.this.progressReagentTier1;
                    case 1 -> CirclePowerBlockEntity.this.progressReagentTier2;
                    case 2 -> CirclePowerBlockEntity.this.progressReagentTier3;
                    case 3 -> CirclePowerBlockEntity.this.progressReagentTier4;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> CirclePowerBlockEntity.this.progressReagentTier1 = value;
                    case 1 -> CirclePowerBlockEntity.this.progressReagentTier2 = value;
                    case 2 -> CirclePowerBlockEntity.this.progressReagentTier3 = value;
                    case 3 -> CirclePowerBlockEntity.this.progressReagentTier4 = value;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    protected final ContainerData data;
    private int
            progressReagentTier1 = 0,
            progressReagentTier2 = 0,
            progressReagentTier3 = 0,
            progressReagentTier4 = 0;
    private static int
            maxProgressReagentTier1 = 1280,
            maxProgressReagentTier2 = 1280,
            maxProgressReagentTier3 = 1280,
            maxProgressReagentTier4 = 1280;

    public static final RegistryObject<Item>
            REAGENT_TIER1 =  ItemRegistry.SILVER_DUST,
            WASTE_TIER1 =  ItemRegistry.TARNISHED_SILVER_LUMP;

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.magichem.circle_power");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CirclePowerMenu(id, inventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ENERGY) {
            return lazyEnergyHandler.cast();
        }

        if(cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyEnergyHandler = LazyOptional.of(() -> ENERGY_STORAGE);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergyHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.put("inventory", itemHandler.serializeNBT());
        nbt.putInt(REGISTRY_NAME+".progressReagentTier1", this.progressReagentTier1);
        nbt.putInt(REGISTRY_NAME+".progressReagentTier2", this.progressReagentTier2);
        nbt.putInt(REGISTRY_NAME+".progressReagentTier3", this.progressReagentTier3);
        nbt.putInt(REGISTRY_NAME+".progressReagentTier4", this.progressReagentTier4);
        nbt.putInt(REGISTRY_NAME+".energy", this.ENERGY_STORAGE.getEnergyStored());
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        itemHandler.deserializeNBT(nbt.getCompound("inventory"));
        progressReagentTier1 = nbt.getInt(REGISTRY_NAME+".progressReagentTier1");
        progressReagentTier2 = nbt.getInt(REGISTRY_NAME+".progressReagentTier2");
        progressReagentTier3 = nbt.getInt(REGISTRY_NAME+".progressReagentTier3");
        progressReagentTier4 = nbt.getInt(REGISTRY_NAME+".progressReagentTier4");
        ENERGY_STORAGE.setEnergy(nbt.getInt(REGISTRY_NAME+".energy"));
    }

    public void dropInventoryToWorld() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots()+4);
        for (int i=0; i<itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        //Make sure we don't void reagents entirely if the block is broken; always drop waste of a currently "burning" reagent
        if(progressReagentTier1 > 0) inventory.addItem(new ItemStack(WASTE_TIER1.get(), 1));

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CirclePowerBlockEntity entity) {
        if(level.isClientSide()) {
            return;
        }

        if(hasReagent(1, entity) || entity.progressReagentTier1 > 0) {
            entity.incrementProgress(1);
            setChanged(level, pos, state);

            if(entity.progressReagentTier1 >= entity.maxProgressReagentTier1) {
                ejectWaste(1, level, entity);
                entity.resetProgress(1);
            }
            setChanged(level, pos, state);
        }

        generatePower(entity);
    }

    /* GENERATOR REAGENT USE LOGIC */

    private static void ejectWaste(int tier, Level level, CirclePowerBlockEntity entity) {
        ItemStack wasteProduct = null;

        switch (tier) {
            case 1: {
                wasteProduct = new ItemStack(WASTE_TIER1.get(), 1);
            }
        }

        if(wasteProduct != null)
            Containers.dropItemStack(level, entity.worldPosition.getX(), entity.worldPosition.getY()+0.125, entity.worldPosition.getZ(), wasteProduct);
    }

    private void resetProgress(int tier) {
        switch (tier) {
            case 1: progressReagentTier1 = 0;
            case 2: progressReagentTier2 = 0;
            case 3: progressReagentTier3 = 0;
            case 4: progressReagentTier4 = 0;
        }
    }

    private void incrementProgress(int tier) {
        if(tier == 1) progressReagentTier1++;
        if(tier == 2) progressReagentTier2++;
        if(tier == 3) progressReagentTier3++;
        if(tier == 4) progressReagentTier4++;
    }

    public static int getMaxProgressByTier(int tier) {
        switch (tier) {
            case 1: return maxProgressReagentTier1;
            case 2: return maxProgressReagentTier2;
            case 3: return maxProgressReagentTier3;
            case 4: return maxProgressReagentTier4;
            default: return -1;
        }
    }

    private static boolean hasReagent(int reagentTier, CirclePowerBlockEntity entity) {
        SimpleContainer inventory = new SimpleContainer(entity.itemHandler.getSlots());
        for (int i=0; i<entity.itemHandler.getSlots(); i++) {
            inventory.setItem(i, entity.itemHandler.getStackInSlot(i));
        }

        boolean query = false;

        switch(reagentTier) {
            case 1: {
                query = entity.itemHandler.getStackInSlot(0).getItem() == REAGENT_TIER1.get();
                //Consume the reagent if we don't have an existing one "burning"
                if(query && entity.progressReagentTier1 == 0) {
                    entity.itemHandler.getStackInSlot(0).setCount(0);
                    entity.incrementProgress(1);
                }
            }
        }

        return query;
    }

    /* FE STUFF */
    private static final int
        ENERGY_GEN_1_REAGENT = 3,
        ENERGY_GEN_2_REAGENT = 12,
        ENERGY_GEN_3_REAGENT = 48,
        ENERGY_GEN_4_REAGENT = 200,
        ENERGY_MAX_MULTIPLIER = 3;

    private final IEnergyStoragePlus ENERGY_STORAGE = new IEnergyStoragePlus(Integer.MAX_VALUE, Integer.MAX_VALUE) {
        @Override
        public void onEnergyChanged() {
            setChanged();
        }
    };

    private static void generatePower(CirclePowerBlockEntity entity) {
        int reagentCount = 0;
        int currentEnergy = entity.ENERGY_STORAGE.getEnergyStored();
        if(entity.progressReagentTier1 > 0) reagentCount++;
        if(entity.progressReagentTier2 > 0) reagentCount++;
        if(entity.progressReagentTier3 > 0) reagentCount++;
        if(entity.progressReagentTier4 > 0) reagentCount++;

        switch(reagentCount) {
            case 1: {
                int cap = ENERGY_GEN_1_REAGENT * ENERGY_MAX_MULTIPLIER;
                if(currentEnergy < cap) {
                    int mod = currentEnergy + ENERGY_GEN_1_REAGENT;
                    if(currentEnergy > cap) mod = cap - currentEnergy;
                    int insert = entity.ENERGY_STORAGE.receiveEnergy(mod,false);
                    //int insert = entity.ENERGY_STORAGE.setEnergy(ENERGY_GEN_1_REAGENT);
                }
            }
        }
    }
}