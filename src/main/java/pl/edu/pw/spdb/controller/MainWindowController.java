package pl.edu.pw.spdb.controller;

import com.sothawo.mapjfx.*;
import com.sothawo.mapjfx.event.MapLabelEvent;
import com.sothawo.mapjfx.event.MapViewEvent;
import com.sothawo.mapjfx.event.MarkerEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.paint.Color;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

@Component
public class MainWindowController implements Initializable {
    private final Coordinate pkinCoordinate = new Coordinate(52.231667, 21.006389);
    private final Coordinate warsawSpireCoordinate = new Coordinate(52.23316, 20.98476
    );
    private final Coordinate royalCastleCoordinate = new Coordinate(52.247778, 21.014167);

    private final Extent extentAll =
            Extent.forCoordinates(pkinCoordinate, warsawSpireCoordinate, royalCastleCoordinate);
    private final CoordinateLine coordinateLine =
            new CoordinateLine(pkinCoordinate, warsawSpireCoordinate, royalCastleCoordinate)
                    .setVisible(true)
                    .setColor(Color.DODGERBLUE)
                    .setWidth(7)
                    .setClosed(true)
                    .setFillColor(Color.web("lawngreen", 0.5));
    private final ToggleGroup radioButtonsGroup = new ToggleGroup();

    @FXML
    private Label costParameterLabel;
    @FXML
    private Slider optionsSlider;
    @FXML
    private RadioButton shortestPathRadioButton, shortestTimeRadioButton, advancedRadioButton;
    private Marker marker;
    private MapCircle circle;
    private MapLabel mapLabel;
    private WMSParam wmsParam;
    private XYZParam xyzParam;
    @FXML
    private MapView mapView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeRadioButtons();
        initializeOptionsSlider();

        marker = Marker.createProvided(Marker.Provided.BLUE)
                .setPosition(pkinCoordinate)
                .setRotation(90)
                .setVisible(true);

        mapLabel = new MapLabel("blau!")
                .setCssClass("blue-label")
                .setPosition(pkinCoordinate)
                .setRotation(90)
                .setVisible(true);

        marker.attachLabel(mapLabel);

        circle = new MapCircle(pkinCoordinate, 1_000).setVisible(true);

        wmsParam = new WMSParam()
                .setUrl("http://ows.terrestris.de/osm/service")
                .addParam("layers", "OSM-WMS");

        xyzParam = new XYZParam()
                .withUrl("https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x})")
                .withAttributions("'Tiles &copy; <a href=\"https://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer\">ArcGIS</a>'");

        mapView.setAnimationDuration(500);

        mapView.setWMSParam(wmsParam);

        //add XYZParam
        mapView.setXYZParam(xyzParam);

        // listen to MapViewEvent MAP_CLICKED
        mapView.addEventHandler(MapViewEvent.MAP_CLICKED, event -> {
            event.consume();
            if (marker.getVisible()) {
                marker.setPosition(event.getCoordinate());
            }
            if (mapLabel.getVisible()) {
                mapLabel.setPosition(event.getCoordinate());
            }
        });

        // listen to MapViewEvent MAP_RIGHTCLICKED
        mapView.addEventHandler(MapViewEvent.MAP_RIGHTCLICKED, event -> {
            event.consume();
        });

        // listen to MapViewEvent MAP_EXTENT
        mapView.addEventHandler(MapViewEvent.MAP_EXTENT, event -> {
            mapView.setExtent(event.getExtent());
            event.consume();
        });

        // listen to MapViewEvent MAP_BOUNDING_EXTENT
        mapView.addEventHandler(MapViewEvent.MAP_BOUNDING_EXTENT, event -> {
            event.consume();
        });

        // listen to MARKER_CLICKED event.
        mapView.addEventHandler(MarkerEvent.MARKER_CLICKED, event -> {
            Marker marker = event.getMarker();
            event.consume();
            marker.setRotation(marker.getRotation() + 5);
        });

