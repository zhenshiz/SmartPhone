package com.smart.phone.ui.app.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.smart.phone.SmartPhone;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaJustify;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * 扫雷游戏 UI
 */
public class MinesweeperUI extends AppUI {

    // --- 配置 ---
    private static final int COLS = 6; // 列数
    private static final int ROWS = 15; // 行数
    private static final int CELL_SIZE = 12;
    private static final int MARGIN = 0;
    private static final int MINE_COUNT = 15; // 地雷数量
    private static final int ICON_SIZE = CELL_SIZE - 4;

    private static final int BOARD_WIDTH = (CELL_SIZE + MARGIN) * COLS + MARGIN;
    private static final int BOARD_HEIGHT = (CELL_SIZE + MARGIN) * ROWS + MARGIN;

    // --- 颜色配置 ---
    private static final int COLOR_HIDDEN = 0xFF4A752C; // 未翻开的草地深绿
    private static final int COLOR_HIDDEN_ALT = 0xFF5D8B38; // 未翻开的草地浅绿（用于棋盘格效果）
    private static final int COLOR_REVEALED = 0xFFD7B899; // 翻开后的土色

    // --- 游戏状态 ---
    private final Cell[][] grid = new Cell[COLS][ROWS];
    private boolean gameOver = false;
    private boolean gameWon = false;
    private boolean firstClick = true; // 保证第一下不死
    private int flagsRemaining = MINE_COUNT;

    // UI 组件
    private final UIElement gameCanvas;
    private final Label infoLabel;
    private final Button restartButton;
    private final IGuiTexture BANNER = SpriteTexture.of(SmartPhone.formattedMod("textures/ui/banner.png"));
    private final IGuiTexture MINESWEEPER = SpriteTexture.of(SmartPhone.formattedMod("textures/ui/app/minesweeper.png"));

    // 格子数据结构
    private static class Cell {
        boolean isMine = false;
        boolean isOpen = false;
        boolean isFlagged = false;
        int neighborMines = 0;
    }

    public MinesweeperUI(HomeScreen homeScreen) {
        super(homeScreen);

        // 初始化 UI
        infoLabel = new Label();
        infoLabel.textStyle(style -> {
            style.fontSize(6);
            style.textColor(ColorPattern.WHITE.color);
            style.adaptiveWidth(true);
            style.adaptiveHeight(true);
        });

        gameCanvas = new MineCanvas();
        // 核心交互：监听点击
        gameCanvas.addEventListener(UIEvents.CLICK, event -> {
            if (gameOver || gameWon) return;

            // 计算点击的是哪个格子
            float localX = event.x - gameCanvas.getPositionX();
            float localY = event.y - gameCanvas.getPositionY();

            int col = (int) (localX / (CELL_SIZE + MARGIN));
            int row = (int) (localY / (CELL_SIZE + MARGIN));

            if (col >= 0 && col < COLS && row >= 0 && row < ROWS) {
                if (event.button == 0) { // 左键：揭开
                    handleLeftClick(col, row);
                } else if (event.button == 1) { // 右键：插旗
                    handleRightClick(col, row);
                }
            }
        });

        restartButton = new Button();
        restartButton.setText("smartPhone.ui.app.game.resetGame");
        restartButton.textStyle(s -> s.fontSize(6));
        restartButton.addEventListener(UIEvents.CLICK, e -> initGame());

        buildUI();
        initGame();
    }

    private void buildUI() {
        // 布局容器
        UIElement root = new UIElement().layout(l -> {
            l.setWidthPercent(100);
            l.setFlexDirection(YogaFlexDirection.COLUMN);
            l.setAlignItems(YogaAlign.CENTER);
            l.setJustifyContent(YogaJustify.FLEX_START);
        });

        // 顶部信息栏
        UIElement header = new UIElement().layout(l -> {
            l.setMargin(YogaEdge.VERTICAL, 2);
            l.setAlignItems(YogaAlign.CENTER);
        });
        header.addChildren(infoLabel);

        // 游戏区域
        UIElement boardContainer = new UIElement().layout(l -> {
            l.setWidth(BOARD_WIDTH);
            l.setHeight(BOARD_HEIGHT);
            l.setAlignItems(YogaAlign.CENTER);
            l.setJustifyContent(YogaJustify.CENTER);
        });
        boardContainer.addChildren(gameCanvas);

        // 底部按钮
        UIElement footer = new UIElement().layout(l -> {
            l.setMargin(YogaEdge.TOP, 10);
            l.setHeight(20);
        });
        footer.addChildren(restartButton);

        root.addChildren(header, boardContainer, footer);
        appScrollView.viewContainer.addChildren(root);
    }

