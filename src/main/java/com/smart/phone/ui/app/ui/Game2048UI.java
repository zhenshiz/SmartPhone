package com.smart.phone.ui.app.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.smart.phone.ui.app.ui.game.Direction;
import com.smart.phone.ui.app.ui.game.Point;
import com.smart.phone.ui.view.HomeScreen;
import com.smart.phone.util.UIElementUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaJustify;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 2048 游戏 UI
 */
public class Game2048UI extends AppUI {
    // --- 游戏配置 ---
    private static final int GRID_SIZE = 4;      // 4x4 网格
    private static final int TILE_SIZE = 18;
    private static final int TILE_MARGIN = 2;
    private static final int BOARD_SIZE = (TILE_SIZE + TILE_MARGIN) * GRID_SIZE + TILE_MARGIN;

    // --- 游戏状态 ---
    private int[][] board = new int[GRID_SIZE][GRID_SIZE];
    private int score = 0;
    private boolean gameOver = false;
    private final Random random = new Random();

    // --- UI 引用 ---
    private final UIElement gameCanvas;
    private final Label scoreLabel;
    private final Button restartButton;

    public Game2048UI(HomeScreen homeScreen) {
        super(homeScreen);

        this.setFocusable(true);
        this.addEventListener(UIEvents.KEY_DOWN, event -> {
            if (gameOver) return;
            boolean moved = false;
            switch (event.keyCode) {
                case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> moved = move(Direction.UP);
                case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> moved = move(Direction.DOWN);
                case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> moved = move(Direction.LEFT);
                case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> moved = move(Direction.RIGHT);
                case GLFW.GLFW_KEY_R -> restartGame();
            }
            if (moved) {
                spawnRandomTile();
                checkGameState();
            }
        });

        // 2. 游戏主画布
        gameCanvas = new GridCanvas();

        // 3. 分数显示
        scoreLabel = new Label();
        scoreLabel.textStyle(style -> {
            style.fontSize(6);
            style.textColor(ColorPattern.WHITE.color);
            style.adaptiveWidth(true);
            style.adaptiveHeight(true);
        });

        // 4. 重开按钮
        restartButton = new Button();
        restartButton.setText("smartPhone.ui.app.game.resetGame");
        restartButton.textStyle(style -> style.fontSize(6));
        restartButton.addEventListener(UIEvents.CLICK, e -> restartGame());

        // 5. 构建布局
        buildUI();

        // 6. 初始化
        restartGame();
    }

    private void buildUI() {
        // --- 顶部标题栏 ---
        UIElement header = new UIElement().layout(l -> {
            l.setWidthPercent(100);
            l.setFlexDirection(YogaFlexDirection.COLUMN);
            l.setAlignItems(YogaAlign.CENTER);
            l.setJustifyContent(YogaJustify.CENTER);
            l.setMargin(YogaEdge.VERTICAL, 2);
        });

        header.addChildren(scoreLabel);

        // --- 游戏画布容器 ---
        UIElement canvasContainer = new UIElement().layout(l -> {
            l.setWidthPercent(100);
            l.setHeight(BOARD_SIZE + 10);
            l.setAlignItems(YogaAlign.CENTER);
            l.setJustifyContent(YogaJustify.CENTER);
        });
        canvasContainer.addChildren(gameCanvas);

        // --- 底部控制区
        UIElement controls = UIElementUtil.createControlButtons(dir -> {
            if (!gameOver && move(dir)) {
                spawnRandomTile();
                checkGameState();
            }
        });

        // --- 底部重开按钮 ---
        UIElement footer = new UIElement().layout(l -> {
            l.setWidthPercent(100);
            l.setHeight(20);
            l.setAlignItems(YogaAlign.CENTER);
            l.setJustifyContent(YogaJustify.CENTER);
        });
        footer.addChildren(restartButton);

        appScrollView.viewContainer.addChildren(header, canvasContainer, controls, footer);
    }

    // --- 游戏逻辑 ---

    private void restartGame() {
        board = new int[GRID_SIZE][GRID_SIZE];
        score = 0;
        gameOver = false;
        spawnRandomTile();
        spawnRandomTile();
        updateScore();
    }

