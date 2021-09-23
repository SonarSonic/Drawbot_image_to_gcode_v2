package drawingbot.utils;

import drawingbot.javafx.observables.ObservableDrawingPen;
import drawingbot.image.ImageTools;

import java.util.Comparator;

public enum EnumDistributionOrder {

    DARKEST_FIRST(Comparator.comparingInt(pen -> ImageTools.getPerceivedLuminanceFromRGB(pen.getARGB()))),
    LIGHTEST_FIRST(Comparator.comparingInt(pen -> -ImageTools.getPerceivedLuminanceFromRGB(pen.getARGB()))),
    DISPLAYED(Comparator.comparingInt(pen -> pen.penNumber.get())),
    REVERSED(Comparator.comparingInt(pen -> -pen.penNumber.get()));

    public final Comparator<ObservableDrawingPen> comparator;

    EnumDistributionOrder(Comparator<ObservableDrawingPen> comparator){
        this.comparator = comparator;
    }

    @Override
    public String toString() {
        if(this == DARKEST_FIRST){
            return Utils.capitalize(name()) + " " + "(Default)";
        }
        return Utils.capitalize(name());
    }

}