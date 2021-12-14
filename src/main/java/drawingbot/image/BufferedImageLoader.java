package drawingbot.image;

import drawingbot.DrawingBotV3;
import drawingbot.files.FileUtils;
import javafx.concurrent.Task;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

public class BufferedImageLoader extends Task<BufferedImage> {

    public String url;
    public boolean internal; //true if the image should be loaded from within the jar

    public BufferedImageLoader(String url, boolean internal){
        this.url = url;
        this.internal = internal;
    }

    @Override
    protected BufferedImage call() throws Exception {
        return loadImage(url, internal);
    }

    @Override
    protected void setException(Throwable t) {
        super.setException(t);
        DrawingBotV3.logger.log(Level.SEVERE, "Buffered Image Loader Failed", t);
    }

    public static FilteredBufferedImage loadFilteredImage(String url, boolean internal) throws IOException {
        BufferedImage source = loadImage(url, internal);
        if(source != null){
            FilteredBufferedImage filtered = new FilteredBufferedImage(source);
            filtered.updateAll();
            return filtered;
        }
        return null;
    }

    public static BufferedImage loadImage(String url, boolean internal) throws IOException {

        boolean isVideo = FileUtils.matchesExtensionFilter(FileUtils.getExtension(url), FileUtils.IMPORT_VIDEOS);
        if(isVideo){
            try {
                return loadImageFromVideo(url);
            } catch (JCodecException e) {
                e.printStackTrace();
            }
            return null;
        }

        InputStream stream;
        if(internal){
            stream = DrawingBotV3.class.getClassLoader().getResourceAsStream(url);
        }else{
            stream = new FileInputStream(url);
        }

        BufferedImage img = null;
        if(stream != null){
            img = ImageIO.read(stream);
        }

        return convertToARGB(img);
    }

    /**
     * Loads the first image from a video
     */
    public static BufferedImage loadImageFromVideo(String url) throws IOException, JCodecException {
        Picture picture = FrameGrab.getFrameFromFile(new File(url), 0);
        return convertToARGB(AWTUtil.toBufferedImage(picture));
    }

    public static BufferedImage convertToARGB(BufferedImage img){
        if(img != null){
            BufferedImage optimalImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = optimalImg.createGraphics();
            graphics.drawImage(img, 0, 0, null);
            graphics.dispose();
            return optimalImg;
        }
        return null;
    }

    public static class Filtered extends Task<FilteredBufferedImage> {

        public String url;
        public boolean internal; //true if the image should be loaded from within the jar

        public Filtered(String url, boolean internal){
            this.url = url;
            this.internal = internal;
        }

        @Override
        protected FilteredBufferedImage call() throws Exception {

            updateProgress(-1, 1);
            updateTitle("Importing Image: " + url);

            updateMessage("Loading");
            BufferedImage source = loadImage(url, internal);

            updateMessage("Filtering");
            FilteredBufferedImage filtered = new FilteredBufferedImage(source);
            filtered.updateAll();

            updateMessage("Finished");
            updateProgress(1, 1);
            return filtered;
        }
    }
}
