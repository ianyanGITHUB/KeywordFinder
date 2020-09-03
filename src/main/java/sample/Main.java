package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import sample.util.ThreadUtil;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("fxml/sample.fxml"));
        primaryStage.setTitle("word关键字查询系统");
        primaryStage.setScene(new Scene(root, 600, 420));
        primaryStage.getScene().getStylesheets().add(getClass().getResource("css/MainStyle.css").toExternalForm());
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("images/logo.png")));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        ThreadUtil.init();
        launch(args);
        System.out.println(args);
    }
}
