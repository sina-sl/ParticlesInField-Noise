package ir.sinasl.emu;

import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.Random;

public class Drawer {

    // the aligned -> opposed gradient is split into this many colour steps
    static final int PALETTE_SIZE = 64;

    // trails are drawn into a ring of stacked canvases. the oldest layer is periodically wiped
    // and reused, so lines disappear after Settings.trailLength frames.
    static final int LAYERS = 6;

    final Settings settings;
    final double w, h;
    final double cellSize;
    final int cols, rows;
    double zoff = 0;

    double[] perlin = randomPerlinTable();
    double[] nextPerlin;
    double force;
    double nextForce;
    long nextReseed;
    // frames into the current transition, -1 when not transitioning
    int transition = -1;

    final Particle[] particles;
    final int[] colorIndex;
    // particle indices sorted by colorIndex, and where each colour starts in it
    final int[] drawOrder;
    final Point2D[][] flowField;

    // strokes are grouped by colour, palette[tone * PALETTE_SIZE + step], to avoid a colour change per line
    Color[] palette;
    int[] colorStart;
    int paletteVersion = -1;

    final Canvas[] layers = new Canvas[LAYERS];
    // frame at which each layer stopped being drawn into
    final long[] layerEnd = new long[LAYERS];
    int current = 0;
    long layerStart = 0;
    long frame = 0;

    public Drawer(Pane root, double w, double h, Settings settings) {
        this.settings = settings;
        this.w = w;
        this.h = h;

        force = randomForce();
        nextReseed = randomReseedDelay();

        cellSize = settings.cellSize.get();
        cols = (int) Math.ceil(w / cellSize);
        rows = (int) Math.ceil(h / cellSize);
        flowField = new Point2D[rows][cols];

        particles = new Particle[(int) (w * h / settings.pixelsPerParticle.get())];
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle(w, h);
        }
        colorIndex = new int[particles.length];
        drawOrder = new int[particles.length];

        Canvas background = new Canvas(w, h);
        background.getGraphicsContext2D().setFill(Color.BLACK);
        background.getGraphicsContext2D().fillRect(0, 0, w, h);
        root.getChildren().setAll(background);

