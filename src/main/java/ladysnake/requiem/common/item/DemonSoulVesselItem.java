/*
 * Requiem
 * Copyright (C) 2017-2021 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 *
 * Linking this mod statically or dynamically with other
 * modules is making a combined work based on this mod.
 * Thus, the terms and conditions of the GNU General Public License cover the whole combination.
 *
 * In addition, as a special exception, the copyright holders of
 * this mod give you permission to combine this mod
 * with free software programs or libraries that are released under the GNU LGPL
 * and with code included in the standard release of Minecraft under All Rights Reserved (or
 * modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the GNU GPL for this mod
 * and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of this mod are not obligated to grant
 * this special exception for their modified versions; it is their choice whether to do so.
 * The GNU General Public License gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version which carries forward this exception.
 */
package ladysnake.requiem.common.item;

import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.api.v1.remnant.RemnantType;
import ladysnake.requiem.common.network.RequiemNetworking;
import ladysnake.requiem.common.sound.RequiemSoundEvents;
import ladysnake.requiem.core.mixin.access.LivingEntityAccessor;
import ladysnake.requiem.core.possession.PossessionComponentImpl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ChatUtil;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class DemonSoulVesselItem extends Item {
    private final RemnantType remnantType;
    private final Formatting color;
    private final String tooltip;

    public DemonSoulVesselItem(RemnantType remnantType, Formatting color, Settings settings, String tooltip) {
        super(settings);
        this.remnantType = remnantType;
        this.color = color;
        this.tooltip = tooltip;
    }

    public RemnantType getRemnantType() {
        return remnantType;
    }

    public Formatting getTooltipColor() {
        return color;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() == Blocks.LECTERN) {
            return LecternBlock.putBookIfAbsent(ctx.getPlayer(), world, pos, state, ctx.getStack()) ? ActionResult.SUCCESS : ActionResult.PASS;
        } else {
            return ActionResult.PASS;
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (player instanceof ServerPlayerEntity serverPlayer && stack.getItem() == this) {
            RemnantComponent remnantComponent = RemnantComponent.get(player);
            RemnantType currentState = remnantComponent.getRemnantType();

            if (currentState != this.remnantType) {
                PossessionComponent possessionComponent = PossessionComponent.get(player);
                MobEntity possessedEntity = possessionComponent.getPossessedEntity();

                if (possessedEntity == null || possessionComponent.isCuring()) {
                    world.playSound(null,
                        player.getX(), player.getY(), player.getZ(),
                        RequiemSoundEvents.ITEM_OPUS_USE,
                        player.getSoundCategory(), 1.0F, 0.1F
                    );
                    world.playSound(null,
                        player.getX(), player.getY(), player.getZ(),
                        this.remnantType.isDemon() ? RequiemSoundEvents.EFFECT_BECOME_REMNANT : RequiemSoundEvents.EFFECT_BECOME_MORTAL,
                        player.getSoundCategory(), 1.4F, 0.1F
                    );
                    RequiemNetworking.sendTo(serverPlayer, RequiemNetworking.createOpusUsePacket(this.remnantType, true));

                    remnantComponent.become(this.remnantType, true);

                    if (possessedEntity != null) {
                        if (remnantComponent.canCurePossessed(possessedEntity)) {
                            remnantComponent.curePossessed(possessedEntity);
                        } else if (remnantComponent.isVagrant()) {
                            possessionComponent.startPossessing(possessedEntity);
                        } else {
                            PossessionComponentImpl.dropEquipment(possessedEntity, serverPlayer);
                        }
                    } else if (remnantComponent.isIncorporeal() && serverPlayer.interactionManager.isSurvivalLike()) {
                        ((LivingEntityAccessor)player).requiem$invokeDropInventory();
                    }

                    player.incrementStat(Stats.USED.getOrCreateStat(this));
                    stack.decrement(1);
                }
            }

            return new TypedActionResult<>(ActionResult.SUCCESS, stack);
        }

        return new TypedActionResult<>(ActionResult.FAIL, stack);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> lines, TooltipContext ctx) {
        lines.add(new TranslatableText(tooltip).formatted(this.getTooltipColor()));

        if (stack.hasNbt()) {
            NbtCompound tag = stack.getNbt();
            assert tag != null;
            String author = tag.getString("author");
            if (!ChatUtil.isEmpty(author)) {
                lines.add((new TranslatableText("book.byAuthor", author)).formatted(Formatting.GRAY));
            }
        }
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
