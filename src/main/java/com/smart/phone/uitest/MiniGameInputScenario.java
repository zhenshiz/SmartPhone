package com.smart.phone.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.*;
import com.smart.phone.ClientConfig;
import com.smart.phone.SmartPhone;
import com.smart.phone.SmartPhoneRegistries;
import com.smart.phone.ui.HeldPhoneScreen;
import com.smart.phone.ui.PhoneUI;
import com.smart.phone.ui.app.Minesweeper;
import com.smart.phone.ui.app.PianoTilesGame;
import com.smart.phone.ui.data.PhoneInfo;
import com.smart.phone.ui.editor.PhoneEditorUI;
import com.smart.phone.util.SmartPhoneClientUtil;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

@LDLRegisterClient(name = "mini_game_input", group = SmartPhone.MOD_ID,
        registry = UIScenario.REGISTRY, environment = RegistrationEnvironment.DEV_ONLY)
public final class MiniGameInputScenario implements UIScenario {
    @Override public void configure(ScenarioOptions options) {
        options.guiScale(3).defaultTimeoutMs(10_000).tags("phone", "games", "input");
    }

    @Override public void define(ScenarioBuilder s) {
        s.step("remember UI preferences", ctx -> {
            ctx.put("editorScale", ClientConfig.EDITOR_GUI_SCALE.get());
            ctx.put("guiScale", ctx.mc().options.guiScale().get());
        });
        for (String mode : List.of("held", "standalone", "editor")) {
            for (int scale : new int[]{2, 4}) {
                String variant = mode + "_" + scale;
                s.setHeldItem(ItemStack.EMPTY).closeScreen().step("configure " + variant, ctx -> {
                    ctx.mc().options.guiScale().set(scale);
                    ctx.mc().resizeDisplay();
                    ClientConfig.EDITOR_GUI_SCALE.set(scale);
                });
                if (mode.equals("held")) {
                    s.setHeldItem(SmartPhoneRegistries.PHONE.get().getDefaultInstance())
                            .waitUntil("held phone is ready", ctx -> ctx.screen() instanceof HeldPhoneScreen h && h.isSynced() && h.isRaised())
                            .step("unlock held phone", ctx -> {
                                PhoneUI phone = phone(ctx);
                                phone.screenContainer.removeChild(phone.lockScreen);
                            });
                } else if (mode.equals("editor")) {
                    s.step("open editor preview", ctx -> PhoneEditorUI.open(ctx.mc().player.getUUID(), "MiniGameTest",
                                    UUID.randomUUID(), new PhoneInfo(), false))
                            .awaitElement("#phone_editor")
                            .step("unlock editor preview", ctx -> phone(ctx).lockScreen.showEditorContent());
                } else {
                    s.step("open standalone phone", ctx -> SmartPhoneClientUtil.openUnlockedPhone(new PhoneInfo()));
                }
                s.step("open minesweeper " + variant, ctx -> phone(ctx).homeScreen.openApp(new Minesweeper()))
                        .awaitElement("#minesweeper_canvas")
                        .step("right click flags the selected cell", ctx -> tap(ctx, "#minesweeper_canvas", 18, 18, 1))
                        .check("flag changes the correct cell and remaining count", ctx ->
                                flag(cell(ctx, 1, 1), "isFlagged") && number(game(ctx), "flagsRemaining") == 14)
                        .step("flagged cell ignores left click", ctx -> tap(ctx, "#minesweeper_canvas", 18, 18, 0))
                        .check("flagged click does not begin the game", ctx -> flag(game(ctx), "firstClick"))
                        .step("right click removes flag", ctx -> tap(ctx, "#minesweeper_canvas", 18, 18, 1))
                        .check("flag is removed", ctx -> !flag(cell(ctx, 1, 1), "isFlagged") && number(game(ctx), "flagsRemaining") == 15)
                        .step("left click starts minesweeper", ctx -> tap(ctx, "#minesweeper_canvas", 6, 6, 0))
                        .check("first click opens the clicked cell safely", ctx -> !flag(game(ctx), "firstClick")
                                && flag(cell(ctx, 0, 0), "isOpen") && !flag(game(ctx), "gameOver"))
                        .screenshot("minesweeper_started_" + variant)
                        .step("click a mine through the board", ctx -> {
                            for (int row = 0; row < 15; row++) for (int col = 0; col < 6; col++) {
                                if (flag(cell(ctx, col, row), "isMine")) {
                                    tap(ctx, "#minesweeper_canvas", col * 12 + 6, row * 12 + 6, 0);
                                    return;
                                }
                            }
                            throw new AssertionError("No mine was generated");
                        }).check("mine ends the game", ctx -> flag(game(ctx), "gameOver"))
                        .repeat(14, scroll -> scroll.scroll("#minesweeper_canvas", -1)).ticks(3)
                        .click("#minesweeper_restart")
                        .check("minesweeper restarts", ctx -> flag(game(ctx), "firstClick") && !flag(game(ctx), "gameOver"))
                        .step("start by clicking the bottom row after scrolling", ctx -> tap(ctx, "#minesweeper_canvas", 66, 174, 0))
                        .check("scrolling preserves cell coordinates", ctx -> flag(cell(ctx, 5, 14), "isOpen") && !flag(game(ctx), "gameOver"))
                        .step("open piano tiles " + variant, ctx -> phone(ctx).homeScreen.openApp(new PianoTilesGame()))
                        .awaitElement("#piano_tiles_canvas")
                        .step("click the green start tile", ctx -> tapPiano(ctx, false, 0))
                        .check("green tile starts the game and scores", ctx -> flag(game(ctx), "gameStarted")
                                && !flag(game(ctx), "gameOver") && number(game(ctx), "score") == 1)
                        .step("click the next black tile", ctx -> tapPiano(ctx, false, 0))
                        .check("second tile scores", ctx -> !flag(game(ctx), "gameOver") && number(game(ctx), "score") == 2)
                        .screenshot("piano_started_" + variant)
                        .step("click a white tile", ctx -> tapPiano(ctx, true, 0))
                        .check("white tile ends the game at the clicked lane", ctx -> {
                            Object failedRow = read(game(ctx), "failedRow");
                            return flag(game(ctx), "gameOver") && failedRow == ((List<?>)read(game(ctx), "rows")).getFirst()
                                    && number(game(ctx), "failedLane") != number(failedRow, "blackLane");
                        })
                        .click("#piano_tiles_restart")
                        .check("piano restarts", ctx -> !flag(game(ctx), "gameStarted") && !flag(game(ctx), "gameOver")
                                && number(game(ctx), "score") == 0)
                        .step("start again after reset", ctx -> tapPiano(ctx, false, 0))
                        .check("reset board remains interactive", ctx -> flag(game(ctx), "gameStarted") && number(game(ctx), "score") == 1);
                if (mode.equals("held") && scale == 2) {
                    s.step("tap a later black tile", ctx -> {
                                Object upper = ((List<?>)read(game(ctx), "rows")).get(1);
                                tap(ctx, "#piano_tiles_canvas", number(upper, "blackLane") * 20 + 10,
                                        ((Number)read(upper, "y")).floatValue() + 15, 0);
                            }).check("later black tile does not cause an early failure", ctx ->
                                    !flag(game(ctx), "gameOver") && number(game(ctx), "score") == 1)
                            .waitUntil("unclicked black tile reaches the bottom", ctx -> flag(game(ctx), "gameOver"))
                            .check("missed tile stops exactly at the bottom line and turns red", ctx -> {
                                Object bottom = ((List<?>)read(game(ctx), "rows")).getFirst();
                                return read(game(ctx), "failedRow") == bottom
                                        && number(game(ctx), "failedLane") == number(bottom, "blackLane")
                                        && Math.abs(((Number)read(bottom, "y")).floatValue() + 30 - 120) < 0.001f;
                            }).screenshot("piano_bottom_line_failure");
                }
            }
        }
        s.setHeldItem(ItemStack.EMPTY).teardown("restore preferences", ctx -> {
            ctx.mc().setScreen(null);
            if (ctx.get("editorScale") != null) ClientConfig.EDITOR_GUI_SCALE.set((Integer)ctx.get("editorScale"));
            if (ctx.get("guiScale") != null) ctx.mc().options.guiScale().set((Integer)ctx.get("guiScale"));
            ctx.mc().resizeDisplay();
        });
    }