        for (int i = 0; i < LAYERS; i++) {
            layers[i] = new Canvas(w, h);
            root.getChildren().add(layers[i]);
        }
    }

    public void draw() {
        if (paletteVersion != settings.paletteVersion.get()) {
            buildPalette();
        }
        updateSeed();
        updateFlowField();

        if (frame - layerStart >= framesPerLayer()) {
            rotateLayers();
        }

        double respawnRate = settings.respawnRate.get();
        double velMin = settings.maxVelMin.get(), velMax = settings.maxVelMax.get();
        double colorSmoothing = settings.colorSmoothing.get();
        double maxColorAngle = settings.maxColorAngle.get();
        int tones = tones();

        Canvas layer = layers[current];
        layer.toFront();
        GraphicsContext gc = layer.getGraphicsContext2D();
        for (int i = 0; i < particles.length; i++) {
            Particle particle = particles[i];
            if (Math.random() < respawnRate) {
                particle.respawn(w, h);
            }
            particle.maxVel = velMin + particle.velSeed * (velMax - velMin);
            Point2D field = sampleField(particle.pos.getX(), particle.pos.getY());
            particle.applyForce(field);
            particle.update(w, h);
            particle.color += (alignment(particle.vel, field, maxColorAngle) - particle.color) * colorSmoothing;
            int tone = Math.min((int) (particle.toneSeed * tones), tones - 1);
            colorIndex[i] = tone * PALETTE_SIZE + (int) Math.round(particle.color * (PALETTE_SIZE - 1));
        }

        sortByColor();
        for (int c = 0; c < palette.length; c++) {
            if (colorStart[c] == colorStart[c + 1]) {
                continue;
            }
            gc.setStroke(palette[c]);
            for (int k = colorStart[c]; k < colorStart[c + 1]; k++) {
                particles[drawOrder[k]].show(gc);
            }
        }

        fadeLayers();
        frame++;
    }

    /** Starts blending into a new random noise seed and force right away. */
    public void reseedNow() {
        if (transition < 0) {
            nextReseed = frame;
        }
    }

    public void clearTrails() {
        for (Canvas layer : layers) {
            layer.getGraphicsContext2D().clearRect(0, 0, w, h);
        }
    }

    private int tones() {
        return Math.max(1, (int) Math.round(settings.tones.get()));
    }

    private long framesPerLayer() {
        return Math.max(1, Math.round(settings.trailLength.get() / LAYERS));
    }

    private void buildPalette() {
        paletteVersion = settings.paletteVersion.get();
        int tones = tones();
        Random random = new Random(settings.toneSeed.get());
        double hueShift = settings.toneHueShift.get();
        double saturationShift = settings.toneSaturationShift.get();
        double brightnessShift = settings.toneBrightnessShift.get();
        double alpha = settings.lineAlpha.get();

        palette = new Color[tones * PALETTE_SIZE];
        colorStart = new int[palette.length + 1];
        for (int t = 0; t < tones; t++) {
            // the first tone is always the exact base colour
            double hue = t == 0 ? 0 : (random.nextDouble() * 2 - 1) * hueShift;
            double saturation = t == 0 ? 1 : 1 + (random.nextDouble() * 2 - 1) * saturationShift;
            double brightness = t == 0 ? 1 : 1 + (random.nextDouble() * 2 - 1) * brightnessShift;
            Color aligned = settings.alignedColor.get().deriveColor(hue, saturation, brightness, 1);
            Color opposed = settings.opposedColor.get().deriveColor(hue, saturation, brightness, 1);
            for (int i = 0; i < PALETTE_SIZE; i++) {
                Color c = opposed.interpolate(aligned, (double) i / (PALETTE_SIZE - 1));
                palette[t * PALETTE_SIZE + i] = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
            }
        }
    }

    // counting sort of particle indices by colour
    private void sortByColor() {
        Arrays.fill(colorStart, 0);
        for (int c : colorIndex) {
            colorStart[c + 1]++;
        }
        for (int c = 0; c < palette.length; c++) {
            colorStart[c + 1] += colorStart[c];
        }
        int[] next = colorStart.clone();
        for (int i = 0; i < colorIndex.length; i++) {
            drawOrder[next[colorIndex[i]]++] = i;
        }
    }

    // bilinear interpolation between the four surrounding field cells, so neither the motion
    // nor the colour steps at cell borders
    private Point2D sampleField(double px, double py) {
        double fx = Math.min(px / cellSize, cols - 1);
        double fy = Math.min(py / cellSize, rows - 1);
        int x0 = (int) fx, y0 = (int) fy;
        int x1 = Math.min(x0 + 1, cols - 1), y1 = Math.min(y0 + 1, rows - 1);
        double tx = fx - x0, ty = fy - y0;

        Point2D top = flowField[y0][x0].interpolate(flowField[y0][x1], tx);
        Point2D bottom = flowField[y1][x0].interpolate(flowField[y1][x1], tx);
        return top.interpolate(bottom, ty);
    }

    // 1 when the velocity follows the field, 0 when it is maxAngle degrees or more off
    private static double alignment(Point2D vel, Point2D field, double maxAngle) {
        if (vel.magnitude() == 0 || field.magnitude() == 0 || maxAngle <= 0) {
            return 1;
        }
        return 1 - Math.min(vel.angle(field), maxAngle) / maxAngle;
    }

    private void updateSeed() {
        if (transition < 0 && frame >= nextReseed) {
            nextPerlin = randomPerlinTable();
            nextForce = randomForce();
            transition = 0;
        }
        if (transition >= transitionFrames()) {
            perlin = nextPerlin;
            force = nextForce;
            nextPerlin = null;
            transition = -1;
            nextReseed = frame + randomReseedDelay();
        }
    }

    private void updateFlowField() {
        double t = transition < 0 ? 0 : scaled_cosine((double) transition / transitionFrames());
        double inc = settings.noiseScale.get() * cellSize;
        double mag = force + (transition < 0 ? 0 : (nextForce - force) * t);

        double yoff = 0;
        for (int y = 0; y < rows; y++) {
            double xoff = 0;
            for (int x = 0; x < cols; x++) {
                double n = noise(perlin, xoff, yoff, zoff);
                if (transition >= 0) {
                    n += (noise(nextPerlin, xoff, yoff, zoff) - n) * t;
                }
                double angle = n * (Math.PI * 2);
                flowField[y][x] = vectorFromAngle(angle, mag);
                xoff += inc;
            }
            yoff += inc;
        }
        zoff += settings.zIncrement.get();
        if (transition >= 0) {
            transition++;
        }
    }

    private int transitionFrames() {
        return Math.max(1, (int) Math.round(settings.transitionFrames.get()));
    }

    private double randomForce() {
        double min = settings.forceMin.get(), max = settings.forceMax.get();
        return min + Math.random() * (max - min);
    }

    private long randomReseedDelay() {
        double min = settings.reseedMin.get(), max = settings.reseedMax.get();
        return Math.max(1, Math.round(min + Math.random() * (max - min)));
    }

    private void rotateLayers() {
        layerEnd[current] = frame;
        layerStart = frame;
        current = (current + 1) % LAYERS;
        layers[current].getGraphicsContext2D().clearRect(0, 0, w, h);
    }

    // older layers fade out linearly until they are wiped and reused
    private void fadeLayers() {
        double lifetime = (double) (LAYERS - 1) * framesPerLayer();
        for (int i = 0; i < LAYERS; i++) {
            if (i == current) {
                layers[i].setOpacity(1);
            } else {
                double age = frame - layerEnd[i];
                layers[i].setOpacity(Math.max(0, 1 - age / lifetime));
            }
        }
    }

    static final int PERLIN_SIZE = 4095;


    int PERLIN_YWRAPB = 4;
    int PERLIN_YWRAP = 16;
    int PERLIN_ZWRAPB = 8;
    int PERLIN_ZWRAP = 256;

    double perlin_octaves = 4; // default to medium smooth
    double perlin_amp_falloff = 0.5; // 50% reduction/octave

