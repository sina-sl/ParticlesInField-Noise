package ir.sinasl.emu;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class Main extends Application implements ControlPanel.Actions {

    private final Settings settings = new Settings();
    private final Pane canvasPane = new Pane();
    private Scene scene;
    private Stage stage;
    private Drawer drawer;
    private boolean paused;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        // size the canvas to the screen resolution
        Rectangle2D bounds = Screen.getPrimary().getBounds();

        ControlPanel panel = new ControlPanel(settings, this);
        StackPane.setAlignment(panel, Pos.TOP_LEFT);
        StackPane.setMargin(panel, new Insets(12));

        StackPane root = new StackPane(canvasPane, panel);
        scene = new Scene(root, bounds.getWidth(), bounds.getHeight(), Color.BLACK);

        drawer = new Drawer(canvasPane, bounds.getWidth(), bounds.getHeight(), settings);

        // rebuild the drawer once the window stops resizing, or when a setting that
        // needs a rebuild stops changing
        PauseTransition restartDebounce = new PauseTransition(Duration.millis(300));
        restartDebounce.setOnFinished(e -> restart());
        scene.widthProperty().addListener((obs, o, n) -> restartDebounce.playFromStart());
        scene.heightProperty().addListener((obs, o, n) -> restartDebounce.playFromStart());
        settings.pixelsPerParticle.addListener((obs, o, n) -> restartDebounce.playFromStart());
        settings.cellSize.addListener((obs, o, n) -> restartDebounce.playFromStart());

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!paused) {
                    drawer.draw();
                }
            }
        };

        // a filter so shortcuts work even while a control in the panel has focus
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            boolean handled = true;
            switch (event.getCode()) {
                case H -> panel.setVisible(!panel.isVisible());
                case SPACE -> togglePause();
                case N -> reseed();
                case T -> settings.newTones();
                case C -> clearTrails();
                case R -> restart();
                case D -> settings.reset();
                case ENTER -> export();
                case F11 -> stage.setFullScreen(!stage.isFullScreen());
                default -> handled = false;
            }
            if (handled) {
                event.consume();
            }
        });

        stage.setTitle("Particles In Field");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        timer.start();
    }

    @Override
    public void togglePause() {
        paused = !paused;
    }

    @Override
    public void reseed() {
        drawer.reseedNow();
    }

    @Override
    public void clearTrails() {
        drawer.clearTrails();
    }

    @Override
    public void restart() {
        if (scene.getWidth() > 0 && scene.getHeight() > 0) {
            drawer = new Drawer(canvasPane, scene.getWidth(), scene.getHeight(), settings);
        }
    }

    @Override
    public void export() {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.BLACK);
        WritableImage image = canvasPane.snapshot(params, null);

        int w = (int) image.getWidth(), h = (int) image.getHeight();
        int[] pixels = new int[w * h];
        image.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0, w);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, w, h, pixels, 0, w);

        try {
            File export = new File(System.currentTimeMillis() + ".png");
            ImageIO.write(img, "png", export);
            System.out.println("exported at: " + export.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }

}
