package ir.sinasl.emu;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import static ir.sinasl.emu.Main.H;
import static ir.sinasl.emu.Main.W;

public class Drawer {

    double inc = 0.009;
    double scl = 2;
    int cols, rows;
    double zoff = 0;

    Particle[] particles = new Particle[5000];
    Point2D[][] flowField;

    public void start(GraphicsContext graphicsContext) {

        cols = (int) Math.floor(W / scl);
        rows = (int) Math.floor(H / scl);

        flowField = new Point2D[rows][cols];

        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle(W, H, 0.5);
        }

        graphicsContext.setFill(Color.BLACK);
        graphicsContext.fillRect(0, 0, W, H);

    }

    public void draw(GraphicsContext graphicsContext, long now) {

        double yoff = 0;
        for (int y = 0; y < rows; y++) {

            double xoff = 0;
            for (int x = 0; x < cols; x++) {

//        int index = (x + y * cols);

                xoff += inc;

                double noise = noise(xoff, yoff, zoff);
                double angle = noise * (Math.PI * 2);
                flowField[y][x] = vectorFromAngle(angle, 0.05 * scl);

//        graphicsContext.setStroke(Color.BLACK);
//        graphicsContext.save();
//        graphicsContext.translate(x*scl,y*scl);
//        graphicsContext.rotate(Math.toDegrees(angle));
//        graphicsContext.strokeLine(0,0,scl,0);
//        graphicsContext.restore();

            }

            yoff += inc;
//      zoff+=0.0001;
        }


        for (Particle particle : particles) {
            int x = (int) (particle.pos.getX() / scl);
            int y = (int) (particle.pos.getY() / scl);

            try {
                particle.applyForce(flowField[y][x]);
                particle.update(W, H);
                particle.show(graphicsContext, 142 / 255., 73 / 255., 196 / 255.);
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.printf("x : %d - y : %d \n ", x, y);
                System.out.printf("x : %f - y : %f \n ", particle.pos.getX(), particle.pos.getY());
                System.exit(0);
            }
        }

    }

    int PERLIN_SIZE = 4095;
    double[] perlin;


    int PERLIN_YWRAPB = 4;
    int PERLIN_YWRAP = 16;
    int PERLIN_ZWRAPB = 8;
    int PERLIN_ZWRAP = 256;

    double perlin_octaves = 4; // default to medium smooth
    double perlin_amp_falloff = 0.5; // 50% reduction/octave

//  double scaled_cosine = i => 0.5 * (1.0 - Math.cos(i * Math.PI));

    public double noise(double x, double y, double z) {

        if (perlin == null) {
            perlin = new double[PERLIN_SIZE + 1];
            for (double i = 0; i < PERLIN_SIZE + 1; i++) {
                perlin[(int) i] = Math.random();
            }
        }

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
