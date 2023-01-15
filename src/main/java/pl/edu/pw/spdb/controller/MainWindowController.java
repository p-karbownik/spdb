package pl.edu.pw.spdb.controller;

import com.sothawo.mapjfx.*;
import com.sothawo.mapjfx.event.MapLabelEvent;
import com.sothawo.mapjfx.event.MapViewEvent;
import com.sothawo.mapjfx.event.MarkerEvent;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.Event;
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
import pl.edu.pw.spdb.model.Route;
import pl.edu.pw.spdb.service.SearchPathService;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/***
 * Klasa reprezentująca kontroler okna aplikacji
 */
@Component
public class MainWindowController implements Initializable {
    private final Coordinate pkinCoordinate = new Coordinate(52.231667, 21.006389);

    private final ToggleGroup radioButtonsGroup = new ToggleGroup();
    private final SearchPathService searchPathService;
    @FXML
    private ListView<Pair<String, Route>> resultListView;
    private MapViewState mapViewState = MapViewState.DEFAUlT;
    @FXML
    private Button chooseStartPointButton, chooseEndPointButton, searchButton, showResultButton, cleanMapViewButton;
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

    @FXML
    private MapView mapView;

    public MainWindowController(SearchPathService searchPathService) {
        this.searchPathService = searchPathService;
    }

