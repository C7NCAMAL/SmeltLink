package com.smeltlink.inventory;

import java.lang.reflect.Field;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public final class FurnaceTelemetry {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Field DATA_ACCESS_FIELD;

    static {
        Field field = null;
        try {
            field = AbstractFurnaceBlockEntity.class.getDeclaredField("dataAccess");
            field.setAccessible(true);
        } catch (Throwable t) {
            LOGGER.error("Failed to access AbstractFurnaceBlockEntity.dataAccess field via reflection", t);
        }
        DATA_ACCESS_FIELD = field;
    }

    private FurnaceTelemetry() { }

    public static ContainerData getDataAccess(AbstractFurnaceBlockEntity furnace) {
        if (DATA_ACCESS_FIELD != null && furnace != null) {
            try {
                return (ContainerData) DATA_ACCESS_FIELD.get(furnace);
            } catch (Throwable t) {
                // fall through
            }
        }
        return null;
    }

    public static int getLitTime(AbstractFurnaceBlockEntity furnace) {
        ContainerData data = getDataAccess(furnace);
        return data != null ? data.get(0) : 0;
    }

    public static int getLitDuration(AbstractFurnaceBlockEntity furnace) {
        ContainerData data = getDataAccess(furnace);
        return data != null ? data.get(1) : 0;
    }

    public static int getCookProgress(AbstractFurnaceBlockEntity furnace) {
        ContainerData data = getDataAccess(furnace);
        return data != null ? data.get(2) : 0;
    }

    public static int getCookTotalTime(AbstractFurnaceBlockEntity furnace) {
        ContainerData data = getDataAccess(furnace);
        return data != null ? data.get(3) : 0;
    }
}