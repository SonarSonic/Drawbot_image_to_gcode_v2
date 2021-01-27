package drawingbot.files;

import drawingbot.DrawingBotV3;
import drawingbot.drawing.ObservableDrawingPen;
import drawingbot.plotting.PlottingTask;
import drawingbot.plotting.PlottedLine;
import drawingbot.utils.Limit;

import java.io.File;
import java.io.PrintWriter;
import java.util.function.BiFunction;

import static processing.core.PApplet.*;

public class GCodeExporter {

    private static PrintWriter output;

    private static void gcodeHeader(PlottingTask task) {
        output.println("G21"); //programming in millimeters, mm
        output.println("G90"); //programming in absolute positioning
        //output.println("G28"); //auto home
        output.println(movePenUp());
    }

    private static void gcodeTrailer(PlottingTask task) {
        output.println(movePenUp());
        output.println(movePen(0.1F, 0.1F));
        output.println(movePen(0, 0));
    }

    /**moves the pen using GCODE Number Format*/
    private static String movePen(float xValue, float yValue){
        return "G1 X" + gcodeFormat(xValue) + " Y" + gcodeFormat(yValue);
    }

    private static String movePenUp(){
        return "G1 Z0"; //TODO MAKE THIS NUMBER CHANGEABLE!!
    }

    private static String movePenDown(){
        return "G1 Z1"; //TODO MAKE THIS NUMBER CHANGEABLE!!
    }

    private static String comment(String comment){
        return "(" + comment.replace(")", "") + ")" + "\n";
    }

    private static void addComment(String comment){
        output.println(comment(comment));
    }

    /**formats the value into GCODE Number Format*/
    private static String gcodeFormat(Float value) {
        String s = nf(value, 0, DrawingBotV3.gcode_decimals);
        s = s.replace('.', DrawingBotV3.gcode_decimal_seperator);
        s = s.replace(',', DrawingBotV3.gcode_decimal_seperator);
        return s;
    }

    public static void exportGCode(ExportTask exportTask, PlottingTask plottingTask, BiFunction<PlottedLine, ObservableDrawingPen, Boolean> lineFilter, String extension, File saveLocation) {
        output = createWriter(saveLocation);
        plottingTask.comments.forEach(GCodeExporter::addComment); //add all task comments
        gcodeHeader(plottingTask);


        Limit dx = new Limit(), dy = new Limit();
        boolean is_pen_down = false;
        int lines_drawn = 0;
        int pen_lifts = 2;
        float pen_movement = 0;
        float pen_drawing = 0;
        float x = 0, y = 0;

        int completedLines = 0;

        // Loop over all lines for every pen.
        for (int p = 0; p < plottingTask.plottedDrawing.getPenCount(); p ++) {
            ObservableDrawingPen drawingPen = plottingTask.plottedDrawing.drawingPenSet.getPens().get(p);
            for (int i = 0 ; i < plottingTask.plottedDrawing.getDisplayedLineCount(); i ++) {
                PlottedLine line = plottingTask.plottedDrawing.plottedLines.get(i);
                if(line.pen_number == p){
                    if (lineFilter.apply(line, drawingPen)) { // we apply the line filter also.

                        float gcode_scaled_x1 = line.x1 * plottingTask.gcode_scale + plottingTask.gcode_offset_x;
                        float gcode_scaled_y1 = line.y1 * plottingTask.gcode_scale + plottingTask.gcode_offset_y;
                        float gcode_scaled_x2 = line.x2 * plottingTask.gcode_scale + plottingTask.gcode_offset_x;
                        float gcode_scaled_y2 = line.y2 * plottingTask.gcode_scale + plottingTask.gcode_offset_y;
                        float distance = sqrt( sq(abs(gcode_scaled_x1 - gcode_scaled_x2)) + sq(abs(gcode_scaled_y1 - gcode_scaled_y2)) );

                        if (x != gcode_scaled_x1 || y != gcode_scaled_y1) {
                            // Oh crap, where the line starts is not where I am, pick up the pen and move there.
                            output.println(movePenUp());
                            is_pen_down = false;
                            distance = sqrt( sq(abs(x - gcode_scaled_x1)) + sq(abs(y - gcode_scaled_y1)) );
                            output.println(movePen(gcode_scaled_x1, gcode_scaled_y1));
                            x = gcode_scaled_x1;
                            y = gcode_scaled_y1;
                            pen_movement += distance;
                            pen_lifts++;
                        }

                        if (line.pen_down) {
                            if (!is_pen_down) {
                                output.println(movePenDown());
                                is_pen_down = true;
                            }
                            pen_drawing += distance;
                            lines_drawn++;
                        } else {
                            if (is_pen_down) {
                                output.println(movePenUp());
                                is_pen_down = false;
                                pen_movement += distance;
                                pen_lifts++;
                            }
                        }
                        output.println(movePen(gcode_scaled_x2, gcode_scaled_y2));
                        x = gcode_scaled_x2;
                        y = gcode_scaled_y2;
                        dx.update_limit(gcode_scaled_x2);
                        dy.update_limit(gcode_scaled_y2);
                    }
                    completedLines++;
                }
                exportTask.updateProgress(completedLines, plottingTask.plottedDrawing.getDisplayedLineCount());
            }
        }

        gcodeTrailer(plottingTask);
        output.println(comment("Drew " + lines_drawn + " lines for " + pen_drawing  / 25.4 / 12 + " feet"));
        output.println(comment("Pen was lifted " + pen_lifts + " times for " + pen_movement  / 25.4 / 12 + " feet"));
        output.println(comment("Extremes of X: " + dx.min + " thru " + dx.max));
        output.println(comment("Extremes of Y: " + dy.min + " thru " + dy.max));
        output.flush();
        output.close();
        output = null;
        println("GCode File Created:  " + saveLocation);
    }

