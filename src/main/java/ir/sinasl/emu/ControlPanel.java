package ir.sinasl.emu;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Overlay with sliders, colour pickers and actions bound to {@link Settings}.
 */
public class ControlPanel extends ScrollPane {

    static final String SHORTCUTS = String.join("\n",
            "H        show / hide this panel",
            "Space    pause / resume",
            "N        new noise seed now",
            "T        new random tones",
            "C        clear lines",
            "R        restart particles",
            "D        reset to defaults",
            "Enter    export PNG",
            "F11      fullscreen");

    private final VBox content = new VBox(6);
    private GridPane grid;
    private int row;

    public ControlPanel(Settings s, Actions actions) {
        setContent(content);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setPrefWidth(420);
        setMaxWidth(420);
        setMaxHeight(USE_PREF_SIZE);
        setFocusTraversable(false);
        setStyle("-fx-background: transparent; -fx-background-color: rgba(18, 22, 16, 0.88);"
                + " -fx-background-radius: 8; -fx-padding: 4;");
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: transparent;");

        FlowPane buttons = new FlowPane(6, 6);
        buttons.getChildren().addAll(
                button("Pause (Space)", actions::togglePause),
                button("New seed (N)", actions::reseed),
                button("New tones (T)", s::newTones),
                button("Clear lines (C)", actions::clearTrails),
                button("Restart (R)", actions::restart),
                button("Defaults (D)", s::reset),
                button("Export (Enter)", actions::export));
        content.getChildren().add(buttons);

        section("Lines");
        slider("Line lifetime (frames)", s.trailLength, 30, 2000, "%.0f");
        slider("Line opacity", s.lineAlpha, 0.005, 0.3, "%.3f");
        slider("Respawn rate", s.respawnRate, 0, 0.02, "%.4f");
        slider("Speed min", s.maxVelMin, 0.1, 5, "%.2f");
        slider("Speed max", s.maxVelMax, 0.1, 5, "%.2f");
        slider("Density (px² / particle) *", s.pixelsPerParticle, 4, 400, "%.0f");

        section("Colour");
        colorPicker("Aligned colour", s.alignedColor);
        colorPicker("Opposed colour", s.opposedColor);
        slider("Tones", s.tones, 1, 64, "%.0f");
        slider("Tone hue shift (°)", s.toneHueShift, 0, 60, "%.0f");
        slider("Tone saturation shift", s.toneSaturationShift, 0, 0.8, "%.2f");
        slider("Tone brightness shift", s.toneBrightnessShift, 0, 0.8, "%.2f");
        slider("Opposed at angle (°)", s.maxColorAngle, 5, 180, "%.0f");
        slider("Colour smoothing", s.colorSmoothing, 0.002, 0.5, "%.3f");

        section("Field");
        slider("Noise scale", s.noiseScale, 0.001, 0.05, "%.4f");
        slider("Field drift speed", s.zIncrement, 0, 0.02, "%.4f");
        slider("Force min", s.forceMin, 0.005, 0.5, "%.3f");
        slider("Force max", s.forceMax, 0.005, 0.5, "%.3f");
        slider("Reseed every min (frames)", s.reseedMin, 30, 3000, "%.0f");
        slider("Reseed every max (frames)", s.reseedMax, 30, 3000, "%.0f");
        slider("Reseed blend (frames)", s.transitionFrames, 1, 600, "%.0f");
        slider("Cell size (px) *", s.cellSize, 2, 40, "%.0f");

        Label note = label("* changing this restarts the simulation");
        note.setOpacity(0.7);
        Label shortcuts = label(SHORTCUTS);
        shortcuts.setStyle(shortcuts.getStyle() + " -fx-font-family: monospace;");
        content.getChildren().addAll(note, sectionTitle("Shortcuts"), shortcuts);
    }

    private void section(String title) {
        grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(2);
        row = 0;
        content.getChildren().addAll(sectionTitle(title), grid);
    }

    private Label sectionTitle(String title) {
        Label label = label(title);
        label.setStyle(label.getStyle() + " -fx-font-weight: bold; -fx-font-size: 13px;");
        label.setPadding(new Insets(8, 0, 2, 0));
        return label;
    }

    private void slider(String name, DoubleProperty property, double min, double max, String format) {
        Slider slider = new Slider(min, max, property.get());
        slider.valueProperty().bindBidirectional(property);
        slider.setPrefWidth(130);
        slider.setFocusTraversable(false);

        Label value = label("");
        value.setMinWidth(50);
        value.setAlignment(Pos.CENTER_RIGHT);
        value.textProperty().bind(property.asString(format));

        grid.addRow(row++, name(name), slider, value);
    }

    private void colorPicker(String name, ObjectProperty<Color> property) {
        ColorPicker picker = new ColorPicker(property.get());
        picker.valueProperty().bindBidirectional(property);
        picker.setFocusTraversable(false);
        grid.add(name(name), 0, row);
        grid.add(picker, 1, row++, 2, 1);
    }

    private static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setFocusTraversable(false);
        button.setOnAction(e -> action.run());
        return button;
    }

    // a row title that is never truncated
    private static Label name(String text) {
        Label label = label(text);
        label.setMinWidth(Region.USE_PREF_SIZE);
        return label;
    }

    private static Label label(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #e6ecdf; -fx-font-size: 12px;");
        return label;
    }

    /** Actions the panel triggers on the running simulation. */
    public interface Actions {
        void togglePause();

        void reseed();

        void clearTrails();

        void restart();

        void export();
    }
}
