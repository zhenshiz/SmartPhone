package com.smart.phone.ui.app.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.smart.phone.ui.app.ui.game.Direction;
import com.smart.phone.ui.app.ui.game.Point;
import com.smart.phone.ui.view.HomeScreen;
import com.smart.phone.util.UIElementUtil;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaJustify;
import org.appliedenergistics.yoga.YogaPositionType;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * 贪吃蛇游戏 UI
 */
public class SnakeGameUI extends AppUI {
    // 游戏配置
    private static final int GRID_SIZE = 4; // 每个格子的像素大小
    private static final int GAME_WIDTH = 20; // 游戏区域宽度（格子数）
    private static final int GAME_HEIGHT = 20; // 游戏区域高度（格子数）
    private static final int INITIAL_SPEED = 300; // 初始移动速度（毫秒）
    private static final int SPEED_INCREMENT = 10; // 每次吃到食物减少的移动时间

    // 游戏状态
    private final LinkedList<Point> snake = new LinkedList<>();
    private Point food;
    private Direction direction = Direction.RIGHT;
    private Direction nextDirection = Direction.RIGHT;
    private int score = 0;
    private boolean gameOver = false;
    private boolean gameStarted = false;
    private long lastMoveTime = 0;
    private int currentSpeed = INITIAL_SPEED;
    private final Random random = new Random();

    // UI 元素
    private final UIElement gameCanvas;
    private final Label scoreLabel;
    private final Button restartButton;
    private final Button startButton;

    public SnakeGameUI(HomeScreen homeScreen) {
        super(homeScreen);

        this.setFocusable(true);

        this.addEventListener(UIEvents.KEY_DOWN, event -> {
            switch (event.keyCode) {
                case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> setDirection(Direction.UP);
                case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> setDirection(Direction.DOWN);
                case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> setDirection(Direction.LEFT);
                case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> setDirection(Direction.RIGHT);
                case GLFW.GLFW_KEY_SPACE -> {
                    if (!gameStarted || gameOver) {
                        startGame();
                    }
                }
            }
        });

        // 创建游戏画布
        gameCanvas = new GameCanvas();

        // 创建分数标签
        Label label = new Label();
        label.textStyle(style -> {
            style.fontSize(6);
            style.textColor(ColorPattern.WHITE.color);
            style.adaptiveWidth(true).adaptiveHeight(true);
        });
        scoreLabel = label;

        // 创建开始按钮
        startButton = (Button) new Button().setText("smartPhone.ui.app.game.startGame").textStyle(textStyle -> textStyle.fontSize(6)).layout(layout -> {
            layout.setPositionType(YogaPositionType.ABSOLUTE);
            layout.top(3);
        }).addEventListener(UIEvents.CLICK, event -> startGame());

        // 创建重新开始按钮
        restartButton = (Button) new Button().setText("smartPhone.ui.app.game.resetGame").textStyle(textStyle -> textStyle.fontSize(6)).layout(layout -> {
            layout.setPositionType(YogaPositionType.ABSOLUTE);
            layout.top(3);
        }).addEventListener(UIEvents.CLICK, event -> startGame());

        // 添加 UI 元素到滚动视图
        buildUI();

        // 初始化游戏
        initializeGame();
    }

