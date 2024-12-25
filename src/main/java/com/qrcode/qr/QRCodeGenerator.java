package com.qrcode.qr;


import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class QRCodeGenerator {

    public static void generateQRCode(String text, String filePath) throws Exception {
        // Формируем URL запроса
        String apiUrl = "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + text;

        // Создаем соединение
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Проверяем ответ
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Сохраняем изображение в файл
            try (InputStream inputStream = connection.getInputStream()) {
                Files.copy(inputStream, Path.of(filePath), StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            throw new Exception("Ошибка: не удалось получить QR-код. Код ответа: " + responseCode);
        }
    }
}