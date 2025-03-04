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
package ladysnake.requiem.mixin.common.possession.gameplay;

import ladysnake.requiem.api.v1.event.minecraft.MobTravelRidingCallback;
import ladysnake.requiem.api.v1.possession.Possessable;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntityMixin implements Possessable {
    @Shadow
    public abstract boolean canBeControlledByRider();

    @Shadow
    public abstract void setMovementSpeed(float movementSpeed);

    public MobEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    protected Vec3d requiem$travelStart(Vec3d movementInput) {
        // Straight up copied from HorseBaseEntity#travel, minus the jumping code
        if (this.isAlive() && this.hasPassengers() && this.canBeControlledByRider()) {
            LivingEntity livingEntity = (LivingEntity) this.getPrimaryPassenger();
            assert livingEntity != null;

            if (!MobTravelRidingCallback.EVENT.invoker().canBeControlled((MobEntity) (Object) this, livingEntity)) {
                return movementInput;
            }

            this.setYaw(livingEntity.getYaw());
            this.prevYaw = this.getYaw();
            this.setPitch(livingEntity.getPitch() * 0.5F);
            this.setRotation(this.getYaw(), this.getPitch());
            this.bodyYaw = this.getYaw();
            this.headYaw = this.bodyYaw;
            float sidewaysSpeed = livingEntity.sidewaysSpeed * 0.5F;
            float forwardSpeed = livingEntity.forwardSpeed;
            if (forwardSpeed <= 0.0F) {
                forwardSpeed *= 0.25F;
            }

            this.flyingSpeed = this.getMovementSpeed() * 0.1F;
            if (this.isLogicalSideForUpdatingMovement()) {
                this.setMovementSpeed((float) this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
                return new Vec3d(sidewaysSpeed, movementInput.y, forwardSpeed);
            } else if (livingEntity instanceof PlayerEntity) {
                this.setVelocity(Vec3d.ZERO);
            }
        }
        return movementInput;
    }

    @Override
    protected void requiem$travelEnd(Vec3d movementInput, CallbackInfo ci) {
        this.updateLimbs((LivingEntity) (Object) this, false);
    }
}
