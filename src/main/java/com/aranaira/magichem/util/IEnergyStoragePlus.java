package com.aranaira.magichem.util;

import net.minecraftforge.energy.EnergyStorage;

public abstract class IEnergyStoragePlus extends EnergyStorage {
    public IEnergyStoragePlus(int capacity, int maxTransfer) {
        super(capacity, maxTransfer);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int receivedEnergy = super.receiveEnergy(maxReceive, simulate);
        if (receivedEnergy != 0)
            onEnergyChanged();

        return receivedEnergy;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extractedEnergy = super.extractEnergy(maxExtract, simulate);
        if(extractedEnergy != 0)
            onEnergyChanged();

        return extractedEnergy;
    }

    public int setEnergy(int energy) {
        this.energy = energy;

        return energy;
    }

    public abstract void onEnergyChanged();
}
