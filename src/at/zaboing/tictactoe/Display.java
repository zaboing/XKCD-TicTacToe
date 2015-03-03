package at.zaboing.tictactoe;

import org.bytedeco.javacv.*;
import org.bytedeco.javacpp.*;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

public class Display {
    private JFrame frame;

    private IplImage currentImage;

    private Thread updateThread;

    private int targetFPS;

    public Display() {
        frame = new JFrame("XKCD: Tic Tac Toe (Implementation)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new JLabel(new ImageIcon(loadDefaultImage())));
        frame.setSize(640, 480);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        targetFPS = 60;
        startUpdateThread();
    }

    private BufferedImage loadDefaultImage() {
        try {
            BufferedImage defaultImage = ImageIO.read(new File("img/tactic_cross.png"));
            currentImage = IplImage.createFrom(defaultImage);
            return defaultImage;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void startUpdateThread() {
        updateThread = new Thread(this::updateLoop);
        updateThread.setName("XKCD Frame update thread");
        updateThread.setDaemon(true);
        updateThread.start();
    }

    private void updateLoop() {
        try {
            frame.setSize(640, 480);


            while (frame.isVisible()) {
                System.gc();
                Insets insets = frame.getInsets();
                Dimension size = frame.getSize();
                size.width -= insets.left + insets.right;
                size.height -= insets.top - insets.bottom;

                long before = System.currentTimeMillis();

                IplImage resized = IplImage.create(size.width, size.height, currentImage.depth(), currentImage.nChannels());

                cvResize(currentImage, resized);

                JLabel content = new JLabel(new ImageIcon(resized.getBufferedImage()));
                //content.setSize(640, 480);
                frame.setContentPane(content);
                frame.revalidate();
                frame.repaint();

                long after = System.currentTimeMillis();

                int target = 1000 / targetFPS;
                if ((after - before) < target) {
                    try {
                        Thread.sleep(target - after + before);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        } finally {
            frame.dispose();
        }
    }

    private IplImage crop() {

        IplImage threshed = IplImage.create(currentImage.width(), currentImage.height(), 8, 1);


        cvInRangeS(currentImage, cvScalar(0, 0, 0, 0), cvScalar(50, 50, 50, 255), threshed);

        CvRect contour = CvUtils.getBiggestContour(threshed);

        return CvUtils.subImage(currentImage, contour);
    }

    private int[] getNextMove() {
        IplImage threshed = IplImage.create(currentImage.cvSize(), 8, 1);

        cvInRangeS(currentImage, cvScalar(0, 0, 200, 0), cvScalar(50, 50, 255, 255), threshed);

        CvRect contour = CvUtils.getBiggestContour(threshed);

        int centerX = contour.x() + contour.width() / 2;
        int centerY = contour.y() + contour.height() / 2;

        int x = 3 * centerX / currentImage.width();
        int y = 3 * centerY / currentImage.height();

        return new int[] { x, y };
    }

    private void move(int i, int j) {
        int widthThird = currentImage.width() / 3;
        int heightThird = currentImage.height() / 3;
        currentImage = CvUtils.subImage(currentImage, cvRect(i * widthThird, j * heightThird, widthThird, heightThird));
        int[] move = getNextMove();
        System.out.println(move[0] + " " + move[1]);
    }

    public static void main(String[] args) {
        Display display = new Display();
        display.getNextMove();
        display.move(1, 1);
    }
}
