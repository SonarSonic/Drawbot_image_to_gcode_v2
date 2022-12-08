package drawingbot.utils;

import drawingbot.DrawingBotV3;
import drawingbot.api.IProgressCallback;
import drawingbot.files.json.projects.DBTaskContext;
import javafx.concurrent.Task;

import java.util.logging.Level;

public abstract class DBTask<V> extends Task<V> implements IProgressCallback {

    public final DBTaskContext context;
    public boolean updateProgressInstantly = true;
    private String title = "";
    private String message = "";
    public String error = "";
    public double workDone = -1;
    public double max = 1;

    public DBTask(DBTaskContext context){
        this.context = context;
    }

    public boolean isPlottingTask(){
        return false;
    }

    public void setError(String error){
        this.error = error;
    }

    @Override
    protected void setException(Throwable t) {
        super.setException(t);
        DrawingBotV3.logger.log(Level.SEVERE, "TASK FAILED", t);
        setError(t.getMessage());
    }

    @Override
    public void updateProgress(long workDone, long max) {
        if(updateProgressInstantly){
            super.updateProgress(workDone, max);
        }
        this.workDone = workDone;
        this.max = max;
    }

    @Override
    public void updateProgress(double workDone, double max) {
        if(updateProgressInstantly){
            super.updateProgress(workDone, max);
        }
        this.workDone = workDone;
        this.max = max;
    }

    @Override
    public void updateMessage(String message) {
        if(updateProgressInstantly){
            super.updateMessage(message);
        }
        this.message = message;
    }

    @Override
    public void updateTitle(String title) {
        if(updateProgressInstantly){
            super.updateTitle(title);
        }
        this.title = title;
    }

    //called on JAVA FX Thread

    /**
     * By avoiding calling the super methods straight away we avoid a performance issue with JavaFX
     * In which progress updates are sent repeatedly, wrapped in runnables.
     * If the JavaFX thread runs behind or the progress updates are too frequent this has a massive impact on performance, so instead we call this method.
     * This allows us to update progress / message / title of a task as many times as we wish without a performance impact.
     */
    public void tick(){
        super.updateProgress(workDone, max);
        super.updateMessage(message);
        super.updateTitle(title);
    }

}
