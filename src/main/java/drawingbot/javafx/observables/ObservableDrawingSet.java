package drawingbot.javafx.observables;

import com.google.gson.annotations.JsonAdapter;
import drawingbot.DrawingBotV3;
import drawingbot.api.IDrawingPen;
import drawingbot.api.IDrawingSet;
import drawingbot.drawing.ColourSeperationHandler;
import drawingbot.files.json.adapters.JsonAdapterObservableDrawingSet;
import drawingbot.registry.Register;
import drawingbot.utils.EnumDistributionOrder;
import drawingbot.utils.EnumDistributionType;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

@JsonAdapter(JsonAdapterObservableDrawingSet.class)
public class ObservableDrawingSet implements IDrawingSet<ObservableDrawingPen> {

    public final SimpleStringProperty type = new SimpleStringProperty();
    public final SimpleStringProperty name = new SimpleStringProperty();
    public final ObservableList<ObservableDrawingPen> pens = FXCollections.observableArrayList();
    public final SimpleObjectProperty<EnumDistributionOrder> distributionOrder = new SimpleObjectProperty<>();
    public final SimpleObjectProperty<EnumDistributionType> distributionType = new SimpleObjectProperty<>();
    public final SimpleObjectProperty<ColourSeperationHandler> colourSeperator = new SimpleObjectProperty<>();

    private transient int[] currentRenderOrder;

    public transient boolean loadingDrawingSet = false;

    public ObservableDrawingSet(){}

    public ObservableDrawingSet(IDrawingSet<?> source){
        this.distributionOrder.set(EnumDistributionOrder.DARKEST_FIRST);
        this.distributionType.set(EnumDistributionType.EVEN_WEIGHTED);
        this.colourSeperator.set(Register.DEFAULT_COLOUR_SPLITTER);

        loadDrawingSet(source);
    }

    public void loadDrawingSet(IDrawingSet<?> source){
        loadingDrawingSet = true;

        if(source instanceof ObservableDrawingSet){
            ObservableDrawingSet drawingSet = (ObservableDrawingSet)source;
            this.distributionOrder.set(drawingSet.distributionOrder.get());
            this.distributionType.set(drawingSet.distributionType.get());
            this.colourSeperator.set(drawingSet.colourSeperator.get() == null ? Register.DEFAULT_COLOUR_SPLITTER : drawingSet.colourSeperator.get());
        }

        this.pens.clear();
        this.type.set(source.getType());
        this.name.set(source.getName());
        for(IDrawingPen pen : source.getPens()){
            pens.add(new ObservableDrawingPen(pens.size(), pen));
        }
        this.currentRenderOrder = calculateRenderOrder();

        loadingDrawingSet = false;
        Platform.runLater(() -> DrawingBotV3.INSTANCE.onDrawingSetChanged()); //TODO REMOVE ME!
    }

    public void mergePens(List<ObservableDrawingPen> pens){
        for(IDrawingPen pen : pens){
            if(!containsPen(pen)){
                pens.add(new ObservableDrawingPen(pens.size(), pen));
            }
        }
    }

    public ObservableDrawingPen addNewPen(IDrawingPen pen){
        ObservableDrawingPen newPen = new ObservableDrawingPen(pens.size(), pen);
        pens.add(newPen);
        return newPen;
    }

    public int[] calculateRenderOrder(){
        List<ObservableDrawingPen> sortedList = new ArrayList<>(pens);
        sortedList.sort(distributionOrder.get().comparator);
        currentRenderOrder = new int[sortedList.size()];
        for(int i = 0; i < sortedList.size(); i++){
            currentRenderOrder[i] = sortedList.get(i).penNumber.get();
        }
        return currentRenderOrder;
    }

    public int getIndexOfPen(int penNumber){
        int index = 0;
        for(ObservableDrawingPen pen : pens){
            if(pen.penNumber.get() == penNumber){
                return index;
            }
            index++;
        }
        return -1;
    }

    public ObservableDrawingPen getPen(int penNumber){
        for(ObservableDrawingPen pen : pens){
            if(pen.penNumber.get() == penNumber){
                return pen;
            }
        }
        return Register.INSTANCE.INVISIBLE_DRAWING_PEN;
    }

    public boolean containsPen(IDrawingPen pen){
        if(pen == null){
            return false;
        }
        return pens.stream().anyMatch(p -> p.getCodeName().equals(pen.getCodeName()));
    }

    @Override
    public String getType() {
        return type.get();
    }

    @Override
    public String getName() {
        return name.get();
    }

    @Override
    public List<ObservableDrawingPen> getPens() {
        return pens;
    }

    @Override
    public String toString(){
        return getName();
    }

}
