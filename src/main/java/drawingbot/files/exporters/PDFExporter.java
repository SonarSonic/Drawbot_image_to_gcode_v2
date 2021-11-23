package drawingbot.files.exporters;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import drawingbot.files.ExportTask;
import drawingbot.geom.basic.IGeometry;
import drawingbot.plotting.PlottingTask;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

public class PDFExporter {

    public static void exportPDF(ExportTask exportTask, PlottingTask plottingTask, Map<Integer, List<IGeometry>> geometries, String extension, File saveLocation) {
        try {
            int width = (int)exportTask.exportResolution.getScaledWidth();
            int height = (int)exportTask.exportResolution.getScaledHeight();

            Document document = new Document(new Rectangle(width, height));
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(saveLocation));

            document.open();
            PdfContentByte content = writer.getDirectContent();
            Graphics2D graphics = new PdfGraphics2D(content, width, height);

            Graphics2DExporter.drawBackground(exportTask, graphics, width, height, plottingTask);
            Graphics2DExporter.preDraw(exportTask, graphics, width, height, plottingTask);
            Graphics2DExporter.drawGeometryWithDrawingSet(exportTask, graphics, plottingTask.getDrawingSet(), geometries);
            Graphics2DExporter.postDraw(exportTask, graphics, width, height, plottingTask);
            document.close(); //dispose is already called within drawGraphics

        } catch (DocumentException | FileNotFoundException e) {
            exportTask.setError(e.getMessage());
            e.printStackTrace();
        }
    }
}
