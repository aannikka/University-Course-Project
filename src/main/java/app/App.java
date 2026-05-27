package app;

import app.controllers.LoginController;

import app.views.auth.LoginView;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        //Запуск створення графічного інтерфейсу в потоці для забезпечення потокобезпечності Swing-компонентів.
        SwingUtilities.invokeLater(() -> {
            try {
                //встановлення системного стилю інтерфейсу
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            //ініціалізація стартового вікна авторизації та його контролера
            LoginView loginView = new LoginView();
            new LoginController(loginView);
            loginView.setVisible(true);
        });
    }
}

