package pl.edu.pw.spdb;


import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/***
 * Klasa z metodą main
 * Uruchamiana jest tutaj aplikacja
 */
@SpringBootApplication
public class SpdbApplication {

    public static void main(String[] args) {
        Application.launch(ClientUI.class, args);
    }
}
