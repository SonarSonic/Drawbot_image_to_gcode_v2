package drawingbot.pfm;

import drawingbot.DrawingBotV3;
import drawingbot.helpers.ImageTools;
import drawingbot.helpers.RawBrightnessData;
import drawingbot.plotting.PlottingTask;
import org.imgscalr.Scalr;
import processing.core.PImage;

import java.awt.image.*;

public class PFMSketch extends AbstractSketchPFM {

    public int squiggles_till_first_change;

    public PFMSketch(PlottingTask task){
        super(task);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void preProcessing() {
        BufferedImage dst = (BufferedImage) task.getPlottingImage().getNative();

        //ImageTools.imageCrop(task); //TODO USE SCALR
        int targetWidth = (int)(app.getDrawingAreaWidthMM() * DrawingBotV3.image_scale);
        int targetHeight = (int)(app.getDrawingAreaHeightMM() * DrawingBotV3.image_scale);
        dst = Scalr.resize(dst, targetWidth, targetHeight);

        dst = ImageTools.lazyConvolutionFilter(dst, ImageTools.MATRIX_UNSHARP_MASK, 4, true);
        dst = ImageTools.lazyConvolutionFilter(dst, ImageTools.MATRIX_UNSHARP_MASK, 3, true);

        dst = ImageTools.lazyImageBorder(dst, "border/b1.png", 0, 0);
        dst = ImageTools.lazyImageBorder(dst, "border/b11.png", 0, 0);
        dst = ImageTools.lazyRGBFilter(dst, ImageTools::grayscaleFilter);

        task.img_plotting = new PImage(dst);
        rawBrightnessData = new RawBrightnessData(dst);
        initialProgress = rawBrightnessData.getAverageBrightness();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void findDarkestNeighbour(int start_x, int start_y) {
        darkest_neighbor = 257;
        float delta_angle;
        float start_angle = randomSeed(-72, -52);    // Spitfire;

        if (squiggle_count < squiggles_till_first_change) {
            delta_angle = 360.0F / (float)tests;
        } else {
            delta_angle = 180F + 7F / (float)tests;
        }

        int nextLineLength = randomSeed(minLineLength, maxLineLength);
        for (int d = 0; d < tests; d ++) {
            bresenhamAvgBrightness(start_x, start_y, nextLineLength, (delta_angle * d) + start_angle);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void postProcessing() {
        task.img_plotting = new PImage(rawBrightnessData.asBufferedImage());
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void outputParameters() {
        task.comment("adjustbrightness: " + adjustbrightness);
        task.comment("squiggle_length: " + squiggle_length);
    }

}