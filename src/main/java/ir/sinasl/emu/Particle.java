package ir.sinasl.emu;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;

public class Particle {


    Point2D pos, prePos;
    Point2D vel;
    Point2D acc;
    double maxVel;
    // 0..1 position between the opposed and aligned colour, smoothed over time
    double color = 1;
    // fixed random 0..1 values that pick this line's colour tone and top speed from the
    // current settings, so both follow the settings when they change
    final double toneSeed = Math.random();
    final double velSeed = Math.random();


    public Particle(double w, double h) {
        pos = new Point2D(Math.random() * w, Math.random() * h);
        prePos = new Point2D(pos.getX(), pos.getY());
        vel = new Point2D(0, 0);
        acc = new Point2D(0, 0);
    }


    public void respawn(double w, double h) {
        pos = new Point2D(Math.random() * w, Math.random() * h);
        vel = new Point2D(0, 0);
        color = 1;
        updatePrevPos();
    }

    private void updatePrevPos() {
        prePos = new Point2D(pos.getX(), pos.getY());
    }

    public void update(double w, double h) {

//    var newVel = vel.add(acc);
        var newVelX = vel.getX() + acc.getX();
        var newVelY = vel.getY() + acc.getY();

        if (-maxVel < newVelX && newVelX < maxVel) {
            vel = new Point2D(newVelX, vel.getY());
        }
        if (-maxVel < newVelY && newVelY < maxVel) {
            vel = new Point2D(vel.getX(), newVelY);
        }

        pos = pos.add(vel);
//    acc.multiply(0);

        if (pos.getX() > w) {
            pos = new Point2D(pos.getX() - w, pos.getY());
            updatePrevPos();
        }
        if (pos.getX() < 0) {
            pos = new Point2D(w + pos.getX(), pos.getY());
            updatePrevPos();
        }
        if (pos.getY() > h) {
            pos = new Point2D(pos.getX(), pos.getY() - h);
            updatePrevPos();
        }
        if (pos.getY() < 0) {
            pos = new Point2D(pos.getX(), h + pos.getY());
            updatePrevPos();
        }
    }


    public void applyForce(Point2D force) {
        acc = force;
    }

    public void show(GraphicsContext graphicsContext) {
        graphicsContext.strokeLine(pos.getX(), pos.getY(), prePos.getX(), prePos.getY());
        updatePrevPos();
    }

}
