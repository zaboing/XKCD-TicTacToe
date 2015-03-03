package at.zaboing.tictactoe;


import org.bytedeco.javacpp.Loader;

import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

public class CvUtils {

    public static CvRect getBiggestContour(IplImage srcImage) {
        List<CvRect> contours = getContours(srcImage);
        return contours.parallelStream().
                max((c1, c2) -> Integer.compare(c1.width() * c1.height(), c2.width() * c2.height())).
                orElse(null);
    }

    public static List<CvRect> getContours(IplImage srcImage) {
        List<CvRect> list = new ArrayList<>();

        IplImage resultImage = IplImage.create(srcImage.width(), srcImage.height(), 8, 1);

        CvMemStorage mem = CvMemStorage.create();
        CvSeq contours = new CvSeq();
        CvSeq ptr;

        cvFindContours(srcImage, mem, contours, Loader.sizeof(CvContour.class), CV_RETR_CCOMP, CV_CHAIN_APPROX_SIMPLE, cvPoint(0, 0));

        for (ptr = contours; ptr != null && !ptr.isNull(); ptr = ptr.h_next()) {
            CvRect boundingBox = cvBoundingRect(ptr);
            list.add(boundingBox);
        }
        mem.deallocate();
        return list;
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

    public static IplImage cropImage(IplImage image, CvScalar minColor, CvScalar maxColor) {
        IplImage threshed = threshExcludingAsMask(image, minColor, maxColor);

        List<CvRect> contours = getContours(threshed);
        CvRect boundingBox = contours.get(0);
        for (CvRect contour : contours) {
            if (contour.x() < boundingBox.x()) {
                int width = boundingBox.width() + boundingBox.x() - contour.x();
                boundingBox = boundingBox.x(contour.x()).width(width);
            }
            if (contour.y() < boundingBox.y()) {
                int height = boundingBox.height() + boundingBox.y() - contour.y();
                boundingBox = boundingBox.y(contour.y()).height(height);
            }
            int maxX = contour.x() + contour.width();
            if (maxX > boundingBox.x() + boundingBox.width()) {
                int distance = contour.x() - boundingBox.x();
                boundingBox = boundingBox.width(contour.width() - distance);
            }
            int maxY = contour.y() + contour.height();
            if (maxY > boundingBox.y() + boundingBox.height()) {
                int distance = contour.y() - boundingBox.y();
                boundingBox = boundingBox.height(contour.height() - distance);
            }
        }

        IplImage dst = threshed.clone();//image.clone();

        cvRectangle(dst, cvPoint(boundingBox.x(), boundingBox.y()),
                cvPoint(boundingBox.x() + boundingBox.width(), boundingBox.y() + boundingBox.height()),
                cvScalar(0, 255, 0, 0), 10, 8, 0);

        return dst;
    }

    private static double clamp(double i, int min, int max) {
        return i > min ? i < max ? i : max : min;
    }
}
