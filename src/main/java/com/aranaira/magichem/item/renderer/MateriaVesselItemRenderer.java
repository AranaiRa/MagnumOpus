package com.aranaira.magichem.item.renderer;

import com.aranaira.magichem.Config;
import com.aranaira.magichem.MagiChemMod;
import com.aranaira.magichem.item.EssentiaItem;
import com.aranaira.magichem.item.MateriaItem;
import com.aranaira.magichem.item.MateriaVesselItem;
import com.aranaira.magichem.registry.ItemRegistry;
import com.aranaira.magichem.util.render.MateriaVesselContentsRenderUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.ForgeRenderTypes;
import net.minecraftforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MateriaVesselItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ResourceLocation SPECIAL_RENDERER = new ResourceLocation(MagiChemMod.MODID, "item/special/materia_vessel");
    private BakedModel bakedModel;

    private final List<Direction> sides = Util.make(new ArrayList<>(), c -> {
        Collections.addAll(c, Direction.values());
        c.add(null);
    });

    public MateriaVesselItemRenderer(BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, EntityModelSet pEntityModelSet) {
        super(pBlockEntityRenderDispatcher, pEntityModelSet);
    }

    @Override
    public void renderByItem(ItemStack pStack, ItemTransforms.TransformType pTransformType, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {

        if(this.bakedModel == null)
            this.bakedModel = Minecraft.getInstance().getModelManager().getModel(SPECIAL_RENDERER);

        PoseStack.Pose last = pPoseStack.last();
        //VertexConsumer buffer = pBuffer.getBuffer(RenderType.armorCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        VertexConsumer buffer = pBuffer.getBuffer(RenderType.solid());

        //ghz version
        BakedModel jarModel = bakedModel.getOverrides().resolve(bakedModel, pStack, null, null, 0);
        if (jarModel == null)
            jarModel = bakedModel;

        boolean leftHand = (pTransformType == ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND)
                || (pTransformType == ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND);

        pPoseStack.pushPose();
        pPoseStack.translate(0.5D, 0.5D, 0.5D);
        jarModel = ForgeHooksClient.handleCameraTransforms(pPoseStack, jarModel, pTransformType, leftHand);

        CompoundTag nbt = pStack.getTag();
        if(nbt != null) {
            if(nbt.contains("type")) {
                MateriaItem materia = ItemRegistry.getMateriaMap(false, false)
                        .get(nbt.getString("type"));
                int cap = materia instanceof EssentiaItem ? Config.materiaVesselEssentiaCapacity : Config.materiaVesselAdmixtureCapacity;
                float fill = (float)nbt.getInt("amount") / (float)cap;

                MateriaVesselContentsRenderUtil.renderFluidContents(last.pose(), last.normal(), buffer, fill, materia.getMateriaColor());
                if(materia instanceof EssentiaItem ei)
                    MateriaVesselContentsRenderUtil.renderEssentiaLabel(last.pose(), last.normal(), buffer, ei, Direction.NORTH);
            }
        }

        pPoseStack.translate(-0.5D, -0.5D, -0.5D);

        buffer = pBuffer.getBuffer(ForgeRenderTypes.ITEM_UNSORTED_TRANSLUCENT.get());
        RandomSource rnd = RandomSource.create();
        for (Direction side : sides)
        {
            rnd.setSeed(42);
            for (BakedQuad quad : jarModel.getQuads(null, side, rnd, ModelData.EMPTY, null))
            {
                buffer.putBulkData(pPoseStack.last(), quad, 1.0f, 1.0f, 1.0f, pPackedLight, pPackedOverlay);
            }
        }

        pPoseStack.popPose();

        super.renderByItem(pStack, pTransformType, pPoseStack, pBuffer, pPackedLight, pPackedOverlay);
    }
}