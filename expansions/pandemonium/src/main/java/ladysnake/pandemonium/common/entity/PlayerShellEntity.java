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
package ladysnake.pandemonium.common.entity;

import baritone.api.IBaritone;
import baritone.api.event.events.BlockInteractEvent;
import baritone.api.event.events.PathEvent;
import baritone.api.event.listener.IGameEventListener;
import com.demonwav.mcdev.annotations.CheckEnv;
import com.demonwav.mcdev.annotations.Env;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import io.github.ladysnake.impersonate.Impersonator;
import ladysnake.pandemonium.common.PlayerSplitter;
import ladysnake.pandemonium.common.entity.ai.ShellBlockGoal;
import ladysnake.pandemonium.common.entity.ai.ShellEatGoal;
import ladysnake.pandemonium.common.entity.ai.ShellPathfindingProcess;
import ladysnake.pandemonium.common.entity.ai.ShellRevengeGoal;
import ladysnake.pandemonium.common.entity.ai.brain.PandemoniumMemoryModules;
import ladysnake.pandemonium.common.entity.ai.brain.PandemoniumSensorTypes;
import ladysnake.pandemonium.common.entity.ai.brain.PlayerShellBrain;
import ladysnake.pandemonium.common.entity.fakeplayer.GuidedFakePlayerEntity;
import ladysnake.requiem.api.v1.remnant.AttritionFocus;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.GoToWalkTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.dynamic.GlobalPos;
import net.minecraft.util.math.Vec3d;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.apiguardian.api.API.Status.MAINTAINED;

// TODO add inventory access
public class PlayerShellEntity extends GuidedFakePlayerEntity {
    public static final List<SensorType<? extends Sensor<? super PlayerShellEntity>>> SENSOR_TYPES = ImmutableList.of(
        SensorType.NEAREST_LIVING_ENTITIES,
        SensorType.NEAREST_PLAYERS,
        PandemoniumSensorTypes.CLOSEST_PLAYER_HOSTILE
    );
    public static final List<MemoryModuleType<?>> MEMORY_MODULE_TYPES = ImmutableList.of(
        MemoryModuleType.MOBS,
        MemoryModuleType.VISIBLE_MOBS,
        MemoryModuleType.NEAREST_VISIBLE_PLAYER,
        MemoryModuleType.LOOK_TARGET,
        MemoryModuleType.WALK_TARGET,
        MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
        MemoryModuleType.ATTACK_TARGET,
        MemoryModuleType.HOME,
        PandemoniumMemoryModules.GO_HOME_ATTEMPTS
    );

    private final ShellPathfindingProcess pathfindingProcess;

