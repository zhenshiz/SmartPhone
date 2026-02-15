package com.smart.phone.ui.app.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.*;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.Random;

/**
 * 别踩白块儿 (Piano Tiles) UI
 */
public class PianoTilesUI extends AppUI {

    // --- 配置 ---
    private static final int LANE_COUNT = 4; // 4条轨道
    private static final int GAME_WIDTH = 80;
    private static final int GAME_HEIGHT = 120;

    // 每一行的高度
    private static final int TILE_HEIGHT = 30;
    private static final int LANE_WIDTH = GAME_WIDTH / LANE_COUNT;

    // 初始速度 (像素/tick)
    private static final float INITIAL_SPEED = 3.0f; // 稍微快一点点

    // --- 游戏状态 ---
    private final LinkedList<Row> rows = new LinkedList<>();

    private float currentSpeed = 0;
    private boolean gameStarted = false;
    private boolean gameOver = false;
    private int score = 0;
    private final Random random = new Random();

    // 颜色
    private static final int COL_BLACK = 0xFF222222;
    private static final int COL_WHITE = 0xFFEEEEEE;
    private static final int COL_START = 0xFF4CAF50; // 开始块绿色
    private static final int COL_FAIL_RED = 0xFFFF5252; // 失败红
    private static final int COL_LINE = 0xFFAAAAAA; // 线条

    // UI 组件
    private final UIElement gameCanvas;
    private final Label infoLabel;
    private final Button restartButton;

    // 行数据结构
    private static class Row {
        float y;
        int blackLane;
        boolean isStartRow;

        public Row(float y, int blackLane, boolean isStartRow) {
            this.y = y;
            this.blackLane = blackLane;
            this.isStartRow = isStartRow;
        }
    }

    public PianoTilesUI(HomeScreen homeScreen) {
        super(homeScreen);
        // 去掉默认 Padding，由我们自己控制布局
        this.layout(layout -> layout.setPadding(YogaEdge.ALL, 0));

        // 初始化 UI
        infoLabel = new Label();
        infoLabel.textStyle(style -> {
            style.fontSize(6);
            style.textColor(ColorPattern.WHITE.color);
            style.adaptiveWidth(true);
            style.adaptiveHeight(true);
        });

        gameCanvas = new GameCanvas();

        // 核心交互
        gameCanvas.addEventListener(UIEvents.CLICK, event -> {
            if (gameOver) return;
            float localX = event.x - gameCanvas.getPositionX();
            float localY = event.y - gameCanvas.getPositionY();
            handleTap(localX, localY);
        });

        restartButton = new Button();
        restartButton.setText("smartPhone.ui.app.game.resetGame");
        restartButton.textStyle(s -> s.fontSize(6).adaptiveWidth(true));
        restartButton.addEventListener(UIEvents.CLICK, e -> initGame());

        buildUI();
        initGame();
    }

    private void buildUI() {
        UIElement root = new UIElement().layout(l -> {
            l.setWidthPercent(100);
            l.setFlexDirection(YogaFlexDirection.COLUMN);
            l.setAlignItems(YogaAlign.CENTER);
            l.setJustifyContent(YogaJustify.FLEX_START);
        });

        UIElement header = new UIElement().layout(l -> {
            l.setWidthPercent(100);
            l.setAlignItems(YogaAlign.CENTER);
            l.setJustifyContent(YogaJustify.CENTER);
            l.setMargin(YogaEdge.BOTTOM, 2);
        });
        header.addChildren(infoLabel);

        // 2. 游戏区域
        UIElement canvasContainer = new UIElement().layout(l -> {
            l.setWidth(GAME_WIDTH);
            l.setHeight(GAME_HEIGHT);
            l.setAlignItems(YogaAlign.CENTER);
            l.setJustifyContent(YogaJustify.CENTER);
            l.overflow(YogaOverflow.HIDDEN);
        });
        // 加个黑边框，看起来像个游戏机
        canvasContainer.style(s -> s.backgroundTexture(new ColorBorderTexture().setColor(COL_BLACK).setBorder(1)));
        canvasContainer.addChildren(gameCanvas);

        // 3. 底部按钮区域
        UIElement footer = new UIElement().layout(l -> {
            l.setWidthPercent(100);
            l.setHeight(20);
            l.setAlignItems(YogaAlign.CENTER);
            l.setJustifyContent(YogaJustify.CENTER);
            l.setMargin(YogaEdge.TOP, 5);
        });
        footer.addChildren(restartButton);

        root.addChildren(header, canvasContainer, footer);
        appScrollView.viewContainer.addChildren(root);
    }

