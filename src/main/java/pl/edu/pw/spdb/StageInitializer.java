package pl.edu.pw.spdb;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;

/***
 * Klasa wiążąca kontekst aplikacji z biblioteką JavaFX
 */
@Component
public class StageInitializer implements ApplicationListener<ClientUI.StageReadyEvent> {

    private final ApplicationContext applicationContext;

    private final String applicationTitle;

    @Value("classpath:/fxml/mainWindow.fxml")
    private Resource mainWindowResource;

    public StageInitializer(@Value("${spring.application.ui.title}") String applicationTitle,
                            ApplicationContext applicationContext) {
        this.applicationTitle = applicationTitle;
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ClientUI.StageReadyEvent event) {
        Stage stage = event.getStage();

        FXMLLoader loader;
        Parent root;
        try {
            loader = new FXMLLoader(mainWindowResource.getURL());
            loader.setControllerFactory(applicationContext::getBean);
            root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Scene scene = new Scene(root);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.setTitle(applicationTitle);
        stage.show();
    }
}
