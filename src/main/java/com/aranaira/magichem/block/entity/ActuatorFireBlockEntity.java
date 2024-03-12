package com.aranaira.magichem.block.entity;

import com.aranaira.magichem.Config;
import com.aranaira.magichem.foundation.DirectionalPluginBlockEntity;
import com.aranaira.magichem.foundation.IBlockWithPowerLevel;
import com.aranaira.magichem.foundation.IPluginDevice;
import com.aranaira.magichem.gui.ActuatorFireMenu;
import com.aranaira.magichem.gui.ActuatorFireScreen;
import com.aranaira.magichem.gui.ActuatorWaterScreen;
import com.aranaira.magichem.registry.BlockEntitiesRegistry;
import com.aranaira.magichem.registry.FluidRegistry;
import com.mna.api.affinity.Affinity;
import com.mna.api.blocks.tile.IEldrinConsumerTile;
import com.mna.api.particles.MAParticleType;
import com.mna.api.particles.ParticleInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ActuatorFireBlockEntity extends DirectionalPluginBlockEntity implements MenuProvider, IBlockWithPowerLevel, IPluginDevice, IEldrinConsumerTile, IFluidHandler {

    private static final int[]
            ELDRIN_POWER_USAGE = {0, 1, 3, 6, 10, 15, 21, 28, 36, 45, 55, 67, 82, 100},
            SMOKE_PER_PROCESS = {0, 3, 5, 7, 10, 13, 17, 22, 27, 33, 39, 46, 53, 61};
    private static final float[]
            POWER_REDUCTION_BASE = {0, 24, 26, 28, 30, 32, 34, 36, 38, 40, 42, 44, 46, 48},
            POWER_REDUCTION_FUEL_NORMAL = {0, 30, 32.5f, 35, 37.5f, 40, 42.5f, 45, 47.5f, 50, 52.5f, 55, 57.5f, 60},
            POWER_REDUCTION_FUEL_SUPER = {0, 36, 39, 42, 45, 48, 51, 54, 57, 60 ,63, 66, 69, 72};
    public static final int
            SLOT_COUNT = 1,
            DATA_COUNT = 4, DATA_REMAINING_ELDRIN_TIME = 0, DATA_POWER_LEVEL = 1, DATA_FLAGS = 2, DATA_SMOKE = 3,
            FLAG_IS_SATISFIED = 1, FLAG_REDUCTION_TYPE_POWER = 2, FLAG_FUEL_SATISFACTION_TYPE = 12, FLAG_FUEL_NORMAL = 4, FLAG_FUEL_SUPER = 8;
    private static final float
            PIPE_VIBRATION_ACCELERATION = 0.002f;
    private int
            powerLevel = 1,
            remainingEldrinTime = -1,
            flags;
    private float
            remainingEldrinForSatisfaction,
            pipeVibrationIntensity = 0;
    protected ContainerData data;
    private FluidStack containedSmoke;
    private final LazyOptional<IFluidHandler> fluidHandler;

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem().getBurnTime(stack, RecipeType.SMELTING) > 0;
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public ActuatorFireBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        this.containedSmoke = FluidStack.EMPTY;
        this.fluidHandler = LazyOptional.of(() -> this);
        this.flags = 0;
    }

    public ActuatorFireBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntitiesRegistry.ACTUATOR_FIRE_BE.get(), pPos, pBlockState);

        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch(pIndex) {
                    case DATA_REMAINING_ELDRIN_TIME -> ActuatorFireBlockEntity.this.remainingEldrinTime;
                    case DATA_POWER_LEVEL -> ActuatorFireBlockEntity.this.powerLevel;
                    case DATA_FLAGS -> ActuatorFireBlockEntity.this.flags;
                    case DATA_SMOKE -> ActuatorFireBlockEntity.this.containedSmoke.getAmount();
                    default -> -1;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case DATA_REMAINING_ELDRIN_TIME -> ActuatorFireBlockEntity.this.remainingEldrinTime = pValue;
                    case DATA_POWER_LEVEL -> ActuatorFireBlockEntity.this.powerLevel = pValue;
                    case DATA_FLAGS -> ActuatorFireBlockEntity.this.flags = pValue;
                    case DATA_SMOKE -> {
                        if(ActuatorFireBlockEntity.this.containedSmoke == FluidStack.EMPTY)
                            ActuatorFireBlockEntity.this.containedSmoke = new FluidStack(FluidRegistry.STEAM.get(), pValue);
                        else
                            ActuatorFireBlockEntity.this.containedSmoke.setAmount(pValue);
                    }
                }
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };

        this.containedSmoke = FluidStack.EMPTY;
        this.fluidHandler = LazyOptional.of(() -> this);
        this.flags = FLAG_IS_SATISFIED;
    }

    public boolean getIsFuelled() {
        return (this.flags & FLAG_FUEL_NORMAL) == FLAG_FUEL_NORMAL;
    }

    public static boolean getIsFuelled(int pFlags) {
        return (pFlags & FLAG_FUEL_NORMAL) == FLAG_FUEL_NORMAL;
    }

    public boolean getIsSuperFuelled() {
        return (this.flags & FLAG_FUEL_SUPER) == FLAG_FUEL_SUPER;
    }

    public static boolean getIsSuperFuelled(int pFlags) {
        return (pFlags & FLAG_FUEL_SUPER) == FLAG_FUEL_SUPER;
    }

    public boolean getIsPowerReductionMode() {
        return (this.flags & FLAG_REDUCTION_TYPE_POWER) == FLAG_REDUCTION_TYPE_POWER;
    }

    public static boolean getIsPowerReductionMode(int pFlags) {
        return (pFlags & FLAG_REDUCTION_TYPE_POWER) == FLAG_REDUCTION_TYPE_POWER;
    }

    public float getReductionRate() {
        boolean fuelSuper = this.getIsSuperFuelled();
        boolean fuelNormal = this.getIsFuelled();
        if(fuelSuper)
            return POWER_REDUCTION_FUEL_SUPER[this.powerLevel];
        else if(fuelNormal)
            return POWER_REDUCTION_FUEL_NORMAL[this.powerLevel];
        else
            return POWER_REDUCTION_BASE[this.powerLevel];
    }

    public static float getReductionRate(int pPowerLevel, int pFlags) {
        boolean fuelSuper = getIsSuperFuelled(pFlags);
        boolean fuelNormal = getIsFuelled(pFlags);
        if(fuelSuper)
            return POWER_REDUCTION_FUEL_SUPER[pPowerLevel];
        else if(fuelNormal)
            return POWER_REDUCTION_FUEL_NORMAL[pPowerLevel];
        else
            return POWER_REDUCTION_BASE[pPowerLevel];
    }

    public int getEldrinPowerUsage() {
        return ELDRIN_POWER_USAGE[this.powerLevel];
    }

    public static int getEldrinPowerUsage(int pPowerLevel) {
        return ELDRIN_POWER_USAGE[pPowerLevel];
    }

    public int getSmokePerProcess() {
        return SMOKE_PER_PROCESS[this.powerLevel];
    }

    public static int getSmokePerProcess(int pPowerLevel) {
        return SMOKE_PER_PROCESS[pPowerLevel];
    }

    public int getPowerLevel() {
        return this.powerLevel;
    }

    public void increasePowerLevel() {
        this.powerLevel = Math.min(13, this.powerLevel + 1);
    }

    public void decreasePowerLevel() {
        this.powerLevel = Math.max(1, this.powerLevel - 1);
    }

    @Override
    public void setPowerLevel(int pPowerLevel) {
        this.powerLevel = pPowerLevel;
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.putInt("remainingEldrinTime", remainingEldrinTime);
        nbt.putInt("powerLevel", powerLevel);
        nbt.putInt("tankSmoke", this.containedSmoke.getAmount());
        if(ownerUUID != null)
            nbt.putUUID("owner", ownerUUID);
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.remainingEldrinTime = nbt.getInt("remainingEldrinTime");
        this.powerLevel = nbt.getInt("powerLevel");

        int nbtSmoke = nbt.getInt("tankSmoke");
        if(nbtSmoke > 0)
            this.containedSmoke = new FluidStack(FluidRegistry.SMOKE.get(), nbtSmoke);
        else
            this.containedSmoke = FluidStack.EMPTY;

        if(nbt.contains("owner"))
            ownerUUID = nbt.getUUID("owner");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("remainingEldrinTime", remainingEldrinTime);
        nbt.putInt("powerLevel", powerLevel);
        nbt.putInt("tankSmoke", this.containedSmoke.getAmount());
        if(ownerUUID != null)
            nbt.putUUID("owner", ownerUUID);
        return nbt;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void processCompletedOperation() {
        int newTotal = Math.min(Config.delugePurifierTankCapacity, containedSmoke.getAmount() + getSmokePerProcess());
        containedSmoke = new FluidStack(FluidRegistry.SMOKE.get(), Math.min(newTotal, Config.delugePurifierTankCapacity));
    }

    public static boolean getIsSatisfied(ActuatorFireBlockEntity entity) {
        return (entity.flags & FLAG_IS_SATISFIED) == FLAG_IS_SATISFIED;
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState blockState, T t) {
        if(t instanceof ActuatorFireBlockEntity afbe) {

            float smoke = 0;
            if(afbe.getFluidInTank(0).getFluid() == FluidRegistry.SMOKE.get()) {
                smoke = afbe.getFluidInTank(0).getAmount() / (float) Config.infernoEngineTankCapacity;

                float mappedSmokePercent = Math.max(0, (smoke - 0.5f) * 2);
                if (mappedSmokePercent > 0f) {
                    int spawnModulus = 5 - (int) Math.floor(mappedSmokePercent * 4);

                    if (level.getGameTime() % spawnModulus == 0) {
                        level.addParticle(new MAParticleType(ParticleInit.COZY_SMOKE.get())
                        .setPhysics(true),
                        pos.getX(), pos.getY(), pos.getZ(), 0, 0.03f, 0);
                    }
                }
            }
        }
    }

    public static void delegatedTick(Level level, BlockPos pos, BlockState state, ActuatorFireBlockEntity entity) {
        Player ownerCheck = entity.getOwner();
        int powerDraw = entity.getEldrinPowerUsage();

        if(ownerCheck != null) {
            float consumption = entity.consume(ownerCheck, pos, pos.getCenter(), Affinity.FIRE, Math.min(powerDraw, entity.remainingEldrinForSatisfaction), 1);
            entity.remainingEldrinForSatisfaction -= consumption;

            if(entity.remainingEldrinTime <= 0) {
                if(entity.remainingEldrinForSatisfaction <= 0) {
                    entity.remainingEldrinForSatisfaction = powerDraw;
                    entity.remainingEldrinTime = Config.infernoEngineOperationTime;
                    if(!getIsSatisfied(entity)) {
                        entity.setChanged();
                        entity.level.sendBlockUpdated(entity.getBlockPos(), entity.getBlockState(), entity.getBlockState(), 3);
                    }
                    //process fuel reduction if present
                }
            }
            entity.remainingEldrinTime = Math.max(-1, entity.remainingEldrinTime - 1);

            if(entity.remainingEldrinTime >= 0) entity.flags = entity.flags | ActuatorFireBlockEntity.FLAG_IS_SATISFIED;
            else {
                if(getIsSatisfied(entity)) {
                    entity.flags = entity.flags & ~ActuatorFireBlockEntity.FLAG_IS_SATISFIED;
                    entity.setChanged();
                    entity.level.sendBlockUpdated(entity.getBlockPos(), entity.getBlockState(), entity.getBlockState(), 3);
                } else
                    entity.flags = entity.flags & ~ActuatorFireBlockEntity.FLAG_IS_SATISFIED;
            }
        }
    }

    public void handleAnimationDrivers() {
        if(remainingEldrinTime >= 0) pipeVibrationIntensity = Math.min(1.0f, pipeVibrationIntensity + PIPE_VIBRATION_ACCELERATION);
        else pipeVibrationIntensity = Math.max(0.0f, pipeVibrationIntensity - PIPE_VIBRATION_ACCELERATION * 2.5f);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        if(cap == ForgeCapabilities.FLUID_HANDLER) return fluidHandler.cast();

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.lazyItemHandler.invalidate();
        this.fluidHandler.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new ActuatorFireMenu(i, inventory, this, this.data);
    }

    public static float getWaterPercent(int pWaterAmount) {
        return (float)pWaterAmount * 100f / Config.infernoEngineTankCapacity;
    }

    public static float getSmokePercent(int pSmokeAmount) {
        return (float)pSmokeAmount * 100f / Config.infernoEngineTankCapacity;
    }

    public static int getScaledSmoke(int pSmokeAmount) {
        return pSmokeAmount * ActuatorFireScreen.FLUID_GAUGE_H / Config.infernoEngineTankCapacity;
    }

    public float getPipeVibrationIntensity() {
        return pipeVibrationIntensity;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int i) {
        return containedSmoke;
    }

    @Override
    public int getTankCapacity(int i) {
        return Config.infernoEngineTankCapacity;
    }

    @Override
    public boolean isFluidValid(int i, @NotNull FluidStack fluidStack) {
        return fluidStack.getFluid() == FluidRegistry.SMOKE.get();
    }

    @Override
    public int fill(FluidStack fluidStack, FluidAction fluidAction) {
        //Smoke is extract only
        return 0;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack fluidStack, FluidAction fluidAction) {
        if(fluidAction.execute()) {
            setChanged();
            level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }

        //Smoke is extract only
        Fluid fluid = fluidStack.getFluid();
        int incomingAmount = fluidStack.getAmount();
        if(fluid == FluidRegistry.SMOKE.get()) {
            int extantAmount = containedSmoke.getAmount();
            if(extantAmount >= incomingAmount) {
                if(fluidAction == FluidAction.EXECUTE)
                    containedSmoke.shrink(incomingAmount);
                return new FluidStack(fluid, incomingAmount);
            } else {
                if(fluidAction == FluidAction.EXECUTE)
                    containedSmoke = FluidStack.EMPTY;
                return new FluidStack(fluid, incomingAmount - extantAmount);
            }
        }
        return fluidStack;
    }

    @Override
    public @NotNull FluidStack drain(int i, FluidAction fluidAction) {
        return drain(new FluidStack(FluidRegistry.SMOKE.get(), i), fluidAction);
    }
}
