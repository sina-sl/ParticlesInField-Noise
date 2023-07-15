package ir.sinasl.emu;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;



public class Main extends Application {

  public static final double W = 1366; // canvas dimensions.
  public static final double H = 768;

  public static final double freq = 30;


  @Override
  public void start(Stage stage) {
    Drawer drawer = new Drawer();
    Canvas canvas = new Canvas(W, H);

    drawer.start(canvas.getGraphicsContext2D());

    AnimationTimer timer = new AnimationTimer() {
      @Override
      public void handle(long now) {
        drawer.draw(canvas.getGraphicsContext2D(), now);
      }
    };

//    Timeline timeline = new Timeline(
//      new KeyFrame(
//        Duration.seconds(1/freq),
//        e -> drawer.draw(canvas.getGraphicsContext2D(), 0)
//      )
//    );

    Scene scene = new Scene(new Group(canvas));
    scene.setOnKeyPressed(event -> {
      if (event.getCode().equals(KeyCode.ENTER)) {

        WritableImage writableImage = new WritableImage(canvas.widthProperty().intValue(), canvas.heightProperty().intValue());
        canvas.snapshot(new SnapshotParameters(),writableImage);

        BufferedImage img = new BufferedImage(canvas.widthProperty().intValue(), canvas.heightProperty().intValue(), BufferedImage.TYPE_INT_ARGB);


        for (int i = 0; i < canvas.heightProperty().intValue(); i++) {
          for (int j = 0; j < canvas.widthProperty().intValue(); j++) {

            img.setRGB(j,i,writableImage.getPixelReader().getArgb(j,i));

          }
        }



        try {
          File export  = new File(String.valueOf(System.currentTimeMillis()));
          export.createNewFile();
          ImageIO.write(img,"png",export);
        } catch (IOException e) {
          e.printStackTrace();
        }


      }
    });

    stage.setScene(scene);
    stage.show();



    timer.start();
//    timeline.setCycleCount(Timeline.INDEFINITE);
//    timeline.play();
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    launch();
  }


}