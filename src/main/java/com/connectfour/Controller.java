package com.connectfour;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;
import java.util.stream.IntStream;

public class Controller implements Initializable {

    private static final int COLUMNS = 7;
    private static final int ROWS = 6;
    private static final int CIRCLE_DIAMETER = 80;
    private static final String diskColor1 = "#24303E";
    private static final String diskColor2 = "#4CAA8A";


    private static String PLAYER_ONE = "Player One";
    private static String PLAYER_TWO = "Player Two";

    private boolean isPlayerOneTurn = true;

    private Disc[][] insertedDiskArray = new Disc[ROWS][COLUMNS];

    @FXML
    public GridPane rootGridPane;
    @FXML
    public Pane insertedDiskPane;
    @FXML
    public Label playerNameLabel;

    @FXML
    public TextField playerOneName;

    @FXML
    public TextField playerTwoName;

    @FXML
    public Button setNamesButton;

    private boolean isAllowedToInsert = true;

    public void createPlayground() {

        Shape rectangleWithHoles = createGameStructuralGrid();

        rootGridPane.add(rectangleWithHoles, 0, 1);

        ArrayList<Rectangle> rectangleArrayList = createClickableColumns();
        for (Rectangle rectangle : rectangleArrayList) {
            rootGridPane.add(rectangle, 0, 1);
        }

    }