    @CheckEnv(Env.SERVER)
    @API(status = MAINTAINED)
    public PlayerShellEntity(EntityType<? extends PlayerEntity> type, ServerWorld world) {
        super(type, world);
        // default to showing whole skin
        this.getDataTracker().set(PLAYER_MODEL_PARTS, (byte) 0xFF);
        ((MobNavigation) this.guide.getNavigation()).setCanPathThroughDoors(true);
        this.guide.getNavigation().setCanSwim(true);
        IBaritone baritone = this.getBaritone();
        baritone.getPathingControlManager().registerProcess(this.pathfindingProcess = new ShellPathfindingProcess(baritone));
        baritone.getGameEventHandler().registerEventListener(new IGameEventListener() {
            @Override
            public void onTickServer() {
                // NO-OP
            }

            @Override
            public void onBlockInteract(BlockInteractEvent event) {
                // NO-OP
            }

            @Override
            public void onPathEvent(PathEvent event) {
                Brain<?> brain = PlayerShellEntity.this.getBrain();
                if (event == PathEvent.AT_GOAL) {
                    brain.forget(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                }
            }
        });
    }

    public static DefaultAttributeContainer.Builder createPlayerShellAttributes() {
        return createPlayerAttributes();
    }

    public ShellPathfindingProcess getPathfindingProcess() {
        return pathfindingProcess;
    }

    public @Nullable GlobalPos getHome() {
        return this.getBrain().getOptionalMemory(MemoryModuleType.HOME).orElse(null);
    }

    public void setHome(@Nullable GlobalPos home) {
        this.getBrain().remember(MemoryModuleType.HOME, home);
    }

    public void storePlayerData(ServerPlayerEntity player, NbtCompound respawnNbt) {
        // Save the complete representation of the player
        PlayerSplitter.performNbtCopy(respawnNbt, this);

        this.getDataTracker().set(PLAYER_MODEL_PARTS, player.getDataTracker().get(PLAYER_MODEL_PARTS));

        this.setDisplayProfile(Optional.ofNullable(Impersonator.get(player).getImpersonatedProfile()).orElse(player.getGameProfile()));
        this.setHome(GlobalPos.create(player.getServerWorld().getRegistryKey(), player.getBlockPos()));
    }

    @Override
    protected Brain.Profile<PlayerShellEntity> createBrainProfile() {
        return Brain.createProfile(MEMORY_MODULE_TYPES, SENSOR_TYPES);
    }

    @Override
    protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
        return PlayerShellBrain.create(this.createBrainProfile().deserialize(dynamic));
    }

    @Override
    public Brain<PlayerShellEntity> getBrain() {
        @SuppressWarnings("unchecked") Brain<PlayerShellEntity> b = (Brain<PlayerShellEntity>) super.getBrain();
        return b;
    }

    @Override
    protected void tickNewAi() {
        super.tickNewAi();
        if (this.shouldTickBrain()) {
            this.world.getProfiler().push("requiem:playerShellBrain");
            this.getBrain().tick(this.getServerWorld(), this);
            this.world.getProfiler().pop();
            PlayerShellBrain.refreshActivities(this);
            DebugInfoSender.sendBrainDebugData(this);   // TODO 1.17 check if we can debug this mess
        }
    }

    private boolean shouldTickBrain() {
        return false;
    }

    @Override
    public void initGoals() {
        this.guide.addGoal(2, ShellBlockGoal.blockCreepers(this));
        this.guide.addGoal(2, ShellBlockGoal.blockRangedAttackers(this));
        this.guide.addGoal(2, ShellBlockGoal.blockProjectiles(this));
        this.guide.addGoal(3, new FleeEntityGoal<>(this.guide, CreeperEntity.class, 8, 1.6, 1.4, c -> ((CreeperEntity) c).getFuseSpeed() > 0));
        this.guide.addGoal(4, new MeleeAttackGoal(this.getGuide(), 1.0D, false));
        this.guide.addGoal(5, new ShellEatGoal(this));
        this.guide.addGoal(7, new GoToWalkTargetGoal(this.guide, 1.0D));
        this.guide.addGoal(7, new LookAtEntityGoal(this.guide, PlayerEntity.class, 12.0F));
        this.guide.addGoal(7, new LookAroundGoal(this.guide));
        this.guide.addGoal(7, new LookAtEntityGoal(this.guide, LivingEntity.class, 8.0F));
        this.guide.addTargetGoal(1, new ShellRevengeGoal(this));
    }

