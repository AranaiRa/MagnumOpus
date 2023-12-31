package com.aranaira.magichem;

import com.aranaira.magichem.datagen.FixationSeparationRecipeGenerator;
import com.aranaira.magichem.gui.CentrifugeScreen;
import com.aranaira.magichem.gui.CircleFabricationScreen;
import com.aranaira.magichem.registry.*;
import com.aranaira.magichem.gui.AlembicScreen;
import com.aranaira.magichem.gui.CirclePowerScreen;
import com.mna.api.guidebook.RegisterGuidebooksEvent;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MagiChemMod.MODID)
public class MagiChemMod
{
    public static final String MODID = "magichem";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);


    public MagiChemMod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ItemRegistry.register(modEventBus);
        BlockRegistry.register(modEventBus);
        BlockEntitiesRegistry.register(modEventBus);
        MenuRegistry.register(modEventBus);
        MateriaRegistry.register(modEventBus);

        RecipeRegistry.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        //Only uncomment this nonsense if we need to generate the custom JSON files again
        //FixationSeparationRecipeGenerator.parseRecipeTable();
        //FixationSeparationRecipeGenerator.generateRecipes();
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {

        });

        PacketRegistry.register();
    }

    @SubscribeEvent
    public void onRegisterGuidebooks(RegisterGuidebooksEvent event) {
        event.getRegistry().addGuidebookPath(new ResourceLocation(MODID, "guide"));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
            //ItemBlockRenderTypes.setRenderLayer(BlockRegistry.CIRCLE_POWER.get(), RenderType.cutout());
            MenuScreens.register(MenuRegistry.CIRCLE_FABRICATION_MENU.get(), CircleFabricationScreen::new);
            MenuScreens.register(MenuRegistry.CIRCLE_POWER_MENU.get(), CirclePowerScreen::new);
            MenuScreens.register(MenuRegistry.ALEMBIC_MENU.get(), AlembicScreen::new);
            MenuScreens.register(MenuRegistry.CENTRIFUGE_MENU.get(), CentrifugeScreen::new);
        }
    }
}
