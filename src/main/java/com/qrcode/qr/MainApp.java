package com.qrcode.qr;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class MainApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    @Override
    public void start(Stage primaryStage) {
        logger.info("Приложение запущено");

        // Поле для ввода текста или ссылки
        TextField textField = new TextField();
        textField.setPromptText("Введите текст или ссылку для QR-кода");

        // Кнопка для генерации QR-кода
        Button generateQRButton = new Button("Сгенерировать QR-код");

        // Кнопка для выбора файла и чтения QR-кода
        Button readQRButton = new Button("Считать QR-код из файла");

        // Область для отображения результата считывания
        TextArea resultTextArea = new TextArea();
        resultTextArea.setEditable(false);
        resultTextArea.setPromptText("Результат считывания QR-кода появится здесь...");

        // Область для отображения QR-кода
        ImageView qrImageView = new ImageView();
        qrImageView.setFitWidth(200);
        qrImageView.setFitHeight(200);
        qrImageView.setPreserveRatio(true);

        // Обработчик для генерации QR-кода
        generateQRButton.setOnAction(event -> {
            String text = textField.getText().trim(); // Получаем текст из поля ввода
            if (!text.isEmpty()) {
                try {
                    logger.info("Пользователь ввёл текст для генерации QR-кода: {}", text);

// Используем FileChooser для выбора пути сохранения
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Сохранить QR-код");
                    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));

// Указываем рекомендуемое имя файла
                    fileChooser.setInitialFileName("qrcode.png");

// Показываем диалог выбора файла
                    File saveFile = fileChooser.showSaveDialog(primaryStage);

                    if (saveFile != null) {
                        String filePath = saveFile.getAbsolutePath();
                        QRCodeGenerator.generateQRCode(text, filePath); // Генерация QR-кода
                        logger.info("QR-код успешно сохранён в файл: {}", filePath);

// Отображение QR-кода в приложении
                        File qrFile = new File(filePath);
                        if (qrFile.exists()) {
                            Image qrImage = new Image(qrFile.toURI().toString());
                            qrImageView.setImage(qrImage);
                            logger.info("QR-код успешно отображён в приложении.");
                        }

                        showAlert(Alert.AlertType.INFORMATION, "Успех", "QR-код сохранён как " + filePath);
                    } else {
                        logger.info("Сохранение QR-кода было отменено пользователем");
                    }
                } catch (Exception e) {
                    logger.error("Ошибка при генерации QR-кода", e);
                    showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось сгенерировать QR-код.");
                }
            } else {
                logger.warn("Попытка генерации QR-кода с пустым текстовым полем");
                showAlert(Alert.AlertType.WARNING, "Предупреждение", "Поле ввода пустое!");
            }
        });

        // Обработчик для считывания QR-кода из файла
        readQRButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
            File selectedFile = fileChooser.showOpenDialog(primaryStage);

            if (selectedFile != null) {
                try {
                    logger.info("Пользователь выбрал файл для чтения QR-кода: {}", selectedFile.getAbsolutePath());
                    String decodedText = readQRCodeFromAPI(selectedFile);
                    resultTextArea.setText(decodedText);
                    logger.info("QR-код успешно считан: {}", decodedText);
                } catch (Exception e) {
                    logger.error("Ошибка при считывании QR-кода из файла: {}", selectedFile.getAbsolutePath(), e);
                    showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось считать QR-код.");
                }
            } else {
                logger.warn("Пользователь не выбрал файл для чтения QR-кода");
                showAlert(Alert.AlertType.WARNING, "Предупреждение", "Файл не выбран.");
            }
        });

        // Контейнер для элементов
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(textField, generateQRButton, qrImageView, readQRButton, resultTextArea);

        // Создаем сцену
        Scene scene = new Scene(root, 360, 640);
        primaryStage.setTitle("QR Code App");
        primaryStage.setScene(scene);
        primaryStage.show();

        logger.info("Интерфейс приложения успешно загружен");
    }

    private String readQRCodeFromAPI(File file) throws IOException {
        String boundary = "----Boundary";
        String urlString = "https://api.qrserver.com/v1/read-qr-code/";

        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        // Формируем тело запроса
        try (OutputStream outputStream = connection.getOutputStream()) {
            // Заголовок файла
            outputStream.write(("--" + boundary + "\r\n").getBytes());
            outputStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
            outputStream.write("Content-Type: image/png\r\n\r\n".getBytes());

            // Содержимое файла
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            // Завершающий boundary
            outputStream.write(("\r\n--" + boundary + "--\r\n").getBytes());
        }

        // Читаем ответ от сервера
        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            try (InputStream inputStream = connection.getInputStream();
                 Scanner scanner = new Scanner(inputStream)) {
                StringBuilder response = new StringBuilder();
                while (scanner.hasNextLine()) {
                    response.append(scanner.nextLine());
                }
                return parseQRResponse(response.toString());
            }
        } else {
            // Если сервер вернул ошибку
            try (InputStream errorStream = connection.getErrorStream();
                 Scanner scanner = new Scanner(errorStream)) {
                StringBuilder errorResponse = new StringBuilder();
                while (scanner.hasNextLine()) {
                    errorResponse.append(scanner.nextLine());
                }
                throw new IOException("Ошибка " + responseCode + ": " + errorResponse);
            }
        }
    }

    // Метод для парсинга ответа от API
    private String parseQRResponse(String response) {
        System.out.println(response);
        if (response.contains("\"data\":\"")) {
            int startIndex = response.indexOf("\"data\":\"") + 8;
            int endIndex = response.indexOf("\"", startIndex);
            return response.substring(startIndex, endIndex).replace("\\/", "/");
        }
        logger.warn("Не удалось распознать QR-код. Ответ: {}", response);
        return "Не удалось распознать QR-код.";
    }

    // Утилита для отображения уведомлений
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}