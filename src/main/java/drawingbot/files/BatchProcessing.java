package drawingbot.files;

import drawingbot.DrawingBotV3;
import drawingbot.javafx.observables.ObservableDrawingPen;
import drawingbot.javafx.observables.ObservableDrawingSet;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class BatchProcessing {

    public static final SimpleBooleanProperty isBatchProcessing = new SimpleBooleanProperty(false);
    public static final SimpleBooleanProperty overwriteExistingFiles = new SimpleBooleanProperty(false);
    public static final SimpleStringProperty inputFolder = new SimpleStringProperty(null);
    public static final SimpleStringProperty outputFolder = new SimpleStringProperty(null);
    public static final ObservableList<BatchExportTask> exportTasks = FXCollections.observableArrayList();

    static{
        for(ExportFormats exportTask : ExportFormats.values()){
            for(FileChooser.ExtensionFilter filter : exportTask.filters){
                BatchExportTask task = new BatchExportTask(exportTask, filter);
                exportTasks.add(task);
                if(filter == FileUtils.FILTER_PNG && exportTask == ExportFormats.EXPORT_IMAGE){
                    task.enablePerDrawing.setValue(true);
                }
            }
        }
    }

    public static void selectFolder(boolean input){
        Platform.runLater(() -> {
            DirectoryChooser d = new DirectoryChooser();
            d.setTitle("Select " + (input ? "Input Folder" : "Output Folder"));
            d.setInitialDirectory(new File(FileUtils.getUserHomeDirectory()));
            File file = d.showDialog(null);
            if(file != null){
                if(input){
                    inputFolder.set(file.getPath());
                }else{
                    outputFolder.set(file.getPath());
                }
            }
        });
    }

    public static void startProcessing(){
        if(inputFolder.get() == null || outputFolder.get() == null){
            return;
        }
        DrawingBotV3.INSTANCE.batchProcessingTask = new BatchProcessingTask(inputFolder.get(), outputFolder.get());
        DrawingBotV3.INSTANCE.taskMonitor.queueTask(DrawingBotV3.INSTANCE.batchProcessingTask);
        isBatchProcessing.setValue(true);
    }

    public static void finishProcessing(){
        if(DrawingBotV3.INSTANCE.batchProcessingTask != null){
            DrawingBotV3.INSTANCE.batchProcessingTask.cancel(true);
            DrawingBotV3.INSTANCE.batchProcessingTask = null;
        }
        isBatchProcessing.setValue(false);
    }


    public static class BatchExportTask{

        public final ExportFormats format;
        public final FileChooser.ExtensionFilter filter;
        public final SimpleBooleanProperty enablePerDrawing = new SimpleBooleanProperty(false);
        public final SimpleBooleanProperty enablePerPen = new SimpleBooleanProperty(false);

        public BatchExportTask(ExportFormats format, FileChooser.ExtensionFilter filter){
            this.format = format;
            this.filter = filter;
        }

        public String formatName(){
            return format.displayName + " " + filter.getExtensions();
        }

        public String getCleanExtension(){
            return filter.getExtensions().get(0).substring(1);
        }

        public boolean hasMissingFiles(String outputFolder, String fileName, ObservableDrawingSet drawingSet){
            List<String> fileNames = getFileNames(fileName, drawingSet);
            for(String name : fileNames){
                File path = new File(outputFolder + "\\" + name);
                if(Files.notExists(path.toPath())){
                    return true;
                }
            }
            return false;
        }

        /** @param fileName the files name without an extension
         * @return list of file names with extensions this export task will create*/
        public List<String> getFileNames(String fileName, ObservableDrawingSet drawingSet){
            List<String> fileNames = new ArrayList<>();
            String extension = getCleanExtension();
            if(enablePerDrawing.get()){
                fileNames.add(fileName + extension);
            }
            if(enablePerPen.get()){
                for (int p = 0; p < drawingSet.pens.size(); p ++) {
                    ObservableDrawingPen drawingPen = drawingSet.pens.get(p);
                    fileNames.add(fileName + "_pen" + p + "_" + drawingPen.getName() + extension);
                }
                fileNames.add(fileName + extension);
            }
            return fileNames;
        }

    }
}
