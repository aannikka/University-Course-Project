package app.views.auth;

import javax.swing.*;
import java.awt.*;

public class LoginView extends JFrame {

    private JTextField loginField; //поле вводу логіну
    private JPasswordField passwordField; //поле вводу паролю
    private JButton loginButton; //кнопка "Увійти"
    private JButton exitButton; //кнопка "Вийти"

    //конструктор
    public LoginView() {
        setTitle("Вхід у систему");
        setSize(350, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        //панель полей
        JPanel fieldsPanel = new JPanel(new GridLayout(2, 2, 10, 10));

        fieldsPanel.add(new JLabel("Логін:"));
        loginField = new JTextField();
        fieldsPanel.add(loginField);

        fieldsPanel.add(new JLabel("Пароль:"));
        passwordField = new JPasswordField();
        fieldsPanel.add(passwordField);

        mainPanel.add(fieldsPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        //панель кнопок
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));

        loginButton = new JButton("Увійти");
        loginButton.setBackground(new Color(60, 130, 200));
        loginButton.setForeground(Color.BLACK);
        loginButton.setFocusPainted(false);

        exitButton = new JButton("Вихід");

        buttonsPanel.add(loginButton);
        buttonsPanel.add(exitButton);

        mainPanel.add(buttonsPanel);
        add(mainPanel);
    }

    //геттери для контролеру
    public String getLogin() {
        return loginField.getText();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    public JButton getLoginButton() { return loginButton; }
    public JButton getExitButton() { return exitButton; }

    //виводить діалогове вікно з повідомленням про помилку
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Помилка входу", JOptionPane.ERROR_MESSAGE);
    }

    //виводить діалогове вікно з повідомленням про успішну авторизацію
    public void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Успіх", JOptionPane.INFORMATION_MESSAGE);
    }
}