    private void initGame() {
        rows.clear();
        score = 0;
        currentSpeed = INITIAL_SPEED;
        gameStarted = false;
        gameOver = false;

        float startY = GAME_HEIGHT - TILE_HEIGHT;

        // Start 块
        rows.add(new Row(startY, random.nextInt(LANE_COUNT), true));

        // 填充上方
        int count = (int) Math.ceil((double) GAME_HEIGHT / TILE_HEIGHT) + 2;
        for (int i = 1; i < count; i++) {
            rows.add(new Row(startY - i * TILE_HEIGHT, random.nextInt(LANE_COUNT), false));
        }

        updateInfo();
        restartButton.setVisible(false);
    }

    private void startGame() {
        gameStarted = true;
    }

    private void handleTap(float x, float y) {
        int lane = (int) (x / LANE_WIDTH);
        if (lane < 0 || lane >= LANE_COUNT) return;

        if (rows.isEmpty()) return;
        Row targetRow = rows.getFirst();

        // 判定 Y 轴点击有效范围
        boolean yHit = y >= targetRow.y && y <= targetRow.y + TILE_HEIGHT;

        if (yHit) {
            if (lane == targetRow.blackLane) {
                onCorrectTap(targetRow);
            } else {
                setGameOver();
            }
        } else {
            // 防误触：点到了上面的黑块判输
            for (int i = 1; i < rows.size(); i++) {
                Row r = rows.get(i);
                if (y >= r.y && y <= r.y + TILE_HEIGHT && lane == r.blackLane) {
                    setGameOver();
                    return;
                }
            }
        }
    }

    private void onCorrectTap(Row row) {
        if (!gameStarted) {
            if (row.isStartRow) {
                startGame();
            } else {
                return;
            }
        }

        score++;
        updateInfo();

        if (currentSpeed < 12.0f) {
            currentSpeed += 0.1f;
        }

        rows.removeFirst();
        Row lastRow = rows.getLast();
        rows.add(new Row(lastRow.y - TILE_HEIGHT, random.nextInt(LANE_COUNT), false));
    }

    @Override
    public void screenTick() {
        super.screenTick();
        if (gameStarted && !gameOver) {
            for (Row row : rows) {
                row.y += currentSpeed;
            }

            if (!rows.isEmpty()) {
                Row bottomRow = rows.getFirst();
                if (bottomRow.y > GAME_HEIGHT) {
                    setGameOver(); // 漏过去了
                }
            }
        }
    }

    private void setGameOver() {
        gameOver = true;
        updateInfo();
        restartButton.setVisible(true);
    }

    private void updateInfo() {
        if (gameOver) {
            infoLabel.setText("smartPhone.ui.app.game.endGame");
            infoLabel.getTextStyle().textColor(ColorPattern.RED.color);
        } else {
            infoLabel.setText(Component.translatable("smartPhone.ui.app.game.score", score));
            infoLabel.getTextStyle().textColor(ColorPattern.WHITE.color);
        }
    }

    // --- 渲染 ---
    private class GameCanvas extends UIElement {
        public GameCanvas() {
            this.layout(l -> {
                l.setWidth(GAME_WIDTH);
                l.setHeight(GAME_HEIGHT);
            });
            this.style(s -> s.backgroundTexture(new ColorRectTexture(COL_WHITE)));
        }

        @Override
        public void drawBackgroundAdditional(@NotNull GUIContext guiContext) {
            super.drawBackgroundAdditional(guiContext);
            var graphics = guiContext.graphics;

            int startX = Math.round(this.getPositionX());
            int startY = Math.round(this.getPositionY());

            for (Row row : rows) {
                // 屏幕外优化
                if (row.y + TILE_HEIGHT < 0) continue;
                if (row.y > GAME_HEIGHT) continue;

                int py = startY + Math.round(row.y);
                int ph = TILE_HEIGHT;
                int px = startX + row.blackLane * LANE_WIDTH;
                int pw = LANE_WIDTH;

                int color = row.isStartRow ? COL_START : COL_BLACK;
                if (gameOver && row == rows.getFirst()) {
                    color = COL_FAIL_RED;
                }

                // 画黑块
                graphics.fill(px, py, px + pw, py + ph, color);

                // 画底部横线
                graphics.fill(startX, py + ph - 1, startX + GAME_WIDTH, py + ph, COL_LINE);
            }

            // 竖向分割线
            for (int i = 1; i < LANE_COUNT; i++) {
                int lx = startX + i * LANE_WIDTH;
                graphics.fill(lx, startY, lx + 1, startY + GAME_HEIGHT, COL_LINE);
            }
        }
    }
}