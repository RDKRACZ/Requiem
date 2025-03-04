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
package ladysnake.pandemonium.common.entity.effect;

import com.mojang.authlib.GameProfile;
import ladysnake.pandemonium.common.PlayerSplitter;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public class PenanceStatusEffect extends StatusEffect {
    protected PenanceStatusEffect(StatusEffectType type, int color) {
        super(type, color);
    }

    @Override
    public void onApplied(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        super.onApplied(entity, attributes, amplifier);
        if (amplifier >= 1 && entity instanceof ServerPlayerEntity) { // level 2+
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            if (amplifier >= 2) { // level 3+
	            PossessionComponent.get(player).stopPossessing();
	        }
            RemnantComponent remnant = RemnantComponent.get(player);
            if (!remnant.isVagrant()) {
                if (remnant.getRemnantType().isDemon()) {
                    PlayerSplitter.split(player);
                } else {
                    player.damage(DamageSource.MAGIC, amplifier*4);
                }
            }
        }
    }

    public static int getLevel(PlayerEntity player) {
        StatusEffectInstance penance = player.getStatusEffect(PandemoniumStatusEffects.PENANCE);
        return penance == null ? -1 : penance.getAmplifier();
    }

    public static boolean canMerge(PlayerEntity possessor, PlayerEntity target, GameProfile shellProfile) {
        StatusEffectInstance penance = possessor.getStatusEffect(PandemoniumStatusEffects.PENANCE);
        return penance == null || penance.getAmplifier() < 1;
    }
}