//  double scaled_cosine = i => 0.5 * (1.0 - Math.cos(i * Math.PI));

    private static double[] randomPerlinTable() {
        double[] table = new double[PERLIN_SIZE + 1];
        for (int i = 0; i < table.length; i++) {
            table[i] = Math.random();
        }
        return table;
    }

    public double noise(double[] perlin, double x, double y, double z) {

        if (x < 0) {
            x = -x;
        }
        if (y < 0) {
            y = -y;
        }
        if (z < 0) {
            z = -z;
        }

        int xi = (int) Math.floor(x),
                yi = (int) Math.floor(y),
                zi = (int) Math.floor(z);

        double xf = x - xi;
        double yf = y - yi;
        double zf = z - zi;
        double rxf, ryf;

        double r = 0;
        double ampl = 0.5;

        double n1, n2, n3;

        for (int o = 0; o < perlin_octaves; o++) {
            int of = xi + (yi << PERLIN_YWRAPB) + (zi << PERLIN_ZWRAPB);


            rxf = scaled_cosine(xf);
            ryf = scaled_cosine(yf);


            n1 = perlin[of & PERLIN_SIZE];
            n1 += rxf * (perlin[(of + 1) & PERLIN_SIZE] - n1);
            n2 = perlin[(of + PERLIN_YWRAP) & PERLIN_SIZE];
            n2 += rxf * (perlin[(of + PERLIN_YWRAP + 1) & PERLIN_SIZE] - n2);
            n1 += ryf * (n2 - n1);

            of += PERLIN_ZWRAP;
            n2 = perlin[of & PERLIN_SIZE];
            n2 += rxf * (perlin[(of + 1) & PERLIN_SIZE] - n2);
            n3 = perlin[(of + PERLIN_YWRAP) & PERLIN_SIZE];
            n3 += rxf * (perlin[(of + PERLIN_YWRAP + 1) & PERLIN_SIZE] - n3);
            n2 += ryf * (n3 - n2);

            n1 += scaled_cosine(zf) * (n2 - n1);

            r += n1 * ampl;
            ampl *= perlin_amp_falloff;
            xi <<= 1;
            xf *= 2;
            yi <<= 1;
            yf *= 2;
            zi <<= 1;
            zf *= 2;


            if (xf >= 1.0) {
                xi++;
                xf--;
            }
            if (yf >= 1.0) {
                yi++;
                yf--;
            }
            if (zf >= 1.0) {
                zi++;
                zf--;
            }
        }
        return r;

    }

    private double scaled_cosine(double i) {
        return 0.5 * (1.0 - Math.cos(i * Math.PI));
    }


    public static Point2D vectorFromAngle(double angle, double mag) {
        return new Point2D(Math.cos(angle) * mag, Math.sin(angle) * mag);
    }

}