    /***
     * Metoda dostarczana przez bibliotekę JavaFX
     * Służy do inicjalizacji widoku aplikacji
     * @param location
     * The location used to resolve relative paths for the root object, or
     * {@code null} if the location is not known.
     *
     * @param resources
     * The resources used to localize the root object, or {@code null} if
     * the root object was not localized.
     */
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
            protected void updateItem(Pair<String, Route> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item.getKey());
                }
            }
        });
    }

    /***
     * Metoda inicjalizująca przyciski
     */
    private void initializeButtons() {
        chooseStartPointButton.setOnMouseClicked(this::handleChooseStartPointButtonClicked);
        chooseEndPointButton.setOnMouseClicked(this::handleChooseEndPointButtonClicked);
        searchButton.setOnMouseClicked(this::handleSearchButtonClicked);
        showResultButton.setOnMouseClicked(this::handleShowResultButtonClicked);
        cleanMapViewButton.setOnMouseClicked(this::handleCleanMapViewButton);
    }

    /***
     * Metoda inicjalizująca przyciski do przełączania parametru kosztu
     */
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

    /***
     * Metoda inicjalizująca suwak wyboru wartości kosztu
     */
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

    /***
     * Metoda inicjalizująca kontorlkę mapView oraz markery reprezentujące punkt początkowy i końcowy
     */
    private void initializeMapView() {
        startPointMarker = Marker.createProvided(Marker.Provided.GREEN).setVisible(false);
        endPointMarker = Marker.createProvided(Marker.Provided.RED).setVisible(false);

        mapView.setAnimationDuration(500);

        mapView.addEventHandler(MapViewEvent.MAP_EXTENT, event -> {
            mapView.setExtent(event.getExtent());
            event.consume();
        });
        mapView.addEventHandler(MapViewEvent.MAP_RIGHTCLICKED, Event::consume);
        mapView.addEventHandler(MapViewEvent.MAP_BOUNDING_EXTENT, Event::consume);
        mapView.addEventHandler(MarkerEvent.MARKER_CLICKED, Event::consume);
        mapView.addEventHandler(MarkerEvent.MARKER_MOUSEDOWN, Event::consume);
        mapView.addEventHandler(MarkerEvent.MARKER_MOUSEUP, Event::consume);
        mapView.addEventHandler(MarkerEvent.MARKER_DOUBLECLICKED, Event::consume);
        mapView.addEventHandler(MarkerEvent.MARKER_RIGHTCLICKED, Event::consume);
        mapView.addEventHandler(MarkerEvent.MARKER_ENTERED, Event::consume);
        mapView.addEventHandler(MarkerEvent.MARKER_EXITED, Event::consume);
        mapView.addEventHandler(MapLabelEvent.MAPLABEL_MOUSEDOWN, Event::consume);
        mapView.addEventHandler(MapLabelEvent.MAPLABEL_MOUSEUP, Event::consume);
        mapView.addEventHandler(MapLabelEvent.MAPLABEL_CLICKED, Event::consume);
        mapView.addEventHandler(MapViewEvent.MAP_CLICKED, this::handleClickOnMapView);
        mapView.addEventHandler(MapLabelEvent.MAPLABEL_RIGHTCLICKED, Event::consume);
        mapView.addEventHandler(MapLabelEvent.MAPLABEL_DOUBLECLICKED, Event::consume);
        mapView.addEventHandler(MapLabelEvent.MAPLABEL_ENTERED, Event::consume);
        mapView.addEventHandler(MapLabelEvent.MAPLABEL_EXITED, Event::consume);
        mapView.addEventHandler(MapViewEvent.MAP_POINTER_MOVED, Event::consume);

        mapView.setCenter(pkinCoordinate);

        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
        });

        mapView.setMapType(MapType.OSM);
        mapView.initialize(Configuration.builder().build());
    }

    /***
     * Metoda obsługi zdarzenia kliknięcia przycisku Wybierz z mapy dla punktu początkowego
     * @param e - obiekt reprezentujący zdarzenie
     */
    private void handleChooseStartPointButtonClicked(MouseEvent e) {
        if (mapViewState != MapViewState.CHOOSING_START_POINT) {
            if (mapViewState == MapViewState.SHOWING_ROUTE) {
                cleanMapView();
            }
            mapViewState = MapViewState.CHOOSING_START_POINT;
        } else {
            mapViewState = MapViewState.DEFAUlT;
        }
        changeApplicationStateLabel();
    }

    /***
     * Metoda obsługi zdarzenia kliknięcia przycisku Wybierz z mapy dla punktu końcowego
     * @param e - obiekt reprezentujący zdarzenie
     */
    private void handleChooseEndPointButtonClicked(MouseEvent e) {
        if (mapViewState != MapViewState.CHOOSING_END_POINT) {
            if (mapViewState == MapViewState.SHOWING_ROUTE) {
                cleanMapView();
            }
            mapViewState = MapViewState.CHOOSING_END_POINT;
        } else {
            mapViewState = MapViewState.DEFAUlT;
        }
        changeApplicationStateLabel();
    }

    /***
     * Metoda zmieniająca etykietę z informacją o stanie aplikacji
     */
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

    /***
     * Metoda inicjalizująca pola tekstowe
     * Ustawione jest tu wiązanie między właściwością reprezentującą zawartość pól, a funkcją walidującą
     */
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

    /***
     * Metoda walidująca poprawność danych w polu tekstowym z prędkością
     * @param textField - obiekt reprezentujący pole tekstowe
     */
    private void validateSpeedTextField(TextField textField) {
        if (!textField.getText().matches("^[0-9]+$")) {
            textField.setText("");
        }
    }

    /***
     * Metoda mapująca koordynat na string
     * @param coordinate - współrzedne
     * @return ciąg znaków reprezentujący współrzędne
     */
    private String getCoordinatesString(Coordinate coordinate) {
        return String.format("%.6f", coordinate.getLongitude()) + ", " + String.format("%.6f", coordinate.getLatitude());
    }

    /***
     * Metoda usuwająca zdarzenie kliknięcia na kontrolkę MapView.
     * Służy do zaznaczania pinezką punktu początkowego i końcowego
     * @param event - zdarzenie naciśnięcia myszką na kontrolkę mapView
     */
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


    /***
     * Metoda usuwająca linie z kontrolki MapView
     */
    private void cleanCoordinateLinesFromMapView() {
        for (var element : resultListView.getItems()) {
            mapView.removeCoordinateLine(element.getValue().getCoordinateLine());
        }
    }

    /***
     * Metoda usuwająca markery z kontrolki MapView oraz czyszcząca pola tekstowe ze współrzędnymi
     */
    private void cleanMapView() {
        mapView.removeMarker(startPointMarker);
        mapView.removeMarker(endPointMarker);
        startPointCoordinatesTextField.clear();
        endPointCoordinatesTextField.clear();
        cleanCoordinateLinesFromMapView();
    }

    /***
     * Metoda obsługująca zdarzenie naciśnięcia przycisku Szukaj
     * @param event - zdarzenie myszki
     */
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

    /***
     * Metoda przygotowująca zadany task wyszukiwania ścieżki.
     * Definiuje zachowanie aplikacji podczas wykonywania się kodu, w przypadku niepowodzenia oraz sukcesu.
     * @param task - zadanie wyszukiwania ścieżki
     */
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

    /***
     * Metoda obsługująca zdarzenie naciśnięcia przycisku Wyczyść mapę
     * @param event - zdarzenie myszki
     */
    private void handleCleanMapViewButton(MouseEvent event) {
        Platform.runLater(() -> {
            cleanMapView();
            mapViewState = MapViewState.DEFAUlT;
            changeApplicationStateLabel();
        });
    }
    /***
     * Metoda pomocnicza tworząca okno pop-up
     * @param title - treść tytułu okna
     * @param headerText - treść nagłówka okna
     * @param contentText - treść ciała alert
     */
    private Alert createAlert(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        return alert;
    }

    /***
     * Metoda zwracająca ciąg znaków reprezentujący zadany czas
     * @param time - czas
     */
    private String getEstimatedTimeString(double time) {
        int hours = (int) time;
        double minutesD = (time - hours) * 60;
        int minutes = (int) minutesD;

        return hours + "godzin " + minutes + "minut";
    }

    /***
     * Metoda obsługująca wyświetlanie szczegółów o trasie
     * @param route - znalezione droga
     */
    private void showRouteDetails(Route route) {
        String text;

        if (route.getSegments().isEmpty()) {
            text = "Nie znaleziono drogi dla zadanych parametrów";
        } else {
            text = "Długość trasy: " + String.format("%.2f", route.getDistance()) +
                    " Szacunkowy czas podróży: " + getEstimatedTimeString(route.getEstimatedTime());
        }

        applicationStateLabel.setText(text);
    }

    /***
     * Metoda obsługująca zdarzenia naciśnięcia przycisku Pokaż wynik
     * @param mouseEvent - zdarzenie myszki
     */
    private void handleShowResultButtonClicked(MouseEvent mouseEvent) {
        var selectedItem = resultListView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            mapViewState = MapViewState.SHOWING_ROUTE;
            CoordinateLine theLine = selectedItem.getValue().getCoordinateLine().setVisible(true)
                    .setColor(Color.DARKGREEN)
                    .setWidth(7)
                    .setClosed(false);

            Platform.runLater(() -> {
                showRouteDetails(selectedItem.getValue());
                cleanCoordinateLinesFromMapView();
                mapView.addCoordinateLine(theLine);
            });
        }
    }

    /***
     * Metoda czyszcząca zasoby kontrolki MapView przy zamknięciu aplikacji.
     */
    public void clearResources() {
        cleanMapView();
        mapView.close();
    }

    /***
     * Klasa implementująca wywołanie wyszukiwania najlepszej drogi.
     * Służy do uruchomienia zadania w osobnym wątku.
     */
    private class SearchRouteTask extends Task<Pair<String, Route>> {
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
        protected Pair<String, Route> call() {
            Route route = searchPathService.findRoute(startPoint, endPoint, speed, costParameter);

            String resultTitle = "Droga dla prędkości " + speed + " km/h";
            return new Pair<>(resultTitle, route);
        }
    }
}