    private Shape createGameStructuralGrid() {
        Shape rectangleWithHoles = new Rectangle((COLUMNS + 1) * CIRCLE_DIAMETER, (ROWS + 1) * CIRCLE_DIAMETER);

        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLUMNS; col++) {
                Circle circle = new Circle();
                circle.setRadius(CIRCLE_DIAMETER / 2);
                circle.setCenterX(CIRCLE_DIAMETER / 2);
                circle.setCenterY(CIRCLE_DIAMETER / 2);
                circle.setSmooth(true);

                circle.setTranslateX(col * (CIRCLE_DIAMETER + 5) + CIRCLE_DIAMETER / 4);
                circle.setTranslateY(row * (CIRCLE_DIAMETER + 5) + CIRCLE_DIAMETER / 4);

                rectangleWithHoles = Shape.subtract(rectangleWithHoles, circle);
            }

        }

        rectangleWithHoles.setFill(Color.WHITE);

        return rectangleWithHoles;
    }


    private ArrayList<Rectangle> createClickableColumns() {

        ArrayList<Rectangle> rectangleArrayList = new ArrayList<>();

        for (int col = 0; col < COLUMNS; col++) {
            Rectangle rectangle = new Rectangle(CIRCLE_DIAMETER, (ROWS + 1) * CIRCLE_DIAMETER);
            rectangle.setFill(Color.TRANSPARENT);
            rectangle.setTranslateX(col * (CIRCLE_DIAMETER + 5) + CIRCLE_DIAMETER / 4);
            rectangle.setOnMouseEntered(mouseEvent -> rectangle.setFill(Color.valueOf("#eeeeee26")));
            rectangle.setOnMouseExited(mouseEvent -> rectangle.setFill(Color.TRANSPARENT));
            final int column = col;
            rectangle.setOnMouseClicked(mouseEvent -> {
                if (isAllowedToInsert) {
                    isAllowedToInsert = false;
                    insertDisk(new Disc(isPlayerOneTurn), column);
                }
            });
            rectangleArrayList.add(rectangle);
        }
        return rectangleArrayList;
    }

    private void insertDisk(Disc disc, int col) {

        int row = ROWS - 1;
        while (row >= 0) {
            if (getDiskIfPresent(row, col) == null)
                break;

            row--;
        }

        if (row < 0)
            return;

        int currentRow = row;
        insertedDiskArray[row][col] = disc;
        insertedDiskPane.getChildren().add(disc);
        disc.setTranslateX(col * (CIRCLE_DIAMETER + 5) + CIRCLE_DIAMETER / 4);
        TranslateTransition translateTransition = new TranslateTransition(Duration.seconds(0.5), disc);

        translateTransition.setToY(row * (CIRCLE_DIAMETER + 5) + CIRCLE_DIAMETER / 4);
        translateTransition.setOnFinished(actionEvent -> {
            isAllowedToInsert = true;
            if (gameEnded(currentRow, col)) {
                gameOver();
                return;
            }

            isPlayerOneTurn = !isPlayerOneTurn;
            playerNameLabel.setText(isPlayerOneTurn ? PLAYER_ONE : PLAYER_TWO);

        });
        translateTransition.play();

    }

    private void gameOver() {
        String winner = isPlayerOneTurn ? PLAYER_ONE : PLAYER_TWO;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Connect Four");
        alert.setHeaderText("The Winner is " + winner);
        alert.setContentText("Want to play again?");

        ButtonType yesBtn = new ButtonType("Yes");
        ButtonType noBtn = new ButtonType("No, Exit");

        alert.getButtonTypes().setAll(yesBtn, noBtn);

        Platform.runLater(() -> {
            Optional<ButtonType> btnClicked = alert.showAndWait();

            if (btnClicked.isPresent() && btnClicked.get() == yesBtn) {
                resetGame();
            } else {
                Platform.exit();
                System.exit(0);

            }
        });
    }

    public void resetGame() {
        insertedDiskPane.getChildren().clear();
        for (int row = 0; row < insertedDiskArray.length; row++)
            Arrays.fill(insertedDiskArray[row], null);

        isPlayerOneTurn = true;
        playerNameLabel.setText(PLAYER_ONE);

        createPlayground();

    }

    private boolean gameEnded(int row, int col) {
        //Vertical
        List<Point2D> verticalPoints = IntStream.rangeClosed(row - 3, row + 3).mapToObj(r -> new Point2D(r, col)).toList();
        //Horizontal
        List<Point2D> horizontalPoints = IntStream.rangeClosed(col - 3, col + 3).mapToObj(c -> new Point2D(row, c)).toList();
        //Diagonal
        Point2D startPoint1 = new Point2D(row - 3, col + 3);
        List<Point2D> diagonal1Points = IntStream.rangeClosed(0, 6).mapToObj(i -> startPoint1.add(i, -i)).toList();

        Point2D startPoint2 = new Point2D(row - 3, col - 3);
        List<Point2D> diagonal2Points = IntStream.rangeClosed(0, 6).mapToObj(i -> startPoint2.add(i, i)).toList();

        boolean isEnded = checkCombinations(verticalPoints) || checkCombinations(horizontalPoints) ||
                checkCombinations(diagonal1Points) || checkCombinations(diagonal2Points);

        return isEnded;
    }

    private boolean checkCombinations(List<Point2D> points) {
        int chain = 0;
        for (Point2D point : points) {
            int rowIndexForArray = (int) point.getX();
            int columnIndexForArray = (int) point.getY();

            Disc disc = getDiskIfPresent(rowIndexForArray, columnIndexForArray);
            if (disc != null && disc.isPlayerOneTurn == isPlayerOneTurn) {
                chain++;
                if (chain == 4)
                    return true;
            } else {
                chain = 0;
            }
        }
        return false;
    }

    private Disc getDiskIfPresent(int row, int column) {
        if (row >= ROWS || row < 0 || column >= COLUMNS || column < 0)
            return null;

        return insertedDiskArray[row][column];
    }

    private static class Disc extends Circle {

        private final boolean isPlayerOneTurn;

        public Disc(boolean isPlayerOneTurn) {
            this.isPlayerOneTurn = isPlayerOneTurn;
            setRadius(CIRCLE_DIAMETER / 2);
            setFill(isPlayerOneTurn ? Color.valueOf(diskColor1) : Color.valueOf(diskColor2));
            setCenterX(CIRCLE_DIAMETER / 2);
            setCenterY(CIRCLE_DIAMETER / 2);

        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setNamesButton.setOnAction(event -> {

            String input1 = playerOneName.getText();
            String input2 = playerTwoName.getText();

            PLAYER_ONE = input1 + "`s";
            PLAYER_TWO = input2 + "`s";

            if (input1.isEmpty())
                PLAYER_ONE = "Player One`s";

            if (input2.isEmpty())
                PLAYER_TWO = "Player Two`s";

            //  isPlayerOneTurn = !isPlayerOneTurn;
            playerNameLabel.setText(isPlayerOneTurn ? PLAYER_ONE : PLAYER_TWO);

        });

    }
}