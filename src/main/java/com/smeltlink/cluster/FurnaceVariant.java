package com.smeltlink.cluster;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

public enum FurnaceVariant {

    FURNACE(0,
            BlockEntityTypes.FURNACE,
            RecipeType.SMELTING,
            RecipePropertySet.FURNACE_INPUT,
            Identifier.withDefaultNamespace("textures/gui/container/furnace.png"),
            Identifier.withDefaultNamespace("container/furnace/lit_progress"),
            Identifier.withDefaultNamespace("container/furnace/burn_progress"),
            "container.furnace"),
    SMOKER(1,
            BlockEntityTypes.SMOKER,
            RecipeType.SMOKING,
            RecipePropertySet.SMOKER_INPUT,
            Identifier.withDefaultNamespace("textures/gui/container/smoker.png"),
            Identifier.withDefaultNamespace("container/smoker/lit_progress"),
            Identifier.withDefaultNamespace("container/smoker/burn_progress"),
            "container.smoker"),
    BLAST_FURNACE(2,
            BlockEntityTypes.BLAST_FURNACE,
            RecipeType.BLASTING,
            RecipePropertySet.BLAST_FURNACE_INPUT,
            Identifier.withDefaultNamespace("textures/gui/container/blast_furnace.png"),
            Identifier.withDefaultNamespace("container/blast_furnace/lit_progress"),
            Identifier.withDefaultNamespace("container/blast_furnace/burn_progress"),
            "container.blast_furnace");

    private final int ordinalId;
    private final BlockEntityType<?> blockEntityType;
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;
    private final ResourceKey<RecipePropertySet> recipePropertySet;
    private final Identifier guiTexture;
    private final Identifier litProgressSprite;
    private final Identifier burnProgressSprite;
    private final String translationKey;

    FurnaceVariant(int ordinalId, BlockEntityType<?> blockEntityType,
                   RecipeType<? extends AbstractCookingRecipe> recipeType,
                   ResourceKey<RecipePropertySet> recipePropertySet,
                   Identifier guiTexture, Identifier litProgressSprite,
                   Identifier burnProgressSprite, String translationKey) {
        this.ordinalId = ordinalId;
        this.blockEntityType = blockEntityType;
        this.recipeType = recipeType;
        this.recipePropertySet = recipePropertySet;
        this.guiTexture = guiTexture;
        this.litProgressSprite = litProgressSprite;
        this.burnProgressSprite = burnProgressSprite;
        this.translationKey = translationKey;
    }

    public int ordinalId() { return ordinalId; }
    public BlockEntityType<?> blockEntityType() { return blockEntityType; }
    public RecipeType<? extends AbstractCookingRecipe> recipeType() { return recipeType; }
    public ResourceKey<RecipePropertySet> recipePropertySet() { return recipePropertySet; }
    public Identifier guiTexture() { return guiTexture; }
    public Identifier litProgressSprite() { return litProgressSprite; }
    public Identifier burnProgressSprite() { return burnProgressSprite; }
    public String translationKey() { return translationKey; }

    public static FurnaceVariant fromBlockEntityType(BlockEntityType<?> type) {
        if (type == BlockEntityTypes.FURNACE)       return FURNACE;
        if (type == BlockEntityTypes.SMOKER)        return SMOKER;
        if (type == BlockEntityTypes.BLAST_FURNACE) return BLAST_FURNACE;
        return null;
    }

    public static FurnaceVariant fromOrdinal(int id) {
        return switch (id) {
            case 0  -> FURNACE;
            case 1  -> SMOKER;
            case 2  -> BLAST_FURNACE;
            default -> FURNACE;
        };
    }
}