package com.smart.phone.command;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.smart.phone.SmartPhone;
import com.smart.phone.util.SmartPhoneServerUtil;
import lombok.SneakyThrows;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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
        );
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
