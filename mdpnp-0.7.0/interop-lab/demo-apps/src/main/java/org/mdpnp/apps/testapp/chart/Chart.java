package org.mdpnp.apps.testapp.chart;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.mdpnp.apps.device.OnListChange;
import org.mdpnp.apps.testapp.vital.Value;
import org.mdpnp.apps.testapp.vital.Vital;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

public class Chart {
    @FXML protected LineChart<Date, Number> lineChart;
    final ObservableList<XYChart.Series<Date, Number>> series = FXCollections.observableArrayList();
    private final List<ValueSeriesListener> values = new ArrayList<ValueSeriesListener>();
    @FXML Button removeButton;
    @FXML BorderPane main;
    
    // TODO externalize this setting
    private static final int MAX_POINTS = 25000;
    
    private Vital vital;
    
    public Vital getVital() {
        return vital;
    }
    public Button getRemoveButton() {
        return removeButton;
    }
    
    private final OnListChange<Value> valueListener = new OnListChange<>(
            (t)->add(t), null, (t)->remove(t));
    
    public void setModel(Vital v, final DateAxis dateAxis) {
        
        if(null != this.vital) {
            this.vital.removeListener(valueListener);
            this.vital.forEach((t)->remove(t));
            main.setCenter(null);
            lineChart.titleProperty().unbind();
            lineChart = null;
        }
        this.vital = v;
        if(null != v) {
            NumberAxis yAxis = new NumberAxis();
            lineChart = new LineChart<>(dateAxis, yAxis);
            lineChart.setMinHeight(250.0);
            lineChart.setAnimated(false);
            lineChart.setCreateSymbols(false);
            
            lineChart.titleProperty().bind(v.labelProperty());
            lineChart.setData(series);
            main.setCenter(lineChart);
            BorderPane.setAlignment(lineChart, Pos.CENTER);
            v.addListener(valueListener);
            v.forEach((t)->add(t));
            yAxis.setForceZeroInRange(false);
            yAxis.setAutoRanging(false);
            yAxis.setUpperBound(v.getMaximum());
            yAxis.setLowerBound(v.getMinimum());
        }
    }

    private void add(final Value vital) {
        ValueSeriesListener vsl = new ValueSeriesListener();
        vsl.v = vital;
        values.add(vsl);
        
        final ObservableList<XYChart.Data<Date, Number>> data = FXCollections.observableArrayList();
        vsl.s = new XYChart.Series<>(data);
        vsl.s.nameProperty().bind(vsl.v.getDevice().makeAndModelProperty());
        series.add(vsl.s);
        vsl.v.timestampProperty().addListener(vsl.l = new ChangeListener<Date>() {

            @Override
            public void changed(ObservableValue<? extends Date> observable, Date oldValue, Date newValue) {
                if(newValue != null) {
                    if(data.size()>MAX_POINTS) {
                        data.remove(0);
                    }
                    data.add(new XYChart.Data<>(newValue, vsl.v.getValue()));
                }
            }
            
        });

        
    }
    
    private void remove(final Value v) {
        Iterator <ValueSeriesListener> vslitr = values.iterator();
        while(vslitr.hasNext()) {
            ValueSeriesListener vsl = vslitr.next();
            if(vsl.v.equals(v)) {
                vsl.v.timestampProperty().removeListener(vsl.l);
                series.remove(vsl.s);
                vslitr.remove();
            }
        }

    }
    
    private static class ValueSeriesListener { 
        public XYChart.Series<Date, Number> s;
        public Value v;
        public ChangeListener<Date> l;
    }
    
}
