package com.smart.phone.ui.app.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.smart.phone.ui.view.HomeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.*;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Flappy Bird 游戏 UI
 */
public class FlappyBirdUI extends AppUI {
    // 游戏配置
    private static final int GAME_WIDTH = 100;  // 虚拟画布宽度
    private static final int GAME_HEIGHT = 100; // 虚拟画布高度

    private static final float GRAVITY = 0.15f; // 重力加速度
    private static final float JUMP_STRENGTH = -1.5f; // 跳跃力度 (负数向上)
    private static final float PIPE_SPEED = 1.5f; // 管道移动速度
    private static final int PIPE_SPAWN_INTERVAL = 1400; // 管道生成间隔(ms)
    private static final int PIPE_GAP_SIZE = 45; // 管道中间缝隙大小
    private static final int PIPE_WIDTH = 26; // 管道宽度
    private static final int BIRD_SIZE = 12; // 鸟的大小

    // --- 游戏状态 ---
    private float birdY = GAME_HEIGHT / 2.0f;
    private float birdVelocity = 0;

    private int score = 0;
    private boolean gameOver = false;
    private boolean gameStarted = false;
    private long lastPipeSpawnTime = 0;
    private final Random random = new Random();

    // --- UI 引用 ---
    private final UIElement gameCanvas;
    private final Label scoreLabel;
    private final Button restartButton;
    private final Button startButton;

    // 可变管道类 (为了方便移动)
    private static class MovingPipe {
        float x;
        int gapY;
        boolean passed; // 是否已经计分

        public MovingPipe(float x, int gapY) {
            this.x = x;
            this.gapY = gapY;
            this.passed = false;
        }
    }

    private final List<MovingPipe> activePipes = new ArrayList<>();

    public FlappyBirdUI(HomeScreen homeScreen) {
        super(homeScreen);

        this.setFocusable(true);

        // 键盘事件：空格或上键跳跃
        this.addEventListener(UIEvents.KEY_DOWN, event -> {
            switch (event.keyCode) {
                case GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> doAction();
            }
        });

        // 画布点击事件：点击屏幕也可以跳跃
        this.gameCanvas = new GameCanvas();
        this.gameCanvas.addEventListener(UIEvents.CLICK, event -> doAction());

        // 分数标签
        scoreLabel = new Label();
        scoreLabel.textStyle(style -> {
            style.fontSize(6);
            style.textColor(ColorPattern.WHITE.color);
            style.adaptiveWidth(true).adaptiveHeight(true);
        });
        updateScore();

        // 开始按钮
        startButton = (Button) new Button().setText("smartPhone.ui.app.game.startGame")
                .textStyle(textStyle -> textStyle.fontSize(6))
                .layout(layout -> {
                    layout.setPositionType(YogaPositionType.ABSOLUTE);
                    layout.top(3);
                }).addEventListener(UIEvents.CLICK, event -> startGame());

        // 重开按钮
        restartButton = (Button) new Button().setText("smartPhone.ui.app.game.resetGame")
                .textStyle(textStyle -> textStyle.fontSize(6))
                .layout(layout -> {
                    layout.setPositionType(YogaPositionType.ABSOLUTE);
                    layout.top(3);
                }).addEventListener(UIEvents.CLICK, event -> startGame());

        buildUI();
        initializeGame();
    }

    private void doAction() {
        if (!gameStarted) {
            startGame();
        } else if (!gameOver) {
            jump();
        }
    }

