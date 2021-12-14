package drawingbot.files;

import drawingbot.DrawingBotV3;
import drawingbot.javafx.observables.ObservableDrawingSet;
import drawingbot.geom.basic.IGeometry;
import drawingbot.image.BufferedImageLoader;
import drawingbot.pfm.PFMFactory;
import drawingbot.plotting.PlottingTask;
import drawingbot.utils.EnumColourSplitter;
import javafx.concurrent.Task;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class BatchProcessingTask extends Task<Boolean> {

    public final String inputFolder;
    public final String outputFolder;
    public final PFMFactory<?> pfmFactory;
    public final ObservableDrawingSet drawingPenSet;
    public final EnumColourSplitter splitter;

    public BatchProcessingTask(String inputFolder, String outputFolder){
        this.inputFolder = inputFolder;
        this.outputFolder = outputFolder;
        this.pfmFactory = DrawingBotV3.INSTANCE.pfmFactory.get();
        this.drawingPenSet = new ObservableDrawingSet(DrawingBotV3.INSTANCE.observableDrawingSet);
        this.splitter = DrawingBotV3.INSTANCE.colourSplitter.get();
    }

    @Override
    protected void setException(Throwable t) {
        super.setException(t);
        BatchProcessing.finishProcessing();
        DrawingBotV3.logger.log(Level.SEVERE, "Batch Processing Task Failed", t);
    }

    @Override
    protected Boolean call() throws Exception {
        PathMatcher imageMatcher = FileSystems.getDefault().getPathMatcher("glob:*.{tif,tga,png,jpg,gif,bmp,jpeg}");
        List<Path> files = Files.list(new File(inputFolder).toPath()).filter(Files::isRegularFile).filter(Files::isReadable).filter(p -> imageMatcher.matches(p.getFileName())).collect(Collectors.toList());
        DrawingBotV3.logger.fine("Batch Processing: Found " + files.size());

        if(!files.isEmpty()){

            ExecutorService service = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "DrawingBotV3 - Batch Processing Thread");
                t.setDaemon(true);
                return t ;
            });

            int imageCount = files.size();
            int imagesDone = 0;

            loop: for(Path path : files){
                updateTitle("Batch Processing " + (imagesDone +1) + " / " + imageCount);
                String simpleFileName = FileUtils.removeExtension(path.getFileName().toString());
                if(BatchProcessing.overwriteExistingFiles.get() || BatchProcessing.exportTasks.stream().anyMatch(b -> b.hasMissingFiles(outputFolder, simpleFileName, drawingPenSet))){
                    BufferedImage image = BufferedImageLoader.loadImage(path.toString(), false);
                    PlottingTask internalTask = DrawingBotV3.INSTANCE.initPlottingTask(pfmFactory, new ObservableDrawingSet(drawingPenSet), image, new File(path.toString()), splitter);
                    internalTask.isSubTask = true;
                    Future<?> futurePlottingTask = service.submit(internalTask);
                    while(!futurePlottingTask.isDone()){
                        ///wait
                        if(isCancelled()){
                            futurePlottingTask.cancel(true);
                            service.shutdown();
                            break loop;
                        }
                    }
                    if(!isCancelled()){
                        List<ExportTask> tasks = new ArrayList<>();
                        for(BatchProcessing.BatchExportTask task : BatchProcessing.exportTasks){
                            if(BatchProcessing.overwriteExistingFiles.get() || task.hasMissingFiles(outputFolder, simpleFileName, drawingPenSet)){
                                File saveLocation = new File(outputFolder + "\\" + simpleFileName + task.getCleanExtension());
                                if(task.enablePerDrawing.get()){
                                    tasks.add(new ExportTask(task.format, internalTask, IGeometry.DEFAULT_FILTER, task.getCleanExtension(), saveLocation, false, BatchProcessing.overwriteExistingFiles.get(), false, true, internalTask.resolution));

                                }
                                if(task.enablePerPen.get()){
                                    tasks.add(new ExportTask(task.format, internalTask, IGeometry.DEFAULT_FILTER, task.getCleanExtension(), saveLocation, true, BatchProcessing.overwriteExistingFiles.get(), false, true, internalTask.resolution));
                                }
                            }
                        }
                        for(ExportTask task : tasks){
                            Future<?> futureExportTask = service.submit(task);
                            while(!futureExportTask.isDone()){
                                ///wait
                                if(isCancelled()){
                                    futureExportTask.cancel(true);
                                    service.shutdown();
                                    break loop;
                                }
                            }
                        }
                    }
                }
                imagesDone++;
            }
            service.shutdown();
        }
        BatchProcessing.finishProcessing();

        return null;
    }

}
