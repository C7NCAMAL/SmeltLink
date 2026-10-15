package com.smeltlink.event;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.smeltlink.cluster.FurnaceCluster;
import com.smeltlink.cluster.FurnaceClusterFinder;
import com.smeltlink.cluster.FurnaceVariant;
import com.smeltlink.inventory.AggregateInventory;
import com.smeltlink.menu.SmeltLinkMenu;
import com.smeltlink.network.FurnaceClusterContextPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Tracks the furnace block right-clicked by a player so that when the
 * individual vanilla furnace GUI opens, the server sends the discovered
 * cluster context to the client to display the top-right "xN" toggle button.
 * <p>
 * Does NOT cancel RightClickBlock on shift-click, allowing normal block placement.
 */
public final class FurnaceInteractHandler {

    private static final Map<UUID, BlockPos> LAST_CLICKED_FURNACE = new ConcurrentHashMap<>();

    private FurnaceInteractHandler() { }

    public static void recordClickedFurnace(UUID playerId, BlockPos pos) {
        LAST_CLICKED_FURNACE.put(playerId, pos.immutable());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        if (player.isSpectator()) return;

        BlockPos pos = event.getPos();
        if (!(level.getBlockState(pos).getBlock() instanceof AbstractFurnaceBlock)) return;

        if (!level.isClientSide()) {
            LAST_CLICKED_FURNACE.put(player.getUUID(), pos.immutable());
        }

        // Smart Shift + Right-Click handling
        if (player.isShiftKeyDown()) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();

            // If player holds any placeable block in either hand, let block placement happen normally
            if (mainHand.getItem() instanceof BlockItem || offHand.getItem() instanceof BlockItem) {
                return;
            }

            // Otherwise (empty hand or non-block item), check cluster size:
            // If cluster has >= 2 furnaces, cancel vanilla event and open SmeltLink Share Mode directly.
            // If size is 1, let single furnace open normally.
            List<BlockPos> clusterPositions = FurnaceClusterFinder.findCluster(level, pos);
            if (clusterPositions.size() >= 2) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);

                if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                    openClusterMenu(serverPlayer, pos);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        if (!(event.getContainer() instanceof AbstractFurnaceMenu)) return;

        BlockPos pos = LAST_CLICKED_FURNACE.get(serverPlayer.getUUID());
        if (pos == null) return;

        Level level = serverPlayer.level();
        if (!(level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity)) return;

        List<BlockPos> clusterPositions = FurnaceClusterFinder.findCluster(level, pos);
        int maxClusterSize = Math.min(64, clusterPositions.size());

        PacketDistributor.sendToPlayer(serverPlayer, new FurnaceClusterContextPayload(pos, maxClusterSize));
    }

    /**
     * Opens the unified SmeltLink cluster menu for the given root furnace position.
     */
    public static void openClusterMenu(ServerPlayer serverPlayer, BlockPos pos) {
        Level level = serverPlayer.level();
        if (!level.isLoaded(pos)) return;
        if (!(level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity rootBE)) return;

        FurnaceVariant variant = FurnaceVariant.fromBlockEntityType(rootBE.getType());
        if (variant == null) return;

        List<BlockPos> clusterPositions = FurnaceClusterFinder.findCluster(level, pos);
        if (clusterPositions.isEmpty()) return;

        FurnaceCluster cluster = FurnaceCluster.create(level, clusterPositions, variant);
        if (cluster == null) return;

        if (serverPlayer.containerMenu != serverPlayer.inventoryMenu) {
            serverPlayer.doCloseContainer();
        }

        serverPlayer.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable(variant.translationKey());
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player p) {
                AggregateInventory aggregateInv = new AggregateInventory(cluster, level);
                return new SmeltLinkMenu(containerId, playerInv, cluster, aggregateInv);
            }
        }, buf -> {
            buf.writeBlockPos(pos);
            buf.writeVarInt(cluster.size());
            buf.writeVarInt(variant.ordinalId());
        });
    }

    /**
     * Opens the single individual vanilla furnace menu for the given root furnace position.
     * Calls doCloseContainer() first if another menu is open so ServerPlayer.openMenu()
     * does not send a ClientboundContainerClosePacket that centers the client mouse.
     */
    public static void openSingleFurnaceMenu(ServerPlayer serverPlayer, BlockPos pos) {
        Level level = serverPlayer.level();
        if (!level.isLoaded(pos)) return;
        if (level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity rootBE) {
            recordClickedFurnace(serverPlayer.getUUID(), pos);
            if (serverPlayer.containerMenu != serverPlayer.inventoryMenu) {
                serverPlayer.doCloseContainer();
            }
            serverPlayer.openMenu(rootBE);
        }
    }
}