package ir.sinasl.emu;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * All tunable parameters of the simulation. The drawer reads them every frame, so changes
 * from the control panel apply live; only {@link #pixelsPerParticle} and {@link #cellSize}
 * need the simulation to be rebuilt.
 */
public class Settings {

    private final List<Runnable> resets = new ArrayList<>();

    // --- field ---
    // noise sampling step per pixel (lower = smoother, larger swirls)
    final DoubleProperty noiseScale = number(0.0095);
    // flow field cell size in pixels (rebuild)
    final DoubleProperty cellSize = number(8);
    // how fast the field evolves over time
    final DoubleProperty zIncrement = number(0.002);
    // the force is re-rolled within this range on every reseed
    final DoubleProperty forceMin = number(0.03);
    final DoubleProperty forceMax = number(0.15);
    // every reseedMin..reseedMax frames switch to a new random noise seed and force,
    // blending into it over transitionFrames so the field morphs instead of jumping
    final DoubleProperty reseedMin = number(300);
    final DoubleProperty reseedMax = number(700);
    final DoubleProperty transitionFrames = number(120);

    // --- particles ---
    // one particle per this many square pixels (rebuild)
    final DoubleProperty pixelsPerParticle = number(10);
    // each particle gets its own top speed in this range
    final DoubleProperty maxVelMin = number(0.4);
    final DoubleProperty maxVelMax = number(1.6);
    // fraction of particles moved to a random spot each frame, so they don't all
    // collapse onto the same few paths
    final DoubleProperty respawnRate = number(0.002);
    // frames a line stays visible before it is removed
    final DoubleProperty trailLength = number(360);

    // --- colour ---
    // lines are coloured by how closely they follow the field: alignedColor when moving
    // along it, opposedColor when moving against it, blended in between
    final ObjectProperty<Color> alignedColor = color(Color.rgb(76, 154, 42));   // grass green
    final ObjectProperty<Color> opposedColor = color(Color.rgb(181, 224, 122)); // light grass green
    final DoubleProperty lineAlpha = number(0.06);
    // angle (degrees) off the field at which a line is fully opposedColor
    final DoubleProperty maxColorAngle = number(90);
    // how quickly a line's colour eases toward its target each frame (lower = smoother)
    final DoubleProperty colorSmoothing = number(0.03);
    // each line gets one of `tones` random variations of both colours, shifted by up to
    // these amounts, so lines stay in the same colour family but are not all identical
    final DoubleProperty tones = number(24);
    final DoubleProperty toneHueShift = number(10);        // degrees, +/-
    final DoubleProperty toneSaturationShift = number(0.2); // factor, +/-
    final DoubleProperty toneBrightnessShift = number(0.2); // factor, +/-
    // seed of the random tone variations, re-rolled by "new tones"
    final IntegerProperty toneSeed = new SimpleIntegerProperty((int) (Math.random() * Integer.MAX_VALUE));

    /** Bumped whenever anything the colour palette depends on changes. */
    final IntegerProperty paletteVersion = new SimpleIntegerProperty();

    public Settings() {
        for (Property<?> p : List.of(alignedColor, opposedColor, lineAlpha, tones,
                toneHueShift, toneSaturationShift, toneBrightnessShift, toneSeed)) {
            p.addListener((obs, o, n) -> paletteVersion.set(paletteVersion.get() + 1));
        }
    }

    public void reset() {
        resets.forEach(Runnable::run);
    }

    public void newTones() {
        toneSeed.set((int) (Math.random() * Integer.MAX_VALUE));
    }

    private DoubleProperty number(double initial) {
        DoubleProperty p = new SimpleDoubleProperty(initial);
        resets.add(() -> p.set(initial));
        return p;
    }

    private ObjectProperty<Color> color(Color initial) {
        ObjectProperty<Color> p = new SimpleObjectProperty<>(initial);
        resets.add(() -> p.set(initial));
        return p;
    }
}