    private static PhoneUI phone(TestContext ctx) {
        UIElement root = ctx.requireUI().ui.rootElement;
        return root instanceof PhoneEditorUI editor ? editor.getPreview() : (PhoneUI)root;
    }

    private static Object game(TestContext ctx) { return phone(ctx).homeScreen.appUI; }

    private static Object cell(TestContext ctx, int col, int row) {
        return ((Object[][])read(game(ctx), "grid"))[col][row];
    }

    private static boolean flag(Object object, String field) { return (Boolean)read(object, field); }
    private static int number(Object object, String field) { return ((Number)read(object, field)).intValue(); }

    private static Object read(Object object, String name) {
        try {
            var field = object.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(object);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void tapPiano(TestContext ctx, boolean white, int button) {
        Object row = ((List<?>)read(game(ctx), "rows")).getFirst();
        int lane = (number(row, "blackLane") + (white ? 1 : 0)) % 4;
        float y = ((Number)read(row, "y")).floatValue();
        tap(ctx, "#piano_tiles_canvas", lane * 20 + 10, y + 15, button);
    }

    private static void tap(TestContext ctx, String selector, float x, float y, int button) {
        UIElement canvas = ctx.el(selector).element();
        var point = canvas.getWorldMouse(canvas.getPositionX() + x, canvas.getPositionY() + y);
        ctx.require("rendered canvas receives the click", ctx.requireUI().hitTestAtScreen(point.x, point.y) == canvas);
        ctx.input().mouseDown(point.x, point.y, button);
        ctx.input().mouseUp(point.x, point.y, button);
    }
}
