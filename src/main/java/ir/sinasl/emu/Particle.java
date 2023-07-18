package ir.sinasl.emu;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Particle {


    Point2D pos, prePos;
    Point2D vel;
    Point2D acc;
    double maxVel;


    public Particle(double w, double h, double maxVel) {
        pos = new Point2D(Math.random() * w, Math.random() * h);
        prePos = new Point2D(pos.getX(), pos.getY());
        vel = new Point2D(0, 0);
        acc = new Point2D(0, 0);
        this.maxVel = maxVel;
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

    public void show(GraphicsContext graphicsContext, double blue, double red, double green) {
        graphicsContext.setStroke(new Color(red, green, blue, .01));
        graphicsContext.strokeLine(pos.getX(), pos.getY(), prePos.getX(), prePos.getY());
        updatePrevPos();
    }

}
