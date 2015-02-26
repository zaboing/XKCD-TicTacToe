package at.zaboing.tictactoe;

import org.bytedeco.javacv.*;
import org.bytedeco.javacpp.*;

import java.io.File;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

public class Display {
    private CanvasFrame frame;

    private IplImage image;

    private Thread updateThread;

    private int targetFPS;

    public Display() {
        frame = new CanvasFrame("XKCD: Tic Tac Toe (Implementation)");
        targetFPS = 60;
        loadDefaultImage();
        startUpdateThread();
    }

    private void loadDefaultImage() {
        try {
            BufferedImage defaultImage = ImageIO.read(new File("img/xkcd.png"));
            image = IplImage.createFrom(defaultImage);
        } catch (Exception e) {
            e.printStackTrace();
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
            frame.setCanvasSize(640, 480);

            IplImage processingImage = cvCloneImage(image);

            while (frame.isVisible()) {
                long before = System.currentTimeMillis();

                IplImage processed = processImage(processingImage);

                frame.showImage(processed);

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

    private IplImage processImage(IplImage processingImage) {

        IplImage processed = IplImage.create(processingImage.width(), processingImage.height(), 8, 1);


        cvInRangeS(processingImage, cvScalar(0, 0, 0, 0), cvScalar(250, 250, 255, 255), processed);

        /*cvRectangle(processingImage,
                cvPoint(0, 0), cvPoint(processingImage.width(), processingImage.height()),
                CvScalar.RED, CV_FILLED, CV_AA, 0);*/

        return detectObjects(processed);
    }

    public static IplImage detectObjects(IplImage srcImage) {

        IplImage resultImage = IplImage.create(srcImage.width(), srcImage.height(), 8, 1);

        CvMemStorage mem = CvMemStorage.create();
        CvSeq contours = new CvSeq();
        CvSeq ptr;

        cvFindContours(srcImage, mem, contours, Loader.sizeof(CvContour.class), CV_RETR_CCOMP, CV_CHAIN_APPROX_SIMPLE, cvPoint(0, 0));

        CvRect boundbox;

        for (ptr = contours; ptr != null && !ptr.isNull(); ptr = ptr.h_next()) {
            boundbox = cvBoundingRect(ptr);

            cvRectangle(resultImage, cvPoint(boundbox.x(), boundbox.y()),
                    cvPoint(boundbox.x() + boundbox.width(), boundbox.y() + boundbox.height()),
                    cvScalar(255, 255, 255, 0), 1, 0, 0);
        }

        return resultImage;
    }

    public static void main(String[] args) {
        new Display();
    }
}
