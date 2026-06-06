package com.phantomstorage.block;

import com.mojang.serialization.MapCodec;
import com.phantomstorage.ModBlockEntities;
import com.phantomstorage.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class PhantomLinkBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<PhantomLinkBlock> CODEC = simpleCodec(PhantomLinkBlock::new);

    public PhantomLinkBlock(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && placer instanceof net.minecraft.world.entity.player.Player player) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PhantomLinkBlockEntity link) {
                link.setOwnerUUID(player.getUUID());
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhantomLinkBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.PHANTOM_LINK.get(),
                PhantomLinkBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PhantomLinkBlockEntity link) || !link.isFlowingClient) return;

        // 1 particle per animateTick call — conservative but always visible
        double px = pos.getX() + 0.2 + random.nextDouble() * 0.6;
        double py = pos.getY() + 0.2 + random.nextDouble() * 0.6;
        double pz = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
        double vx = (random.nextDouble() - 0.5) * 0.015;
        double vy = (random.nextDouble() - 0.5) * 0.015;
        double vz = (random.nextDouble() - 0.5) * 0.015;
        // addAlwaysVisibleParticle bypasses the particle setting — visible at Minimal/Decreased/All
        level.addAlwaysVisibleParticle(ModParticles.PHANTOM_LINK_FLOW.get(), px, py, pz, vx, vy, vz);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PhantomLinkBlockEntity link) {
                link.onRemoved();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
