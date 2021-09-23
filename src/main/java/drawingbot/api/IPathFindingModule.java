package drawingbot.api;

import java.awt.image.BufferedImage;

/**
 * A {@link IPathFindingModule} (or PFM) defines an algorithm/method for converting an image to lines/curves.
 * Specifically generating lines from raw pixel data and providing them to the {@link IPlottingTask}
 */
public interface IPathFindingModule {

    /**
     * The colour mode, currently supported 0 = ARGB, 1 = HSB, 2 = Luminance (Y), 3 = ARGBY, 4 = Hybrid
     * This setting is very important to allow for an efficient {@link IPathFindingModule} as the selected setting dictates if values are cached or calculated
     * If you primarily need red/green/blue/alpha calculations go with ARGB
     * If you primarily need hue/saturation/brightness calculations go with HSB
     * If you only need luminance calculations go with Luminance
     * If you primarily need red/green/blue/alpha calculations & luminance go with ARGBY
     * If you need fast access to every value in ARGB & HSB & Luminance (very often) use Hybrid
     * @return the default colour mode is ARGB, but this is often the least efficient, especially for luminance orientated PFMs using bresenham calculations
     */
    default int getColourMode(){
        return 0;
    }

    /**
     * the transparent ARGB value of the {@link IPixelData}, this is important for brightness orientated PFMs
     * @return the current transparent ARGB value
     */
    default int getTransparentARGB(){
        return -1;
    }

    /**
     * The plotting resolution, how much to scale the image by before plotting.
     * @return typically = 1.0F
     */
    default float getPlottingResolution(){
        return 1.0F;
    }

    /**
     * Called immediately after the {@link IPathFindingModule}'s settings have been set.
     * Used to check the given settings and apply any special options to the plotting task}
     * Shouldn't be used for initial calculations the {@link IPlottingTask#getPixelData()} and {@link IPlottingTask#getReferencePixelData()} ()} will be initialized but the pixel data will not have been set
     */
    void init(IPlottingTask task);


    default BufferedImage preFilter(BufferedImage image){
        return image;
    }

    /**
     * Called once before the first {@link IPathFindingModule#doProcess()}
     * Should be used for initial calculations the {@link IPlottingTask#getPixelData()} and {@link IPlottingTask#getReferencePixelData()} ()} will now have been set.
     */
    default void preProcess(){}

    /**
     * Runs the PFM, generating the lines from the pixel data provided by {@link IPlottingTask#getPixelData()}
     * Called indefinitely until {@link IPlottingTask#finishProcess()} is called.
     * Implementations should also update the progress of the process with {@link IPlottingTask#updatePlottingProgress(double, double)} ()}
     */
    void doProcess();

    /**
     * Called once after the process has finished
     */
    default void postProcess(){}

    default void onStopped(){}

    default void onReset(){}

}