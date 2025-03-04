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

import baritone.api.fakeplayer.FakePlayers;
import ladysnake.pandemonium.common.entity.fakeplayer.FakePlayerGuide;
import ladysnake.requiem.Requiem;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.registry.Registry;

public class PandemoniumEntities {
    public static final EntityType<PlayerEntity> PLAYER_SHELL = FabricEntityTypeBuilder.<PlayerEntity>createLiving()
        .spawnGroup(SpawnGroup.MISC)
        .entityFactory(FakePlayers.entityFactory(PlayerShellEntity::new))
        .defaultAttributes(PlayerShellEntity::createPlayerShellAttributes)
        .dimensions(EntityDimensions.changing(EntityType.PLAYER.getWidth(), EntityType.PLAYER.getHeight()))
        .trackRangeBlocks(64)
        .trackedUpdateRate(1)
        .forceTrackedVelocityUpdates(true)
        .build();
    public static final EntityType<FakePlayerGuide> FAKE_PLAYER_AI = FabricEntityTypeBuilder.<FakePlayerGuide>createLiving()
        .disableSaving()
        .disableSummon()
        .dimensions(EntityDimensions.changing(PLAYER_SHELL.getWidth(), PLAYER_SHELL.getHeight()))
        .defaultAttributes(MobEntity::createMobAttributes)
        .build();
    public static final EntityType<MorticianEntity> MORTICIAN = FabricEntityTypeBuilder.createLiving()
        .spawnGroup(SpawnGroup.CREATURE)
        .entityFactory(MorticianEntity::new)
        .defaultAttributes(MorticianEntity::createMobAttributes)
        .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
        .build();

    public static void init() {
        Registry.register(Registry.ENTITY_TYPE, Requiem.id("player_shell"), PLAYER_SHELL);
        Registry.register(Registry.ENTITY_TYPE, Requiem.id("mortician"), MORTICIAN);
    }
}