    private void initGame() {
        gameOver = false;
        gameWon = false;
        firstClick = true;
        flagsRemaining = MINE_COUNT;

        // 初始化空网格
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                grid[x][y] = new Cell();
            }
        }

        updateInfo();
        restartButton.setVisible(false);
    }

    // 生成地雷（保证 startX, startY 及其周围不是雷）
    private void generateMines(int startX, int startY) {
        Random rand = new Random();
        int minesPlaced = 0;

        while (minesPlaced < MINE_COUNT) {
            int x = rand.nextInt(COLS);
            int y = rand.nextInt(ROWS);

            // 如果已经是雷，或者在起始点周围，则跳过
            if (grid[x][y].isMine) continue;
            if (Math.abs(x - startX) <= 1 && Math.abs(y - startY) <= 1) continue;

            grid[x][y].isMine = true;
            minesPlaced++;
        }

        // 计算每个格子的邻居雷数
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                if (!grid[x][y].isMine) {
                    grid[x][y].neighborMines = countNeighborMines(x, y);
                }
            }
        }
    }

    private int countNeighborMines(int cx, int cy) {
        int count = 0;
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int y = cy - 1; y <= cy + 1; y++) {
                if (x >= 0 && x < COLS && y >= 0 && y < ROWS) {
                    if (grid[x][y].isMine) count++;
                }
            }
        }
        return count;
    }

    private void handleLeftClick(int x, int y) {
        Cell cell = grid[x][y];
        if (cell.isFlagged || cell.isOpen) return;

        if (firstClick) {
            generateMines(x, y);
            firstClick = false;
        }

        if (cell.isMine) {
            triggerGameOver(false);
        } else {
            openCell(x, y); // 递归翻开
            checkWin();
        }
    }

    private void handleRightClick(int x, int y) {
        Cell cell = grid[x][y];
        if (cell.isOpen) return;

        cell.isFlagged = !cell.isFlagged;
        flagsRemaining += cell.isFlagged ? -1 : 1;
        updateInfo();
    }

    // 【核心算法】递归泛洪填充：翻开空白区域
    private void openCell(int x, int y) {
        if (x < 0 || x >= COLS || y < 0 || y >= ROWS) return;
        Cell cell = grid[x][y];

        if (cell.isOpen || cell.isFlagged) return;

        cell.isOpen = true;

        // 如果该格子周围没有雷，自动翻开周围 8 个格子
        if (cell.neighborMines == 0) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx != 0 || dy != 0) {
                        openCell(x + dx, y + dy);
                    }
                }
            }
        }
    }

    private void checkWin() {
        int coveredCount = 0;
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                if (!grid[x][y].isOpen) coveredCount++;
            }
        }
        if (coveredCount == MINE_COUNT) {
            triggerGameOver(true);
        }
    }

    private void triggerGameOver(boolean won) {
        gameOver = true;
        gameWon = won;
        restartButton.setVisible(true);

        // 游戏结束时，翻开所有雷
        if (!won) {
            for (int x = 0; x < COLS; x++) {
                for (int y = 0; y < ROWS; y++) {
                    if (grid[x][y].isMine) grid[x][y].isOpen = true;
                }
            }
        }
        updateInfo();
    }

    private void updateInfo() {
        if (gameOver) {
            infoLabel.setText(gameWon ? "smartPhone.ui.app.game.win" : "smartPhone.ui.app.game.endGame");
            infoLabel.getTextStyle().textColor(gameWon ? ColorPattern.GREEN.color : ColorPattern.RED.color);
        } else {
            infoLabel.setText(Component.translatable("smartPhone.ui.app.minesweeper.minesweeperCount", flagsRemaining));
            infoLabel.getTextStyle().textColor(ColorPattern.WHITE.color);
        }
    }

    // --- 渲染 ---
    private class MineCanvas extends UIElement {
        public MineCanvas() {
            this.layout(l -> {
                l.setWidth(BOARD_WIDTH);
                l.setHeight(BOARD_HEIGHT);
            });
            // 棋盘背景色
            this.style(s -> s.backgroundTexture(new ColorRectTexture(0xFF888888)));
        }

        @Override
        public void drawBackgroundAdditional(@NotNull GUIContext guiContext) {
            super.drawBackgroundAdditional(guiContext);
            var graphics = guiContext.graphics;
            var font = Minecraft.getInstance().font;

            int startX = Math.round(this.getPositionX());
            int startY = Math.round(this.getPositionY());

            for (int x = 0; x < COLS; x++) {
                for (int y = 0; y < ROWS; y++) {
                    Cell cell = grid[x][y];

                    int px = startX + MARGIN + x * (CELL_SIZE + MARGIN);
                    int py = startY + MARGIN + y * (CELL_SIZE + MARGIN);

                    int color;
                    if (cell.isOpen) {
                        if (cell.isMine) color = 0xFFCC0000;
                        else color = COLOR_REVEALED;
                    } else {
                        color = ((x + y) % 2 == 0) ? COLOR_HIDDEN : COLOR_HIDDEN_ALT;
                    }
                    graphics.fill(px, py, px + CELL_SIZE, py + CELL_SIZE, color);

                    // 计算居中偏移量
                    float centerX = px + CELL_SIZE / 2.0f;
                    float centerY = py + CELL_SIZE / 2.0f;

                    if (cell.isOpen && !cell.isMine && cell.neighborMines > 0) {
                        String text = String.valueOf(cell.neighborMines);
                        int numColor = getNumColor(cell.neighborMines);
                        int textW = font.width(text);
                        float scale = 0.8f;

                        graphics.pose().pushPose();
                        graphics.pose().translate(centerX, centerY, 0);
                        graphics.pose().scale(scale, scale, 1f);
                        graphics.drawString(font, text, -textW / 2f, -4, numColor, false);
                        graphics.pose().popPose();

                    } else if (cell.isFlagged) {
                        int iconOffset = (CELL_SIZE - ICON_SIZE) / 2;
                        BANNER.draw(graphics, 0, 0, px + iconOffset, py + iconOffset, ICON_SIZE, ICON_SIZE, 0);

                    } else if (cell.isOpen && cell.isMine) {
                        int mineSize = ICON_SIZE;
                        int mineOffset = (CELL_SIZE - mineSize) / 2;

                        MINESWEEPER.draw(graphics, 0, 0, px + mineOffset, py + mineOffset, mineSize, mineSize, 0);
                    }
                }
            }
        }

        private int getNumColor(int n) {
            return switch (n) {
                case 1 -> 0xFF1976D2; // 蓝
                case 2 -> 0xFF388E3C; // 绿
                case 3 -> 0xFFD32F2F; // 红
                case 4 -> 0xFF7B1FA2; // 紫
                default -> 0xFF000000;
            };
        }
    }
}