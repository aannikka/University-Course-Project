package app.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordHasher {
    //хешує вхідний рядок (пароль) за допомогою алгоритму SHA-256.
    public static String hashPassword(String password) {
        try {
            //ініціалізація алгоритму хешування
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            //перетворення рядка в масив байтів (UTF-8) та його хешування
            byte[] encodedHash = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8));

            //конвертація байтового хешу в зрозумілий шістнадцятковий рядок (Hex)
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Помилка: алгоритм хешування не знайдено", e);
        }
    }
}