    //TODO PREVENT GCODE TEST FILES BEING EXPORTED "PER PEN"
    public static void createGcodeTestFile(ExportTask exportTask, PlottingTask plottingTask, BiFunction<PlottedLine, ObservableDrawingPen, Boolean> lineFilter, String extension, File saveLocation) {

        Limit dx = new Limit(), dy = new Limit();
        for (PlottedLine line : plottingTask.plottedDrawing.plottedLines) { //to allow the export of the gcode test file seperately we must update the limits
            float gcode_scaled_x1 = line.x1 * plottingTask.gcode_scale + plottingTask.gcode_offset_x;
            float gcode_scaled_y1 = line.y1 * plottingTask.gcode_scale + plottingTask.gcode_offset_y;
            float gcode_scaled_x2 = line.x2 * plottingTask.gcode_scale + plottingTask.gcode_offset_x;
            float gcode_scaled_y2 = line.y2 * plottingTask.gcode_scale + plottingTask.gcode_offset_y;

            dx.update_limit(gcode_scaled_x1);
            dx.update_limit(gcode_scaled_x2);

            dy.update_limit(gcode_scaled_y1);
            dy.update_limit(gcode_scaled_y2);
        }


        float test_length = 25.4F * 2F; //TODO CHECK ME?

        String gname = FileUtils.removeExtension(saveLocation) + "gcode_test" + extension;
        output = DrawingBotV3.INSTANCE.createWriter(gname);
        output.println(comment("This is a test file to draw the extremes of the drawing area."));
        output.println(comment("Draws a 2 inch mark on all four corners of the paper."));
        output.println(comment("WARNING:  pen will be down."));
        output.println(comment("Extremes of X: " + dx.min + " thru " + dx.max));
        output.println(comment("Extremes of Y: " + dy.min + " thru " + dy.max));
        gcodeHeader(plottingTask);

        output.println(comment("Upper left"));
        output.println(movePen(dx.min, dy.min + test_length));
        output.println(movePenDown());
        output.println(movePen(dx.min, dy.min));
        output.println(movePen(dx.min + test_length, dy.min));
        output.println(movePenUp());

        output.println(comment("Upper right"));
        output.println(movePen(dx.max - test_length, dy.min));
        output.println(movePenDown());
        output.println(movePen(dx.max, dy.min));
        output.println(movePen(dx.max, dy.min + test_length));
        output.println(movePenUp());

        output.println(comment("Lower right"));
        output.println(movePen(dx.max,dy.max - test_length));
        output.println(movePenDown());
        output.println(movePen(dx.max, dy.max));
        output.println(movePen(dx.max - test_length, dy.max));
        output.println(movePenUp());

        output.println(comment("Lower left"));
        output.println(movePen(dx.min + test_length, dy.max));
        output.println(movePenDown());
        output.println(movePen(dx.min, dy.max));
        output.println(movePen(dx.min, dy.max - test_length));
        output.println(movePenUp());

        gcodeTrailer(plottingTask);
        output.flush();
        output.close();
        output = null;
        println("GCode Test Created:  " + gname);

        exportTask.updateProgress(1,1);
    }


}
