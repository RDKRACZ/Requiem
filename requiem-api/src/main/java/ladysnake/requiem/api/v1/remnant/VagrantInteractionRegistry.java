/*
 * Requiem
 * Copyright (C) 2017-2021 Ladysnake
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; If not, see <https://www.gnu.org/licenses>.
 */
package ladysnake.requiem.api.v1.remnant;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.apiguardian.api.API;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

@API(status = API.Status.EXPERIMENTAL)
public interface VagrantInteractionRegistry {
    <E extends LivingEntity> void registerPossessionInteraction(Class<E> targetType, BiPredicate<E, PlayerEntity> precondition, BiConsumer<E, PlayerEntity> action);
}