        // listen to MARKER_MOUSEDOWN event.
        mapView.addEventHandler(MarkerEvent.MARKER_MOUSEDOWN, event -> {
            event.consume();
        });
        // listen to MARKER_MOUSEUP event.
        mapView.addEventHandler(MarkerEvent.MARKER_MOUSEUP, event -> {
            event.consume();
        });
        // listen to MARKER_DOUBLECLICKED event.
        mapView.addEventHandler(MarkerEvent.MARKER_DOUBLECLICKED, event -> {
            event.consume();
        });
        // listen to MARKER_RIGHTCLICKED event.
        mapView.addEventHandler(MarkerEvent.MARKER_RIGHTCLICKED, event -> {
            event.consume();
        });
        // listen to MARKER_ENTERED event.
        mapView.addEventHandler(MarkerEvent.MARKER_ENTERED, event -> {
            event.consume();
        });
        // listen to MARKER_EXITED event.
        mapView.addEventHandler(MarkerEvent.MARKER_EXITED, event -> {
            event.consume();
        });
        // listen to MAPLABEL_MOUSEDOWN event.
        mapView.addEventHandler(MapLabelEvent.MAPLABEL_MOUSEDOWN, event -> {
            event.consume();
        });
        // listen to MAPLABEL_MOUSEUP event.
        mapView.addEventHandler(MapLabelEvent.MAPLABEL_MOUSEUP, event -> {
            event.consume();
        });
        // listen to MAPLABEL_CLICKED event.
        mapView.addEventHandler(MapLabelEvent.MAPLABEL_CLICKED, event -> {
            event.consume();
        });
        // listen to MAPLABEL_RIGHTCLICKED event.
        mapView.addEventHandler(MapLabelEvent.MAPLABEL_RIGHTCLICKED, event -> {
            event.consume();
        });
        // listen to MAPLABEL_DOUBLECLICKED event.
        mapView.addEventHandler(MapLabelEvent.MAPLABEL_DOUBLECLICKED, event -> {
            event.consume();
        });
        // listen to MAPLABEL_ENTERED event.
        mapView.addEventHandler(MapLabelEvent.MAPLABEL_ENTERED, event -> {
            event.consume();
            event.getMapLabel().setCssClass("green-label");
        });
        // listen to MAPLABEL_EXITED event.
        mapView.addEventHandler(MapLabelEvent.MAPLABEL_EXITED, event -> {
            event.consume();
            event.getMapLabel().setCssClass("blue-label");
        });
        // listen to MAP_POINTER_MOVED event
        mapView.addEventHandler(MapViewEvent.MAP_POINTER_MOVED, event -> {
            event.consume();
        });

        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // a map is only displayed when an initial coordinate is set
                mapView.setCenter(pkinCoordinate);
                mapView.setExtent(extentAll);
//                mapView.setZoom(0);

                // add two markers without keeping a ref to them, they should disappear from the map when gc'ed
                mapView.addMarker(Marker.createProvided(Marker.Provided.GREEN).setPosition(pkinCoordinate)
                        .setVisible(true));
                mapView.addMarker(
                        Marker.createProvided(Marker.Provided.ORANGE).setPosition(warsawSpireCoordinate).setVisible(
                                true));

                // add a coordinate line to be gc'ed
                mapView.addCoordinateLine(
                        new CoordinateLine(pkinCoordinate, warsawSpireCoordinate, royalCastleCoordinate)
                                .setVisible(true)
                                .setColor(Color.FUCHSIA).setWidth(5));

                // add a label to be gc'ed
                mapView.addLabel(new MapLabel("clean me up").setPosition(warsawSpireCoordinate)
                        .setVisible(true));

                // add normal circle and a circle to be gc'ed
                mapView.addMapCircle(circle);
                mapView.addMapCircle(new MapCircle(pkinCoordinate, 100).setVisible(true));
            }
        });
        mapView.setMapType(MapType.OSM);
        mapView.initialize(Configuration.builder()
                .build());
    }

    private void initializeRadioButtons() {
        shortestPathRadioButton.setToggleGroup(radioButtonsGroup);
        shortestTimeRadioButton.setToggleGroup(radioButtonsGroup);
        advancedRadioButton.setToggleGroup(radioButtonsGroup);

        radioButtonsGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (!Objects.equals(oldValue, newValue)) {
                if (newValue == shortestPathRadioButton) {
                    costParameterLabel.setText(String.valueOf(1));
                } else if (newValue == shortestTimeRadioButton) {
                    costParameterLabel.setText(String.valueOf(0));
                } else {
                    costParameterLabel.setText(String.format("%.2f", optionsSlider.getValue()));
                }
            }
        });

        shortestPathRadioButton.setSelected(true);
    }

    private void initializeOptionsSlider() {
        optionsSlider.setMin(0);
        optionsSlider.setMax(1);
        optionsSlider.setMajorTickUnit(0.5f);
        optionsSlider.setShowTickMarks(true);
        optionsSlider.setShowTickLabels(true);
        optionsSlider.setBlockIncrement(0.1f);

        optionsSlider.disableProperty().
                bind(radioButtonsGroup.selectedToggleProperty().isNotEqualTo(advancedRadioButton));

        optionsSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                costParameterLabel.setText(String.format("%.2f", optionsSlider.getValue())));
    }

}