    private void spawnRandomTile() {
        List<Point> emptySpots = new ArrayList<>();
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (board[i][j] == 0) emptySpots.add(new Point(i, j));
            }
        }
        if (!emptySpots.isEmpty()) {
            Point p = emptySpots.get(random.nextInt(emptySpots.size()));
            board[p.x()][p.y()] = (random.nextFloat() < 0.9f) ? 2 : 4;
        }
    }

    // 核心移动逻辑：对每一行/列进行 压缩(Compress) 和 合并(Merge)
    private boolean move(Direction dir) {
        boolean moved = false;

        for (int i = 0; i < GRID_SIZE; i++) {
            // 提取一维数组 (行或列)
            int[] line = new int[GRID_SIZE];
            for (int j = 0; j < GRID_SIZE; j++) {
                // 根据方向提取数据
                if (dir == Direction.LEFT || dir == Direction.RIGHT) line[j] = board[i][j];
                else line[j] = board[j][i];
            }

            // 如果是向右或向下，需要先反转数组处理，处理完再反转回来
            boolean reverse = (dir == Direction.RIGHT || dir == Direction.DOWN);

            // 执行合并逻辑
            int[] newLine = compressAndMerge(line, reverse);

            // 写回并检查是否有变动
            for (int j = 0; j < GRID_SIZE; j++) {
                int val = newLine[j];
                int oldVal = (dir == Direction.LEFT || dir == Direction.RIGHT) ? board[i][j] : board[j][i];
                if (val != oldVal) moved = true;

                if (dir == Direction.LEFT || dir == Direction.RIGHT) board[i][j] = val;
                else board[j][i] = val;
            }
        }
        return moved;
    }

    private int[] compressAndMerge(int[] line, boolean reverse) {
        // 1. 去零并根据方向处理
        List<Integer> list = new ArrayList<>();
        for (int val : line) if (val != 0) list.add(val);
        if (reverse) java.util.Collections.reverse(list);

        // 2. 合并相同项
        List<Integer> merged = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            int curr = list.get(i);
            if (i < list.size() - 1 && curr == list.get(i + 1)) {
                int newVal = curr * 2;
                merged.add(newVal);
                score += newVal;
                i++; // 跳过下一个
            } else {
                merged.add(curr);
            }
        }

        // 3. 补齐 0 并还原数组顺序
        int[] result = new int[GRID_SIZE];
        if (reverse) {
            // 如果是反向操作 (Right/Down)，填充在数组右侧 (高位)
            for (int i = 0; i < merged.size(); i++) {
                result[GRID_SIZE - 1 - i] = merged.get(i);
            }
        } else {
            // 正向操作 (Left/Up)，填充在数组左侧
            for (int i = 0; i < merged.size(); i++) {
                result[i] = merged.get(i);
            }
        }
        return result;
    }

    private void checkGameState() {
        updateScore();

        // 检查是否有空格
        for (int[] row : board) {
            for (int val : row) {
                if (val == 0) return; // 还有空位，没输
            }
        }

        // 检查四周能否合并
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                int val = board[i][j];
                if (j < GRID_SIZE - 1 && board[i][j + 1] == val) return;
                if (i < GRID_SIZE - 1 && board[i + 1][j] == val) return;
            }
        }

        gameOver = true;
    }

    private void updateScore() {
        scoreLabel.setText(Component.translatable("smartPhone.ui.app.game.score", score));
    }

    // --- 渲染画布 ---
    private class GridCanvas extends UIElement {
        public GridCanvas() {
            this.layout(l -> {
                l.setWidth(BOARD_SIZE);
                l.setHeight(BOARD_SIZE);
            });
            this.style(s -> s.backgroundTexture(new ColorRectTexture(0xFFBBADA0)));
        }

        @Override
        public void drawBackgroundAdditional(@NotNull GUIContext guiContext) {
            super.drawBackgroundAdditional(guiContext);
            var graphics = guiContext.graphics;
            var font = Minecraft.getInstance().font;

            int startX = Math.round(this.getPositionX());
            int startY = Math.round(this.getPositionY());

            for (int i = 0; i < GRID_SIZE; i++) {
                for (int j = 0; j < GRID_SIZE; j++) {
                    int val = board[i][j];

                    // 计算当前方块的坐标 (全部用 int 计算，保证严丝合缝)
                    int x = startX + TILE_MARGIN + j * (TILE_SIZE + TILE_MARGIN);
                    int y = startY + TILE_MARGIN + i * (TILE_SIZE + TILE_MARGIN);

                    // 绘制方块背景
                    int color = getTileColor(val);
                    graphics.fill(x, y, x + TILE_SIZE, y + TILE_SIZE, color);

                    // 绘制数字
                    if (val > 0) {
                        String text = String.valueOf(val);
                        int textColor = (val <= 4) ? 0xFF776E65 : 0xFFF9F6F2;
                        int textW = font.width(text);
                        float scale = (val > 1000) ? 0.6f : (val > 100) ? 0.8f : 1.0f;
                        graphics.pose().pushPose();
                        graphics.pose().translate(x + TILE_SIZE / 2f, y + TILE_SIZE / 2f, 0);
                        graphics.pose().scale(scale, scale, 1f);
                        graphics.drawString(font, text, -textW / 2, -4, textColor, false);
                        graphics.pose().popPose();
                    }
                }
            }

            // 游戏结束遮罩
            if (gameOver) {
                graphics.fill(startX, startY, startX + BOARD_SIZE, startY + BOARD_SIZE, 0x88000000);

                String over = Component.translatable("smartPhone.ui.app.game.endGame").getString();
                int textW = font.width(over);
                graphics.drawString(font, over, startX + (BOARD_SIZE - textW) / 2, startY + (BOARD_SIZE - 8) / 2, ColorPattern.RED.color);
            }
        }

        private int getTileColor(int val) {
            return switch (val) {
                case 0 -> 0xFFCDC1B4;
                case 2 -> 0xFFEEE4DA;
                case 4 -> 0xFFEDE0C8;
                case 8 -> 0xFFF2B179;
                case 16 -> 0xFFF59563;
                case 32 -> 0xFFF67C5F;
                case 64 -> 0xFFF65E3B;
                case 128 -> 0xFFEDCF72;
                case 256 -> 0xFFEDCC61;
                case 512 -> 0xFFEDC850;
                case 1024 -> 0xFFEDC53F;
                case 2048 -> 0xFFEDC22E;
                default -> 0xFF3C3A32;
            };
        }
    }
}