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
package ladysnake.requiem.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import ladysnake.requiem.api.v1.possession.PossessionComponent;
import ladysnake.requiem.api.v1.remnant.RemnantComponent;
import ladysnake.requiem.api.v1.remnant.RemnantType;
import ladysnake.requiem.common.remnant.RemnantTypes;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import net.minecraft.world.GameRules;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class RequiemCommand {

    public static final String REQUIEM_ROOT_COMMAND = "requiem";
    public static final String POSSESSION_SUBCOMMAND = "possession";
    public static final String REMNANT_SUBCOMMAND = "remnant";
    public static final String ETHEREAL_SUBCOMMAND = "soul";

    private static final Set<String> permissions = new HashSet<>();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal(REQUIEM_ROOT_COMMAND)
            .requires(RequiemCommand::checkPermissions)
            .then(literal(POSSESSION_SUBCOMMAND)
                .requires(permission("possession.start.self").or(permission("possession.stop.self")))
                // requiem possession stop [player]
                .then(literal("stop")
                    .requires(permission("possession.stop.self"))
                    .executes(context -> stopPossession(context.getSource(), Collections.singleton(context.getSource().getPlayer())))
                    .then(argument("target", EntityArgumentType.players())
                        .requires(permission("possession.stop"))
                        .executes(context -> stopPossession(context.getSource(), EntityArgumentType.getPlayers(context, "target")))
                    )
                )
                // requiem possession start <possessed> [player]
                .then(literal("start")
                    .requires(permission("possession.start.self"))
                    .then(argument("possessed", EntityArgumentType.entity())
                        .executes(context -> startPossession(context.getSource(), EntityArgumentType.getEntity(context, "possessed"), context.getSource().getPlayer()))
                        .then(argument("possessor", EntityArgumentType.player())
                            .requires(permission("possession.start"))
                            .executes(context -> startPossession(context.getSource(), EntityArgumentType.getEntity(context, "possessed"), EntityArgumentType.getPlayer(context, "possessor"))))
                    )
                )
            )
            .then(literal(REMNANT_SUBCOMMAND)
                .requires(permission("remnant.query.self").or(permission("remnant.set.self")))
                // requiem remnant query [player]
                .then(literal("query")
                    .requires(permission("remnant.query.self"))
                    .executes(context -> queryRemnant(context.getSource(), context.getSource().getPlayer()))
                    .then(argument("target", EntityArgumentType.player())
                        .requires(permission("remnant.query"))
                        .executes(context -> queryRemnant(context.getSource(), EntityArgumentType.getPlayer(context, "target")))
                    )
                )
                // requiem remnant set <true|false|identifier> [player]
                .then(literal("set")
                    .requires(permission("remnant.set.self"))
                    .then(argument("remnant_type", RemnantArgumentType.remnantType())
                        .executes(context -> setRemnant(context.getSource(), Collections.singleton(context.getSource().getPlayer()), RemnantArgumentType.getRemnantType(context, "remnant_type")))
                        .then(argument("target", EntityArgumentType.players())
                            .requires(permission("remnant.set"))
                            .executes(context -> setRemnant(context.getSource(), EntityArgumentType.getPlayers(context, "target"), RemnantArgumentType.getRemnantType(context, "remnant_type")))
                        )
                    )
                )
            )
            .then(literal(ETHEREAL_SUBCOMMAND)
                .requires(permission("soul.query.self").or(permission("soul.set.self")))
                // requiem soul query [player]
                .then(literal("query")
                    .requires(permission("soul.query.self"))
                    .executes(context -> queryEthereal(context.getSource(), context.getSource().getPlayer()))
                    .then(argument("target", EntityArgumentType.player())
                        .requires(permission("soul.query"))
                        .executes(context -> queryEthereal(context.getSource(), EntityArgumentType.getPlayer(context, "target")))
                    )
                )
                // requiem soul set <true|false> [player]
                .then(literal("set")
                    .requires(permission("soul.set.self"))
                    .then(argument("ethereal", BoolArgumentType.bool())
                        .executes(context -> setEthereal(context.getSource(), Collections.singleton(context.getSource().getPlayer()), BoolArgumentType.getBool(context, "ethereal")))
                        .then(argument("target", EntityArgumentType.players())
                            .requires(permission("soul.set"))
                            .executes(context -> setEthereal(context.getSource(), EntityArgumentType.getPlayers(context, "target"), BoolArgumentType.getBool(context, "ethereal")))
                        )
                    )
                )
            )
        );
    }

    private static Predicate<ServerCommandSource> permission(String name) {
        String perm = "requiem.command" + name;
        permissions.add(perm);
        return Permissions.require(perm, 2);
    }

    private static boolean checkPermissions(ServerCommandSource source) {
        if (source.hasPermissionLevel(2)) return true;

        for (String perm : permissions) {
            if (Permissions.check(source, perm)) {
                return true;
            }
        }
        return false;
    }

    private static int queryEthereal(ServerCommandSource source, ServerPlayerEntity player) {
        boolean remnant = RemnantComponent.get(player).isVagrant();
        Text remnantState = new TranslatableText("requiem:" + (remnant ? "ethereal" : "not_ethereal"));
        source.sendFeedback(new TranslatableText("requiem:commands.query.success." + (source.getEntity() == player ? "self" : "other"), remnantState), true);
        return remnant ? 1 : 0;
    }

    private static int setEthereal(ServerCommandSource source, Collection<ServerPlayerEntity> players, boolean ethereal) {
        int count = 0;
        for (ServerPlayerEntity player : players) {
            if (RemnantComponent.get(player).isVagrant() != ethereal) {
                if (!isRemnant(player)) {
                    throw new CommandException(new TranslatableText("requiem:commands.ethereal.set.fail.mortal", player.getDisplayName()));
                }
                if (!RemnantComponent.get(player).setVagrant(ethereal)) {
                    throw new CommandException(new TranslatableText("requiem:commands.ethereal.set.fail", player.getDisplayName()));
                }
                sendSetEtherealFeedback(source, player, ethereal);
                ++count;
            }
        }
        return count;
    }

    private static void sendSetEtherealFeedback(ServerCommandSource source, ServerPlayerEntity player, boolean ethereal) {
        Text name = new TranslatableText("requiem:" + (ethereal ? "ethereal" : "not_ethereal"));
        if (source.getEntity() == player) {
            source.sendFeedback(new TranslatableText("requiem:commands.ethereal.set.success.self", name), true);
        } else {
            if (source.getWorld().getGameRules().getBoolean(GameRules.SEND_COMMAND_FEEDBACK)) {
                player.sendSystemMessage(new TranslatableText("requiem:commands.ethereal.set.target", name), Util.NIL_UUID);
            }

            source.sendFeedback(new TranslatableText("requiem:commands.ethereal.set.success.other", player.getDisplayName(), name), true);
        }
    }

    private static int startPossession(ServerCommandSource source, Entity possessed, ServerPlayerEntity player) {
        if (!(possessed instanceof MobEntity)) {
            throw new CommandException(new TranslatableText("requiem:commands.possession.start.fail.not_mob", possessed.getDisplayName()));
        }
        if (!RemnantComponent.get(player).isIncorporeal()) {
            throw new CommandException(new TranslatableText("requiem:commands.possession.start.fail.not_incorporeal", player.getDisplayName()));
        }
        boolean success = PossessionComponent.get(player).startPossessing((MobEntity) possessed);
        if (!success) {
            throw new CommandException(new TranslatableText("requiem:commands.possession.start.fail", possessed.getDisplayName()));
        }
        TranslatableText message;
        String baseKey = "requiem:commands.possession.start.success";
        if (source.getEntity() == player) {
            message = new TranslatableText(baseKey + ".self", possessed.getDisplayName());
        } else {
            message = new TranslatableText(baseKey + ".other", player.getDisplayName(), possessed.getDisplayName());
        }
        source.sendFeedback(message, true);
        return 1;
    }

    private static int stopPossession(ServerCommandSource source, Collection<ServerPlayerEntity> players) {
        int count = 0;
        for (ServerPlayerEntity player : players) {
            PossessionComponent possessionComponent = PossessionComponent.get(player);
            if (possessionComponent.isPossessing()) {
                Entity possessed = Objects.requireNonNull(possessionComponent.getPossessedEntity());
                possessionComponent.stopPossessing();
                sendStopPossessionFeedback(source, player, possessed);
                ++count;
            }
        }
        return count;
    }

    private static void sendStopPossessionFeedback(ServerCommandSource source, ServerPlayerEntity player, Entity possessed) {
        Text name = possessed.getDisplayName();
        if (source.getEntity() == player) {
            source.sendFeedback(new TranslatableText("requiem:commands.possession.stop.success.self", name), true);
        } else {
            source.sendFeedback(new TranslatableText("requiem:commands.possession.stop.success.other", player.getDisplayName(), name), true);
        }
    }

    private static int queryRemnant(ServerCommandSource source, ServerPlayerEntity player) {
        RemnantType remnantState = RemnantComponent.get(player).getRemnantType();
        source.sendFeedback(new TranslatableText("requiem:commands.query.success." + (source.getEntity() == player ? "self" : "other"), remnantState.getName(), player.getDisplayName()), true);
        return remnantState.isDemon() ? 1 : 0;
    }

    private static int setRemnant(ServerCommandSource source, Collection<ServerPlayerEntity> players, boolean remnant) {
        return setRemnant(source, players, remnant ? RemnantTypes.REMNANT : RemnantTypes.MORTAL);
    }

    private static int setRemnant(ServerCommandSource source, Collection<ServerPlayerEntity> players, RemnantType type) {
        int count = 0;
        for (ServerPlayerEntity player : players) {
            if (RemnantComponent.get(player).getRemnantType() != type) {
                RemnantComponent.get(player).become(type);
                sendSetRemnantFeedback(source, player, type);
                ++count;
            }
        }
        return count;
    }

    private static boolean isRemnant(ServerPlayerEntity player) {
        return RemnantComponent.get(player).getRemnantType().isDemon();
    }

    private static void sendSetRemnantFeedback(ServerCommandSource source, ServerPlayerEntity player, RemnantType type) {
        if (source.getEntity() == player) {
            source.sendFeedback(new TranslatableText("requiem:commands.remnant.set.success.self", type.getName()), true);
        } else {
            if (source.getWorld().getGameRules().getBoolean(GameRules.SEND_COMMAND_FEEDBACK)) {
                player.sendSystemMessage(new TranslatableText("requiem:commands.remnant.set.target", type.getName()), Util.NIL_UUID);
            }

            source.sendFeedback(new TranslatableText("requiem:commands.remnant.set.success.other", player.getDisplayName(), type.getName()), true);
        }
    }
}