    private void buildUI() {
        // 分数显示区域
        UIElement scoreContainer = new UIElement()
                .layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeight(6);
                    layout.setJustifyContent(YogaJustify.CENTER);
                    layout.setAlignItems(YogaAlign.CENTER);
                });

        scoreContainer.addChildren(scoreLabel);

        // 游戏画布容器
        UIElement canvasContainer = new UIElement()
                .layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeight(GAME_HEIGHT * GRID_SIZE);
                    layout.setJustifyContent(YogaJustify.CENTER);
                    layout.setAlignItems(YogaAlign.CENTER);
                    layout.setMargin(YogaEdge.VERTICAL, 3);
                });

        canvasContainer.addChildren(gameCanvas);

        // 控制按钮区域
        UIElement controls = UIElementUtil.createControlButtons(this::setDirection);

        // 开始/重新开始按钮
        UIElement buttonContainer = new UIElement()
                .layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeight(10);
                    layout.setJustifyContent(YogaJustify.CENTER);
                    layout.setAlignItems(YogaAlign.CENTER);
                });

        buttonContainer.addChildren(startButton, restartButton);

        // 添加所有元素到滚动视图
        appScrollView.viewContainer.addChildren(scoreContainer, canvasContainer, controls, buttonContainer);
    }

    private void initializeGame() {
        snake.clear();
        score = 0;
        direction = Direction.RIGHT;
        nextDirection = Direction.RIGHT;
        gameOver = false;
        gameStarted = false;
        currentSpeed = INITIAL_SPEED;

        // 初始化蛇（长度为3）
        for (int i = 0; i < 3; i++) {
            snake.add(new Point(3 - i, 10));
        }

        // 生成食物
        spawnFood();

        // 更新分数显示
        updateScore();

        // 更新按钮状态
        startButton.setVisible(true);
        restartButton.setVisible(false);
    }

    private void startGame() {
        if (gameOver) {
            initializeGame();
        }
        gameStarted = true;
        startButton.setVisible(false);
        restartButton.setVisible(false);
    }

    private void setDirection(Direction newDirection) {
        // 防止直接反向
        if ((direction == Direction.UP && newDirection != Direction.DOWN) ||
                (direction == Direction.DOWN && newDirection != Direction.UP) ||
                (direction == Direction.LEFT && newDirection != Direction.RIGHT) ||
                (direction == Direction.RIGHT && newDirection != Direction.LEFT)) {
            nextDirection = newDirection;
        }
    }

    private void spawnFood() {
        List<Point> emptySpots = new ArrayList<>();
        for (int x = 0; x < GAME_WIDTH; x++) {
            for (int y = 0; y < GAME_HEIGHT; y++) {
                Point p = new Point(x, y);
                if (!snake.contains(p)) {
                    emptySpots.add(p);
                }
            }
        }

        if (!emptySpots.isEmpty()) {
            food = emptySpots.get(random.nextInt(emptySpots.size()));
        }
    }

    private void updateScore() {
        scoreLabel.setText(Component.translatable("smartPhone.ui.app.game.score", score));
    }

    private void gameOver() {
        gameOver = true;
        gameStarted = false;
        restartButton.setVisible(true);
    }

    @Override
    public void screenTick() {
        super.screenTick();

        if (gameStarted && !gameOver) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastMoveTime >= currentSpeed) {
                moveSnake();
                lastMoveTime = currentTime;
            }
        }
    }

    private void moveSnake() {
        direction = nextDirection;

        // 计算新的头部位置
        Point head = snake.getFirst();
        Point newHead = switch (direction) {
            case UP -> new Point(head.x(), head.y() - 1);
            case DOWN -> new Point(head.x(), head.y() + 1);
            case LEFT -> new Point(head.x() - 1, head.y());
            case RIGHT -> new Point(head.x() + 1, head.y());
        };

        // 检查碰撞
        if (checkCollision(newHead)) {
            gameOver();
            return;
        }

        // 添加新头部
        snake.addFirst(newHead);

        // 检查是否吃到食物
        if (newHead.equals(food)) {
            score += 10;
            updateScore();
            currentSpeed = Math.max(100, currentSpeed - SPEED_INCREMENT);
            spawnFood();
        } else {
            // 移除尾部
            snake.removeLast();
        }
    }

    private boolean checkCollision(Point point) {
        // 检查边界碰撞
        if (point.x() < 0 || point.x() >= GAME_WIDTH || point.y() < 0 || point.y() >= GAME_HEIGHT) {
            return true;
        }

        // 检查自身碰撞
        return snake.contains(point);
    }

    /**
     * 游戏画布组件
     */
    private class GameCanvas extends UIElement {

        public GameCanvas() {
            this.layout(layout -> {
                layout.setWidth(GAME_WIDTH * GRID_SIZE);
                layout.setHeight(GAME_HEIGHT * GRID_SIZE);
            }).style(style -> style.backgroundTexture(new ColorRectTexture(ColorPattern.T_BLACK.color)));
        }

        @Override
        public void drawBackgroundAdditional(GUIContext guiContext) {
            super.drawBackgroundAdditional(guiContext);

            var guiGraphics = guiContext.graphics;

            float screenX = this.getPositionX();
            float screenY = this.getPositionY();

            // 绘制蛇
            for (int i = 0; i < snake.size(); i++) {
                Point segment = snake.get(i);
                int x = Math.round(screenX + segment.x() * GRID_SIZE);
                int y = Math.round(screenY + segment.y() * GRID_SIZE);

                // 蛇头用不同的颜色
                int color = (i == 0) ? ColorPattern.GREEN.color : ColorPattern.T_GREEN.color;

                guiGraphics.fill(x, y, x + GRID_SIZE - 1, y + GRID_SIZE - 1, color);
            }

            // 绘制食物
            if (food != null) {
                int foodX = Math.round(screenX + food.x() * GRID_SIZE);
                int foodY = Math.round(screenY + food.y() * GRID_SIZE);
                guiGraphics.fill(foodX, foodY, foodX + GRID_SIZE - 1, foodY + GRID_SIZE - 1, ColorPattern.RED.color);
            }

            // 绘制游戏结束提示
            if (gameOver) {
                float textX = screenX + (float) (GAME_WIDTH * GRID_SIZE) / 2;
                float textY = screenY + (float) (GAME_HEIGHT * GRID_SIZE) / 2;

                String gameOverText = Component.translatable("smartPhone.ui.app.game.endGame").getString();
                float textWidth = minecraft.font.width(gameOverText);

                guiGraphics.drawString(minecraft.font, gameOverText, (int) (textX - textWidth / 2), (int) textY, ColorPattern.RED.color);
            }
        }
    }
}