    @Override
    protected boolean useGuide() {
        return false;
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.useGuide() && this.getHome() != null && this.getHome().getDimension() == this.world.getRegistryKey()) {
            this.guide.setPositionTarget(this.getHome().getPos(), 5);
        }
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        if (target instanceof PlayerEntity && target.getUuid().equals(this.getOwnerUuid())) {
            return false;
        }
        return super.canTarget(target);
    }

    /**
     * Applies the given player interaction to this Entity.
     */
    @Override
    public ActionResult interactAt(PlayerEntity player, Vec3d vec, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() != Items.NAME_TAG) {
            if (!this.world.isClient && !player.isSpectator()) {
                EquipmentSlot slot = MobEntity.getPreferredEquipmentSlot(stack);
                if (stack.isEmpty()) {
                    EquipmentSlot clickedSlot = this.getClickedSlot(vec);
                    if (this.hasStackEquipped(clickedSlot)) {
                        this.swapItem(player, clickedSlot, stack, hand);
                    } else {
                        return ActionResult.PASS;
                    }
                } else {
                    this.swapItem(player, slot, stack, hand);
                }
                return ActionResult.SUCCESS;
            } else {
                return stack.isEmpty() && !this.hasStackEquipped(this.getClickedSlot(vec))
                        ? ActionResult.PASS
                        : ActionResult.SUCCESS;
            }
        } else {
            return ActionResult.PASS;
        }
    }

    /**
     * Vanilla code from the armor stand
     *
     * @param rayTrace the look vector of the player
     * @return the targeted equipment slot
     */
    protected EquipmentSlot getClickedSlot(Vec3d rayTrace) {
        EquipmentSlot slot = EquipmentSlot.MAINHAND;
        boolean flag = this.isBaby();
        double d0 = (rayTrace.y) * (flag ? 2.0D : 1.0D);

        if (d0 >= 0.1D && d0 < 0.1D + (flag ? 0.8D : 0.45D) && this.hasStackEquipped(EquipmentSlot.FEET)) {
            slot = EquipmentSlot.FEET;
        } else if (d0 >= 0.9D + (flag ? 0.3D : 0.0D) && d0 < 0.9D + (flag ? 1.0D : 0.7D)
                && this.hasStackEquipped(EquipmentSlot.CHEST)) {
            slot = EquipmentSlot.CHEST;
        } else if (d0 >= 0.4D && d0 < 0.4D + (flag ? 1.0D : 0.8D) && this.hasStackEquipped(EquipmentSlot.LEGS)) {
            slot = EquipmentSlot.LEGS;
        } else if (d0 >= 1.6D && this.hasStackEquipped(EquipmentSlot.HEAD)) {
            slot = EquipmentSlot.HEAD;
        }

        return slot;
    }

    protected void swapItem(PlayerEntity player, EquipmentSlot targetedSlot, ItemStack playerItemStack,
                            Hand hand) {
        ItemStack equippedStack = this.getEquippedStack(targetedSlot);
        if (player.getAbilities().creativeMode && equippedStack.isEmpty() && !playerItemStack.isEmpty()) {
            ItemStack copy = playerItemStack.copy();
            copy.setCount(1);
            this.equipStack(targetedSlot, copy);
        } else if (!playerItemStack.isEmpty() && playerItemStack.getCount() > 1) {
            if (equippedStack.isEmpty()) {
                ItemStack copy = playerItemStack.copy();
                copy.setCount(1);
                this.equipStack(targetedSlot, copy);
                playerItemStack.decrement(1);
            }
        } else {
            this.equipStack(targetedSlot, playerItemStack);
            player.setStackInHand(hand, equippedStack);
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        UUID playerUuid = this.getOwnerUuid();

        if (playerUuid != null) {
            AttritionFocus.KEY.get(this.world.getScoreboard()).addAttrition(playerUuid, 1);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound tag) {
        super.readCustomDataFromNbt(tag);

        if (tag.contains("PlayerProfile")) {    // port from previous versions
            this.setDisplayProfile(NbtHelper.toGameProfile(tag.getCompound("PlayerProfile")));
        }

        this.getDataTracker().set(PLAYER_MODEL_PARTS, tag.getByte("PlayerModelParts"));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound tag) {
        super.writeCustomDataToNbt(tag);
        tag.putByte("PlayerModelParts", this.getDataTracker().get(PLAYER_MODEL_PARTS));
    }

    public void playSound(SoundEvent soundEvent) {
        this.playSound(soundEvent, this.getSoundVolume(), this.getSoundPitch());
    }
}
