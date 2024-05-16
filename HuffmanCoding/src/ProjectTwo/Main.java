package ProjectTwo;

import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Duration;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

/**
 * This JavaFX application facilitates file compression and decompression using Huffman Coding, a popular 
 * lossless data compression algorithm. Through an interactive graphical user interface, users can choose 
 * files to compress into Huffman encoded formats or decompress previously encoded files. The application 
 * provides visual feedback through animations and is equipped with error handling to ensure robust file 
 * operations. It aims to deliver a user-friendly experience while showcasing the efficiency of Huffman 
 * Coding for reducing file sizes.
 */
public class Main extends Application {

    File file; // To hold the reference to the selected file

    public void start(Stage stage) throws Exception {
        Image cursorImage = new Image("cursor.png");
        Cursor cursor = new ImageCursor(cursorImage); // Custom cursor for the application

        BorderPane bp = new BorderPane(); // Main layout pane

        Glow glow = new Glow(); // Glow effect for interactive elements
        glow.setLevel(0.5);

        ColorAdjust colorAdjust = new ColorAdjust(); // Adjust color settings for glow effect
        colorAdjust.setBrightness(0.2);
        colorAdjust.setContrast(0.0);
        colorAdjust.setSaturation(-0.1);
        colorAdjust.setHue(0.166);

        glow.setInput(colorAdjust); // Apply color adjustment to the glow effect

        ImageView logoTopView = new ImageView(new Image("bzuLogo.png"));
        logoTopView.setPreserveRatio(true);
        logoTopView.setFitHeight(289.5 / 4);
        logoTopView.setFitWidth(422.5 / 4);

        // Event handler for mouse entering and exiting the logo view to apply/remove the glow effect
        logoTopView.addEventHandler(MouseEvent.MOUSE_ENTERED, (MouseEvent e) -> {
            logoTopView.setEffect(glow);
        });
        logoTopView.addEventHandler(MouseEvent.MOUSE_EXITED, (MouseEvent e) -> {
            logoTopView.setEffect(null);
        });

        HBox topBox = new HBox(logoTopView);
        topBox.setAlignment(Pos.TOP_LEFT);
        bp.setTop(topBox);

        Image logoImage = new Image("rar-format.png");
        ImageView logoView = new ImageView(logoImage);
        logoView.setFitHeight(logoImage.getHeight() / 2);
        logoView.setFitWidth(logoImage.getWidth() / 2);

        // Mouse event handlers for another logo view
        logoView.addEventHandler(MouseEvent.MOUSE_ENTERED, (MouseEvent e) -> {
            logoView.setEffect(glow);
        });
        logoView.addEventHandler(MouseEvent.MOUSE_EXITED, (MouseEvent e) -> {
            logoView.setEffect(null);
        });

        addZoomEffect(logoView, 0.1, 1.0, 1500); // Zoom effect for the logoView

        Image minimumImage = new Image("huffmanTitle.png");
        ImageView minimumView = new ImageView(minimumImage);
        minimumView.setFitHeight(minimumImage.getHeight() / 5.5);
        minimumView.setFitWidth(minimumImage.getWidth() / 5.5);

        SequentialTransition sequentialTransition = new SequentialTransition();
        addZoomEffect(minimumView, 0.1, 1.0, 850, sequentialTransition); // Zoom effect as part of a sequence

        sequentialTransition.play(); // Start the sequential transition

        VBox vBox = new VBox(10, logoView, minimumView);
        vBox.setAlignment(Pos.CENTER);
        bp.setCenter(vBox); // Center the VBox in the border pane

        Button compressButton = new Button("Compress File");
        Button decompressButton = new Button("Decompress File");
        HBox optionsBox = new HBox(10, compressButton, decompressButton);
        optionsBox.setAlignment(Pos.CENTER);
        bp.setBottom(optionsBox); // Set the HBox with buttons at the bottom of the border pane

        Scene scene = new Scene(bp, 1200, 600);
        bp.setPadding(new Insets(15, 15, 15, 15)); // Padding around the border pane

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("File Chooser");

        // Event handler for compressing files
        compressButton.setOnAction(e -> {
            ExtensionFilter filterAll = new ExtensionFilter("Text files", "*");
            ExtensionFilter filterTXT = new ExtensionFilter("Text files", "*txt");
            fileChooser.getExtensionFilters().clear();
            fileChooser.getExtensionFilters().addAll(filterTXT, filterAll);

            file = fileChooser.showOpenDialog(stage);
            try {
                if (file.length() == 0) throw new IOException();
                CompressScene compressScene = new CompressScene(stage, scene, file);
                compressScene.setCursor(cursor);
                compressScene.getStylesheets().add("LightMode.css");
                stage.setScene(compressScene);
            } catch (Exception e2) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("File is empty, try another valid file");
                alert.showAndWait();
            }
        });

        // Event handler for decompressing files
        decompressButton.setOnAction(e -> {
            ExtensionFilter filterHUFF = new ExtensionFilter("Text files", "*huff");
            fileChooser.getExtensionFilters().clear();
            fileChooser.getExtensionFilters().add(filterHUFF);

            file = fileChooser.showOpenDialog(stage);
            try {
                if (file.length() == 0) throw new IOException();
                DecompressScene decompressScene = new DecompressScene(stage, scene, file);
                decompressScene.setCursor(cursor);
                decompressScene.getStylesheets().add("LightMode.css");
                stage.setScene(decompressScene);
            } catch (Exception e2) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("File is empty, try another valid file");
                alert.showAndWait();
            }
        });

        scene.setCursor(cursor);
        scene.getStylesheets().add("LightMode.css"); // Apply CSS for styling
        stage.setScene(scene);
        stage.setTitle("Huffman Compression");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Helper method to apply a zoom effect to an ImageView
    private void addZoomEffect(ImageView imageView, double fromScale, double toScale, int durationMillis) {
        ScaleTransition st = new ScaleTransition(Duration.millis(durationMillis), imageView);
        st.setFromX(fromScale);
        st.setFromY(fromScale);
        st.setToX(toScale);
        st.setToY(toScale);
        st.play();
    }

    // Overloaded method to add a zoom effect to an ImageView as part of a SequentialTransition
    private void addZoomEffect(ImageView imageView, double fromScale, double toScale, int durationMillis, SequentialTransition sequentialTransition) {
        ScaleTransition st = new ScaleTransition(Duration.millis(durationMillis), imageView);
        st.setFromX(fromScale);
        st.setFromY(fromScale);
        st.setToX(toScale);
        st.setToY(toScale);
        sequentialTransition.getChildren().add(st);
    }
}