    private void buildUI() {
        // 顶部 Header
        UIElement header = new UIElement().layout(l -> {
            l.setWidthPercent(100);
            l.setFlexDirection(YogaFlexDirection.COLUMN);
            l.setAlignItems(YogaAlign.CENTER);
            l.setJustifyContent(YogaJustify.CENTER);
            l.setMargin(YogaEdge.VERTICAL, 2);
        });
        header.addChildren(scoreLabel);

        // 游戏区域容器
        UIElement canvasContainer = new UIElement().layout(l -> {
            l.setWidthPercent(100);
            l.setHeight(GAME_HEIGHT);
            l.setAlignItems(YogaAlign.CENTER);
            l.setJustifyContent(YogaJustify.CENTER);
            l.setMargin(YogaEdge.VERTICAL, 5);
        });
        canvasContainer.addChildren(gameCanvas);

        // 按钮容器
        UIElement buttonContainer = new UIElement().layout(l -> {
            l.setWidthPercent(100);
            l.setHeight(20);
            l.setAlignItems(YogaAlign.CENTER);
            l.setJustifyContent(YogaJustify.CENTER);
        });
        buttonContainer.addChildren(startButton, restartButton);

        appScrollView.viewContainer.addChildren(header, canvasContainer, buttonContainer);
    }

    private void initializeGame() {
        birdY = GAME_HEIGHT / 2.0f;
        birdVelocity = 0;
        activePipes.clear();
        score = 0;
        gameOver = false;
        gameStarted = false;

        updateScore();
        startButton.setVisible(true);
        restartButton.setVisible(false);
    }

    private void startGame() {
        if (gameOver) {
            initializeGame();
        }
        gameStarted = true;
        lastPipeSpawnTime = System.currentTimeMillis();
        jump(); // 开始时自动跳一下

        startButton.setVisible(false);
        restartButton.setVisible(false);
    }

    private void jump() {
        birdVelocity = JUMP_STRENGTH;
    }

    private void gameOver() {
        gameOver = true;
        restartButton.setVisible(true);
    }

    private void updateScore() {
        scoreLabel.setText(Component.translatable("smartPhone.ui.app.game.score", score));
    }

    @Override
    public void screenTick() {
        super.screenTick();

        if (gameStarted && !gameOver) {
            // 1. 更新鸟的物理位置
            birdVelocity += GRAVITY;
            birdY += birdVelocity;

            // 2. 生成管道
            long now = System.currentTimeMillis();
            if (now - lastPipeSpawnTime > PIPE_SPAWN_INTERVAL) {
                spawnPipe();
                lastPipeSpawnTime = now;
            }

            // 3. 更新管道位置 & 碰撞检测 & 计分
            Iterator<MovingPipe> iterator = activePipes.iterator();
            while (iterator.hasNext()) {
                MovingPipe pipe = iterator.next();
                pipe.x -= PIPE_SPEED;

                // 移除超出屏幕的管道
                if (pipe.x + PIPE_WIDTH < 0) {
                    iterator.remove();
                    continue;
                }

                // 计分 (当鸟通过管道后)
                if (!pipe.passed && pipe.x + PIPE_WIDTH < (GAME_WIDTH - BIRD_SIZE) / 2.0f) {
                    score++;
                    updateScore();
                    pipe.passed = true;
                }

                // 管道碰撞检测
                if (checkPipeCollision(pipe)) {
                    gameOver();
                    return;
                }
            }

            // 4. 地面/天花板碰撞检测
            if (birdY < 0 || birdY + BIRD_SIZE > GAME_HEIGHT) {
                gameOver();
            }
        }
    }

    private void spawnPipe() {
        // 缝隙的 Y 坐标随机范围
        // 留出上下边距，防止缝隙太靠边
        int minGapY = 20;
        int maxGapY = GAME_HEIGHT - 20 - PIPE_GAP_SIZE;
        int gapY = minGapY + random.nextInt(maxGapY - minGapY);

        activePipes.add(new MovingPipe(GAME_WIDTH, gapY));
    }

