package com.smart.phone.command;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.smart.phone.SmartPhone;
import com.smart.phone.util.SmartPhoneServerUtil;
import com.viscript_lib.register.ICommand;
import lombok.SneakyThrows;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

@LDLRegister(name = SmartPhone.MOD_ID, registry = ICommand.COMMAND_ID)
public class SmartPhoneCommand implements ICommand {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection) {
        dispatcher.register(Commands.literal(SmartPhone.MOD_ID).requires((source) -> source.hasPermission(2))
                .then(Commands.literal("open")
                        .executes(this::openPhone)
                )
                .then(Commands.literal("reload")
                        .executes(this::reload)
                )
                .then(Commands.literal("setting")
                        .executes(this::openSetting)
                )
                .then(Commands.literal("message")
                        .then(Commands.literal("send")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("title", StringArgumentType.string())
                                                .then(Commands.argument("body", StringArgumentType.greedyString())
                                                        .executes(this::sendOfficialMessage)
                                                )
                                        )
                                )
                        )
                )
        );
    }

    @SneakyThrows
    private int sendOfficialMessage(CommandContext<CommandSourceStack> context) {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        String title = StringArgumentType.getString(context, "title");
        String body = StringArgumentType.getString(context, "body");
        SmartPhoneServerUtil.sendOfficialMessage(players, title, body);
        context.getSource().sendSuccess(() -> Component.translatable("smartPhone.command.message.sent", players.size()), true);
        return players.size();
    }

    @SneakyThrows
    private int openSetting(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            SmartPhoneServerUtil.openSetting(player);
            return 1;
        } else {
            throw this.playerOnlyException();
        }
    }

    @SneakyThrows
    private int reload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            SmartPhoneServerUtil.reload(player);
            player.sendSystemMessage(Component.translatable("smartPhone.command.reload", player.getDisplayName()));
            return 1;
        } else {
            throw this.playerOnlyException();
        }
    }

    @SneakyThrows
    private int openPhone(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            SmartPhoneServerUtil.openPhone(player);
            return 1;
        } else {
            throw this.playerOnlyException();
        }
    }
}
