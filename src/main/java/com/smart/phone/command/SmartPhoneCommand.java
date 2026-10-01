package com.smart.phone.command;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.smart.phone.SmartPhone;
import com.smart.phone.Config;
import com.smart.phone.PhoneItem;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.util.SmartPhoneServerUtil;
import com.smart.phone.util.PhoneEditorServer;
import com.viscript_lib.register.ICommand;
import lombok.SneakyThrows;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

import java.util.Collection;
import java.util.function.Consumer;
import com.smart.phone.ui.data.PhoneInfo;

@LDLRegister(name = SmartPhone.MOD_ID, registry = ICommand.COMMAND_ID)
public class SmartPhoneCommand implements ICommand {

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection commandSelection) {
        dispatcher.register(Commands.literal(SmartPhone.MOD_ID).requires((source) -> source.hasPermission(2))
                .then(configCommands())
                .then(Commands.literal("character").then(Commands.literal("register")
                        .then(Commands.argument("name", StringArgumentType.string()).executes(this::registerCharacter))))
                .then(Commands.literal("open")
                        .executes(this::openPhone)
                )
                .then(Commands.literal("bind")
                        .then(Commands.argument("player", StringArgumentType.string())
                                .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        com.smart.phone.util.PhoneOwnerResolver.suggestions(context.getSource().getServer()), builder))
                                .executes(this::bindPhone)
                        )
                )
                .then(Commands.literal("reload")
                        .executes(this::reload)
                )
                .then(Commands.literal("editor")
                        .executes(context -> openEditor(context, null))
                        .then(Commands.argument("player", StringArgumentType.string())
                                .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        com.smart.phone.util.PhoneOwnerResolver.suggestions(context.getSource().getServer()), builder))
                                .executes(context -> openEditor(context, StringArgumentType.getString(context, "player"))))
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

    private LiteralArgumentBuilder<CommandSourceStack> configCommands() {
        return Commands.literal("config").then(Commands.argument("player", StringArgumentType.string())
                .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                        com.smart.phone.util.PhoneOwnerResolver.suggestions(context.getSource().getServer()), builder))
                .then(Commands.literal("wallpaper").then(Commands.argument("texture", ResourceLocationArgument.id())
                        .executes(ctx -> configurePhone(ctx, "wallpaper", info ->
                                info.setPhoneWallpaper(ResourceLocationArgument.getId(ctx, "texture"))))))
                .then(Commands.literal("locked").then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> configurePhone(ctx, "locked", info ->
                                info.setBlocked(BoolArgumentType.getBool(ctx, "value"))))))
                .then(Commands.literal("hide_date").then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> configurePhone(ctx, "hide_date", info ->
                                info.setHideDate(BoolArgumentType.getBool(ctx, "value"))))))
                .then(Commands.literal("hide_status_icons").then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> configurePhone(ctx, "hide_status_icons", info ->
                                info.setHideStatusIcons(BoolArgumentType.getBool(ctx, "value"))))))
                .then(Commands.literal("hide_owner_name").then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> configurePhone(ctx, "hide_owner_name", info ->
                                info.setHideOwnerName(BoolArgumentType.getBool(ctx, "value"))))))
                .then(Commands.literal("hide_lock_icon").then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> configurePhone(ctx, "hide_lock_icon", info ->
                                info.setHideLockIcon(BoolArgumentType.getBool(ctx, "value")))))));
    }

    private int configurePhone(CommandContext<CommandSourceStack> context, String setting, Consumer<PhoneInfo> update) {
        var source = context.getSource();
        String target = StringArgumentType.getString(context, "player");
        UUID owner = resolveExistingPlayer(source, target);
        if (owner == null) {
            source.sendFailure(Component.translatable("smartPhone.command.bind.unknownPlayer", target));
            return 0;
        }
        var savedData = SmartPhone.getPhoneSavedData();
        var info = savedData.getPhoneInfo(owner);
        boolean wasBlocked = info.isBlocked();
        update.accept(info);
        savedData.setPhoneInfo(owner, info);
        PhoneEditorServer.syncChanges(source.getServer(), owner, wasBlocked != info.isBlocked());
        source.sendSuccess(() -> Component.translatable("smartPhone.command.config.updated", target, setting), true);
        return 1;
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

    @SneakyThrows
    private int bindPhone(CommandContext<CommandSourceStack> context) {
        ServerPlayer holder = context.getSource().getPlayerOrException();
        var stack = holder.getMainHandItem();
        if (!stack.is(SmartPhoneRegistries.PHONE.get())) {
            context.getSource().sendFailure(Component.translatable("smartPhone.command.bind.notPhone"));
            return 0;
        }

        String input = StringArgumentType.getString(context, "player");
        UUID owner = resolveExistingPlayer(context.getSource(), input);
        if (owner == null) {
            context.getSource().sendFailure(Component.translatable("smartPhone.command.bind.unknownPlayer", input));
            return 0;
        }

        stack.set(SmartPhoneRegistries.PHONE_OWNER.get(), owner);
        stack.remove(SmartPhoneRegistries.PHONE_OWNER_NAME.get());
        PhoneItem.updateOwnerName(stack, holder.getServer());
        if (Config.HELD_PHONE_MODE.get()) SmartPhoneServerUtil.openHeldPhone(holder);
        String ownerName = stack.get(SmartPhoneRegistries.PHONE_OWNER_NAME.get());
        context.getSource().sendSuccess(() -> Component.translatable("smartPhone.command.bind.success",
                ownerName == null ? Component.translatable("smartPhone.item.phone.unknownOwner") : ownerName), true);
        return 1;
    }

    @SneakyThrows
    private int openEditor(CommandContext<CommandSourceStack> context, String target) {
        ServerPlayer editor = context.getSource().getPlayerOrException();
        UUID owner = target == null ? editor.getMainHandItem().getOrDefault(
                SmartPhoneRegistries.PHONE_OWNER.get(), editor.getUUID())
                : resolveExistingPlayer(context.getSource(), target);
        if (owner == null || !PhoneEditorServer.open(editor, owner)) {
            context.getSource().sendFailure(Component.translatable("smartPhone.command.bind.unknownPlayer",
                    target == null ? editor.getGameProfile().getName() : target));
            return 0;
        }
        return 1;
    }

    private int registerCharacter(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        var source = context.getSource();
        if (!com.smart.phone.util.PhoneOwnerResolver.validCharacterName(name)) {
            source.sendFailure(Component.translatable("smartPhone.command.character.invalid"));
            return 0;
        }
        var data = SmartPhone.getPhoneSavedData();
        if (data.getCharacters().find(name) != null) {
            source.sendFailure(Component.translatable("smartPhone.command.character.exists", name));
            return 0;
        }
        data.registerCharacter(name);
        source.sendSuccess(() -> Component.translatable("smartPhone.command.character.registered", name), true);
        return 1;
    }

    private UUID resolveExistingPlayer(CommandSourceStack source, String input) {
        return com.smart.phone.util.PhoneOwnerResolver.resolve(source.getServer(), input);
    }
}