    private boolean checkPipeCollision(MovingPipe pipe) {
        // 鸟的矩形
        float birdLeft = (GAME_WIDTH - BIRD_SIZE) / 2.0f; // 鸟水平居中
        float birdRight = birdLeft + BIRD_SIZE;
        float birdTop = birdY;
        float birdBottom = birdY + BIRD_SIZE;

        // 管道矩形
        float pipeLeft = pipe.x;
        float pipeRight = pipe.x + PIPE_WIDTH;

        // 横向是否有交集
        boolean xOverlap = birdRight > pipeLeft && birdLeft < pipeRight;

        if (xOverlap) {
            // 纵向检测：是否撞到了上管子 OR 下管子
            // 缝隙区间是 [gapY, gapY + PIPE_GAP_SIZE]
            // 如果鸟不在缝隙里，就是撞了
            boolean inGap = birdTop > pipe.gapY && birdBottom < (pipe.gapY + PIPE_GAP_SIZE);
            return !inGap;
        }

        return false;
    }

    // --- 渲染画布 ---
    private class GameCanvas extends UIElement {
        public GameCanvas() {
            this.layout(l -> {
                l.setWidth(GAME_WIDTH);
                l.setHeight(GAME_HEIGHT);
            });
            // 天空蓝背景
            this.style(s -> s.backgroundTexture(new ColorRectTexture(0xFF70C5CE)));
        }

        @Override
        public void drawBackgroundAdditional(@NotNull GUIContext guiContext) {
            super.drawBackgroundAdditional(guiContext);
            var graphics = guiContext.graphics;
            var font = Minecraft.getInstance().font;

            // 获取绝对坐标
            int startX = Math.round(this.getPositionX());
            int startY = Math.round(this.getPositionY());

            // 1. 绘制管道
            // 0xFF73BF2E 是绿色
            int pipeColor = 0xFF73BF2E;
            int pipeBorder = 0xFF558C22; // 深绿色边框

            for (MovingPipe pipe : activePipes) {
                int px = startX + Math.round(pipe.x);
                int py = startY;

                // 上管道 (从 0 到 gapY)
                graphics.fill(px, py, px + PIPE_WIDTH, py + pipe.gapY, pipeColor);
                // 绘制上管道口装饰
                graphics.fill(px - 2, py + pipe.gapY - 10, px + PIPE_WIDTH + 2, py + pipe.gapY, pipeColor);

                // 下管道 (从 gapY + gapSize 到 底部)
                int bottomPipeTop = pipe.gapY + PIPE_GAP_SIZE;
                graphics.fill(px, py + bottomPipeTop, px + PIPE_WIDTH, py + GAME_HEIGHT, pipeColor);
                // 绘制下管道口装饰
                graphics.fill(px - 2, py + bottomPipeTop, px + PIPE_WIDTH + 2, py + bottomPipeTop + 10, pipeColor);
            }

            // 2. 绘制小鸟
            // 鸟固定在水平中间，只移动 Y 轴
            int birdDrawX = startX + (GAME_WIDTH - BIRD_SIZE) / 2;
            int birdDrawY = startY + Math.round(birdY);

            // 黄色小鸟 0xFFFFD700
            graphics.fill(birdDrawX, birdDrawY, birdDrawX + BIRD_SIZE, birdDrawY + BIRD_SIZE, 0xFFFFD700);
            // 眼睛
            graphics.fill(birdDrawX + BIRD_SIZE - 4, birdDrawY + 2, birdDrawX + BIRD_SIZE, birdDrawY + 6, 0xFFFFFFFF);
            graphics.fill(birdDrawX + BIRD_SIZE - 2, birdDrawY + 3, birdDrawX + BIRD_SIZE - 1, birdDrawY + 5, 0xFF000000);
            // 嘴巴
            graphics.fill(birdDrawX + 2, birdDrawY + 8, birdDrawX + BIRD_SIZE + 2, birdDrawY + BIRD_SIZE - 2, 0xFFE36D36);

            // 3. 游戏结束
            if (gameOver) {
                graphics.fill(startX, startY, startX + GAME_WIDTH, startY + GAME_HEIGHT, 0x88000000);
                String over = Component.translatable("smartPhone.ui.app.game.endGame").getString();
                int textW = font.width(over);
                graphics.drawString(font, over, startX + (GAME_WIDTH - textW) / 2, startY + (GAME_HEIGHT - 8) / 2, ColorPattern.RED.color);
            }
        }
    }
}