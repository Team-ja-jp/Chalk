package io.github.mortuusars.chalk.core;

import io.github.mortuusars.chalk.Chalk;
import io.github.mortuusars.chalk.blocks.ChalkMarkBlock;
import io.github.mortuusars.chalk.utils.ParticleUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class ChalkMark {
    public static InteractionResult tryDraw(MarkSymbol symbol, DyeColor color, boolean isGlowing, BlockPos clickedPos, Direction clickedFace, Vec3 clickLocation, Level level) {
        if ( !ChalkMark.canBeDrawnAt(clickedPos.relative(clickedFace), clickedPos, clickedFace, level) ) {
//            LOGGER.info("Chalk cannot be drawn at this position. ({}, {}, {})", clickedPos.getX(), clickedPos.getY(), clickedPos.getZ());
            return InteractionResult.FAIL;
        }

        final boolean isClickedOnAMark = level.getBlockState(clickedPos).getBlock() instanceof ChalkMarkBlock;

        BlockPos newMarkPosition = isClickedOnAMark ? clickedPos : clickedPos.relative(clickedFace);
        final Direction newMarkFacing = isClickedOnAMark ? level.getBlockState(newMarkPosition).getValue(ChalkMarkBlock.FACING) : clickedFace;

//        BlockState markBlockState = ChalkMark.createMarkBlockState(symbol, color, newMarkFacing, clickLocation, clickedPos, isGlowing);

        BlockState markBlockState = Chalk.Blocks.getMarkBlock(color).defaultBlockState()
                .setValue(ChalkMarkBlock.FACING, newMarkFacing)
                .setValue(ChalkMarkBlock.SYMBOL, symbol)
                .setValue(ChalkMarkBlock.ORIENTATION, SymbolOrientation.fromClickLocationAll(clickLocation, clickedFace))
                .setValue(ChalkMarkBlock.GLOWING, isGlowing);

        // Cancel drawing if marks are same.
        // Remove old mark if different.
        if (isClickedOnAMark) {
            BlockState oldMarkBlockState = level.getBlockState(newMarkPosition);

            if (markBlockState.getValue(ChalkMarkBlock.ORIENTATION).equals(oldMarkBlockState.getValue(ChalkMarkBlock.ORIENTATION))
                    && newMarkFacing == oldMarkBlockState.getValue(ChalkMarkBlock.FACING)
                    && symbol == oldMarkBlockState.getValue(ChalkMarkBlock.SYMBOL)
                    && (!isGlowing || oldMarkBlockState.getValue(ChalkMarkBlock.GLOWING))) {
                return InteractionResult.FAIL;
            }

            // Remove old mark. It would be replaced with new one.
            level.removeBlock(newMarkPosition, false);
        }

        ChalkMark.drawMark(markBlockState, newMarkPosition, level);
        return InteractionResult.SUCCESS;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean canBeDrawnAt(BlockPos pos, BlockPos clickedBlockPos, Direction clickedFace, Level level){
        BlockState clickedBlockState = level.getBlockState(clickedBlockPos);
        if (clickedBlockState.getBlock() instanceof ChalkMarkBlock)
            return true; // Marks can be replaced.

        BlockState stateAtMarkPos = level.getBlockState(pos);
        if (stateAtMarkPos.isAir() || stateAtMarkPos.getBlock() instanceof ChalkMarkBlock)
            return Block.isFaceFull(clickedBlockState.getCollisionShape(level, clickedBlockPos), clickedFace);

        return false;
    }

    public static boolean canBeDrawnOn(BlockPos pos, Direction face, Level level) {
        BlockState blockStateAtPos = level.getBlockState(pos);
        return canMarkReplace(level.getBlockState(pos.relative(face))) &&
                Block.isFaceFull(blockStateAtPos.getCollisionShape(level, pos), face);
    }

    public static boolean canMarkReplace(BlockState state) {
        return state.isAir() || state.getBlock() instanceof ChalkMarkBlock;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean drawMark(BlockState markState, BlockPos markPos, Level level) {
        boolean isMarkDrawn = level.setBlock(markPos, markState, Block.UPDATE_ALL_IMMEDIATE);

        if (isMarkDrawn){
            double pX = markPos.getX() + 0.5;
            double pY = markPos.getY() + 0.5;
            double pZ = markPos.getZ() + 0.5;
            level.playSound(null, pX, pY, pZ, Chalk.SoundEvents.MARK_DRAW.get(),
                    SoundSource.BLOCKS, 0.7f,  new Random().nextFloat() * 0.2f + 0.8f);

            if (level.isClientSide) {
                DyeColor color = ((ChalkMarkBlock) markState.getBlock()).getColor();
                ParticleUtils.spawnColorDustParticles(color, level, markPos, markState.getValue(ChalkMarkBlock.FACING));
            }
        }

        return isMarkDrawn;
    }
}
