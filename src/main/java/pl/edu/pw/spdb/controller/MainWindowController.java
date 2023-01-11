package pl.edu.pw.spdb.controller;

import com.sothawo.mapjfx.*;
import com.sothawo.mapjfx.event.MapLabelEvent;
import com.sothawo.mapjfx.event.MapViewEvent;
import com.sothawo.mapjfx.event.MarkerEvent;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Pair;
import org.springframework.stereotype.Component;
import pl.edu.pw.spdb.service.SearchPathService;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class MainWindowController implements Initializable {
    private final Coordinate pkinCoordinate = new Coordinate(52.231667, 21.006389);
    private final Coordinate warsawSpireCoordinate = new Coordinate(52.23316, 20.98476);
    private final Coordinate royalCastleCoordinate = new Coordinate(52.247778, 21.014167);


    private final CoordinateLine coordinateLine = new CoordinateLine(pkinCoordinate, warsawSpireCoordinate, royalCastleCoordinate).setVisible(true).setColor(Color.DODGERBLUE).setWidth(7).setClosed(true).setFillColor(Color.web("lawngreen", 0.5));
    private final ToggleGroup radioButtonsGroup = new ToggleGroup();

    @FXML
    private ListView<Pair<String, CoordinateLine>> resultListView;

    private MapViewState mapViewState = MapViewState.DEFAUlT;

    @FXML
    private Button chooseStartPointButton, chooseEndPointButton, searchButton, showResultButton;
    @FXML
    private Label costParameterLabel, applicationStateLabel;

    @FXML
    private TextField startPointCoordinatesTextField, endPointCoordinatesTextField;

    @FXML
    private TextField v1TextField, v2TextField, v3TextField;
    @FXML
    private Slider optionsSlider;
    @FXML
    private RadioButton shortestPathRadioButton, shortestTimeRadioButton, advancedRadioButton;

    private Marker startPointMarker;

    private Marker endPointMarker;
    private Marker marker;
    private MapCircle circle;
    private MapLabel mapLabel;
    private WMSParam wmsParam;
    private XYZParam xyzParam;
    @FXML
    private MapView mapView;

    private final SearchPathService searchPathService;

    public MainWindowController(SearchPathService searchPathService) {
        this.searchPathService = searchPathService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applicationStateLabel.setText("");
        initializeRadioButtons();
        initializeOptionsSlider();
        initializeMapView();
        initializeButtons();
        initializeTextFields();
        resultListView.setCellFactory(x -> new ListCell<>() {
            @Override
            protected void updateItem(Pair<String, CoordinateLine> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item.getKey());
                }
            }
        });
    }

    private void initializeButtons() {
        chooseStartPointButton.setOnMouseClicked(this::handleChooseStartPointButtonClicked);
        chooseEndPointButton.setOnMouseClicked(this::handleChooseEndPointButtonClicked);
        searchButton.setOnMouseClicked(this::handleSearchButtonClicked);
        showResultButton.setOnMouseClicked(this::handleShowResultButtonClicked);
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

        optionsSlider.disableProperty().bind(radioButtonsGroup.selectedToggleProperty().isNotEqualTo(advancedRadioButton));

        optionsSlider.valueProperty().addListener((observable, oldValue, newValue) -> costParameterLabel.setText(String.format("%.2f", optionsSlider.getValue())));
    }

    private void initializeMapView() {
        startPointMarker = Marker.createProvided(Marker.Provided.GREEN).setVisible(false);
        endPointMarker = Marker.createProvided(Marker.Provided.RED).setVisible(false);

        marker = Marker.createProvided(Marker.Provided.BLUE).setPosition(pkinCoordinate).setRotation(90).setVisible(true);

        mapLabel = new MapLabel("blau!").setCssClass("blue-label").setPosition(pkinCoordinate).setRotation(90).setVisible(true);

        marker.attachLabel(mapLabel);

        circle = new MapCircle(pkinCoordinate, 1_000).setVisible(true);

        wmsParam = new WMSParam().setUrl("http://ows.terrestris.de/osm/service").addParam("layers", "OSM-WMS");

        xyzParam = new XYZParam().withUrl("https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x})").withAttributions("'Tiles &copy; <a href=\"https://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer\">ArcGIS</a>'");

        mapView.setAnimationDuration(500);

        mapView.setWMSParam(wmsParam);

        //add XYZParam
        mapView.setXYZParam(xyzParam);

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

        mapView.addEventHandler(MapViewEvent.MAP_CLICKED, this::handleClickOnMapView);

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

        mapView.setCenter(pkinCoordinate);

        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                /*
                // a map is only displayed when an initial coordinate is set
                mapView.setExtent(extentAll);
                mapView.setZoom(28);

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
                mapView.addMapCircle(new MapCircle(pkinCoordinate, 100).setVisible(true));*/
            }
        });
        mapView.setMapType(MapType.OSM);
        mapView.initialize(Configuration.builder().build());
    }

    private void handleChooseStartPointButtonClicked(MouseEvent e) {
        if (mapViewState != MapViewState.CHOOSING_START_POINT) {
            mapViewState = MapViewState.CHOOSING_START_POINT;
        } else {
            mapViewState = MapViewState.DEFAUlT;
        }
        changeApplicationStateLabel();
    }

    private void handleChooseEndPointButtonClicked(MouseEvent e) {
        if (mapViewState != MapViewState.CHOOSING_END_POINT) {
            mapViewState = MapViewState.CHOOSING_END_POINT;
        } else {
            mapViewState = MapViewState.DEFAUlT;
        }
        changeApplicationStateLabel();
    }

    private void changeApplicationStateLabel() {

        String applicationState = "";

        switch (mapViewState) {
            case DEFAUlT -> applicationState = "";
            case CHOOSING_END_POINT -> applicationState = "Wybór punktu końcowego";
            case CHOOSING_START_POINT -> applicationState = "Wybór punktu startowego";
        }

        String finalApplicationState = applicationState;
        Platform.runLater(() -> applicationStateLabel.setText(finalApplicationState));
    }

    private void initializeTextFields() {
        v1TextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                validateSpeedTextField(v1TextField);
            }
        });
        v2TextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                validateSpeedTextField(v2TextField);
            }
        });
        v3TextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                validateSpeedTextField(v3TextField);
            }
        });
    }

    private void validateSpeedTextField(TextField textField) {
        if (!textField.getText().matches("^[0-9]+$")) {
            textField.setText("");
        }
    }

    private String getCoordinatesString(Coordinate coordinate) {
        return String.format("%.6f", coordinate.getLongitude()) + ", " + String.format("%.6f", coordinate.getLatitude());
    }

    private void handleClickOnMapView(MapViewEvent event) {
        switch (mapViewState) {
            case CHOOSING_END_POINT -> {
                endPointMarker.setPosition(event.getCoordinate()).setVisible(true);
                Platform.runLater(() -> {
                    mapView.addMarker(endPointMarker);
                    endPointCoordinatesTextField.setText(getCoordinatesString(endPointMarker.getPosition()));
                });

                mapViewState = MapViewState.DEFAUlT;
            }

            case CHOOSING_START_POINT -> {
                startPointMarker.setPosition(event.getCoordinate()).setVisible(true);
                Platform.runLater(() -> {
                    mapView.addMarker(startPointMarker);
                    startPointCoordinatesTextField.setText(getCoordinatesString(startPointMarker.getPosition()));
                });

                mapViewState = MapViewState.DEFAUlT;
            }
            default -> {
            }
        }
        changeApplicationStateLabel();
    }

    private void cleanCoordinateLinesFromMapView() {
        for (var element : resultListView.getItems()) {
            mapView.removeCoordinateLine(element.getValue());
        }
    }

    private void cleanMapView() {
        mapView.removeMarker(startPointMarker);
        mapView.removeMarker(endPointMarker);
        cleanCoordinateLinesFromMapView();
    }

    private void handleSearchButtonClicked(MouseEvent event) {
        if (startPointCoordinatesTextField.getText().isBlank() || endPointCoordinatesTextField.getText().isBlank()) {
            createAlert("Błąd", "Brak współrzędnych", "Podaj punkt początkowy i końcowy w prawidłowy sposób").showAndWait();
        } else if (v1TextField.getText().isBlank() || v2TextField.getText().isBlank() || v3TextField.getText().isBlank()) {
            createAlert("Błąd", "Brak prędkości", "Uzupełnij brakujące prędkości").showAndWait();
        } else {
            cleanCoordinateLinesFromMapView();
            resultListView.getItems().clear();

            SearchRouteTask v1SearchTask = new SearchRouteTask(startPointMarker.getPosition(), endPointMarker.getPosition(), Integer.parseInt(v1TextField.getText()), Double.parseDouble(costParameterLabel.getText()));
            prepareTask(v1SearchTask);
            SearchRouteTask v2SearchTask = new SearchRouteTask(startPointMarker.getPosition(), endPointMarker.getPosition(), Integer.parseInt(v2TextField.getText()), Double.parseDouble(costParameterLabel.getText()));
            prepareTask(v2SearchTask);
            SearchRouteTask v3SearchTask = new SearchRouteTask(startPointMarker.getPosition(), endPointMarker.getPosition(), Integer.parseInt(v3TextField.getText()), Double.parseDouble(costParameterLabel.getText()));
            prepareTask(v3SearchTask);

            ExecutorService executorService = Executors.newFixedThreadPool(3);
            executorService.execute(v1SearchTask);
            executorService.execute(v2SearchTask);
            executorService.execute(v3SearchTask);
            executorService.shutdown();
        }
    }

    private void prepareTask(SearchRouteTask task) {
        Stage dialog = new Stage();
        dialog.setWidth(300);
        dialog.setHeight(150);
        dialog.initStyle(StageStyle.UNDECORATED);
        Scene scene = new Scene(new Group(new Text(50, 50, "Trwa komunikacja z bazą danych")));
        dialog.setScene(scene);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(searchButton.getScene().getWindow());

        task.setOnFailed((failed) -> {
            dialog.hide();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Błąd podczas szukania najlepszej ścieżki");
            alert.setHeaderText("Błąd podczas szukania najlepszej ścieżki");
            alert.setContentText("Błąd podczas szukania najlepszej ścieżki");
            alert.showAndWait();

        });

        task.setOnRunning((successesEvent) -> {

            dialog.showAndWait();

        });

        task.setOnSucceeded((succeededEvent) -> {
            dialog.hide();

            try {
                resultListView.getItems().add(task.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Alert createAlert(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        return alert;
    }

    private class SearchRouteTask extends Task<Pair<String, CoordinateLine>> {
        private final int speed;
        private final double costParameter;
        private final Coordinate startPoint;
        private final Coordinate endPoint;

        public SearchRouteTask(Coordinate startPoint, Coordinate endPoint, int speed, double costParameter) {
            this.startPoint = startPoint;
            this.endPoint = endPoint;
            this.speed = speed;
            this.costParameter = costParameter;
        }

        @Override
        protected Pair<String, CoordinateLine> call() throws Exception {
            CoordinateLine route = searchPathService.findRoute(startPoint, endPoint, speed, costParameter);

            String resultTitle = "Droga dla prędkości " + speed + " km/h";
            return new Pair<>(resultTitle, route);
        }
    }

    private void handleShowResultButtonClicked(MouseEvent mouseEvent) {
        var selectedItem = resultListView.getSelectionModel().getSelectedItem();

        if (selectedItem != null) {
            cleanMapView();
            Platform.runLater(() -> {
                mapView.addCoordinateLine(selectedItem.getValue()
                        .setVisible(true)
                        .setColor(Color.DODGERBLUE)
                        .setWidth(7)
                        .setClosed(true)
                        .setFillColor(Color.web("lawngreen", 0.5)));
            });
        }
    }
}
