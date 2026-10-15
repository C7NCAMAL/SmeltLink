package com.smeltlink.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SmeltLinkConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_CONNECTED_FURNACES = BUILDER
            .comment("Maximum number of furnaces that can form a connected cluster.")
            .defineInRange("max_connected_furnaces", 64, 2, 256);

    public static final ModConfigSpec.BooleanValue STRICT_INPUT_MATCHING = BUILDER
            .comment("When true, only items with a valid smelting recipe for the",
                     "cluster's furnace type will be accepted into the input slot.")
            .define("strict_input_matching", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private SmeltLinkConfig() { }
}