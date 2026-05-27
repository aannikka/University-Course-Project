package app.views.admin.users;

import app.entities.user.Role;
import app.entities.user.User;

import javax.swing.*;
import java.awt.*;

public class CreateUserView extends JDialog {

    private JTextField fullNameField; //поле вводу ПІБ
    private JTextField loginField; //поле вводу логіну
    private JPasswordField passwordField; //поле вводу пароля
    private JComboBox<Role> roleComboBox; //вибір ролі
    private JButton saveButton; //кнопка "Зберегти"
    private JButton cancelButton; //кнопка "Скасувати"

    //конструктор
    public CreateUserView(Frame owner) {
        super(owner, "Створення нового користувача", true);
        setSize(400, 300);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        //панель
        JPanel mainPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        mainPanel.add(new JLabel("ПІБ:"));
        fullNameField = new JTextField();
        mainPanel.add(fullNameField);

        mainPanel.add(new JLabel("Роль:"));

        roleComboBox = new JComboBox<>(Role.values());
        mainPanel.add(roleComboBox);

        mainPanel.add(new JLabel("Логін:"));
        loginField = new JTextField();
        mainPanel.add(loginField);

        mainPanel.add(new JLabel("Пароль:"));
        passwordField = new JPasswordField();
        mainPanel.add(passwordField);

        add(mainPanel, BorderLayout.CENTER);

        //кнопки
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveButton = new JButton("Зберегти");
        cancelButton = new JButton("Скасувати");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    //заповнення полів при редагування
    public void fillFields(User user) {
        setTitle("Редагування користувача: ");
        fullNameField.setText(user.getFullName());
        loginField.setText(user.getLogin());
        passwordField.setText(user.getPassword());
        roleComboBox.setSelectedItem(user.getRole());
        saveButton.setText("Оновити");
    }

    //геттери для контролеру
    public String getFullName() { return fullNameField.getText(); }
    public String getLogin() { return loginField.getText(); }
    public String getPassword() { return new String(passwordField.getPassword()); }
    public Role getSelectedRole() { return (Role) roleComboBox.getSelectedItem(); }
    public JButton getSaveButton() { return saveButton; }
    public JButton getCancelButton() { return cancelButton; }
}