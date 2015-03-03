package at.zaboing.tictactoe;


import org.bytedeco.javacpp.Loader;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

public class CvUtils {

    public static CvRect getBiggestContour(IplImage srcImage) {

        IplImage resultImage = IplImage.create(srcImage.width(), srcImage.height(), 8, 1);

        CvMemStorage mem = CvMemStorage.create();
        CvSeq contours = new CvSeq();
        CvSeq ptr;

        cvFindContours(srcImage, mem, contours, Loader.sizeof(CvContour.class), CV_RETR_CCOMP, CV_CHAIN_APPROX_SIMPLE, cvPoint(0, 0));

        CvRect biggestBox = null;
        int biggestSize = 0;

        for (ptr = contours; ptr != null && !ptr.isNull(); ptr = ptr.h_next()) {
            CvRect boundbox = cvBoundingRect(ptr);

            int size = boundbox.width() * boundbox.height();
            if (size >= biggestSize) {
                biggestBox = boundbox;
                biggestSize = size;
            }
        }
        if (biggestBox != null) {
            mem.deallocate();
            return biggestBox;
        }
        return null;
    }


    public static IplImage subImage(IplImage src, CvRect region) {
        IplImage subImage = IplImage.create(region.width(), region.height(),
                src.depth(), src.nChannels());

        cvSetImageROI(src, region);
        cvCopy(src, subImage);
        cvResetImageROI(src);

        return subImage;
    }

    public static IplImage threshIncluding(IplImage src, CvScalar minColor, CvScalar maxColor) {
        IplImage mask = getMask(src, minColor, maxColor);
        IplImage dst = IplImage.create(src.cvSize(), src.depth(), src.nChannels());
        cvCopy(src, dst, mask);

        return dst;
    }

    public static IplImage threshExcluding(IplImage src, CvScalar minColor, CvScalar maxColor) {
        IplImage mask = threshExcludingAsMask(src, minColor, maxColor);
        IplImage dst = IplImage.create(src.cvSize(), src.depth(), src.nChannels());
        cvCopy(src, dst, mask);

        return dst;
    }

    public static IplImage threshExcludingAsMask(IplImage src, CvScalar minColor, CvScalar maxColor) {
        IplImage mask = getMask(src, minColor, maxColor);
        Mat maskMat = Mat.createFrom(mask.getBufferedImage());
        bitwise_not(maskMat, maskMat);
        mask = maskMat.asIplImage();

        return mask;
    }

    private static IplImage getMask(IplImage src, CvScalar min, CvScalar max) {
        IplImage mask = IplImage.create(src.cvSize(), 8, 1);
        cvInRangeS(src, min, max, mask);

        return mask;
    }

    public static IplImage cropImage(IplImage image, CvScalar backgroundColor) {
        CvScalar min = cvScalar(clamp(backgroundColor.blue() - 20, 0, 255),
                clamp(backgroundColor.green() - 20, 0, 255),
                clamp(backgroundColor.red() - 20, 0, 255),
                0);

        CvScalar max = cvScalar(clamp(backgroundColor.blue() + 20, 0, 255),
                clamp(backgroundColor.green() + 20, 0, 255),
                clamp(backgroundColor.red() + 20, 0, 255),
                0);

        IplImage threshed = threshExcluding(image, min, max);

        return threshed;
    }

    private static double clamp(double i, int min, int max) {
        return i > min ? i < max ? i : max : min;
    }
}
