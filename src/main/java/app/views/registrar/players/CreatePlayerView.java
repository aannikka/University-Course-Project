package app.views.registrar.players;

import app.entities.participant.Player;
import app.entities.tournament.Tournament;

import javax.swing.*;
import java.awt.*;

public class CreatePlayerView extends JDialog {
    private JTextField fullNameField; //поле введення ПІБ
    private JTextField phoneNumberField; //поле введення номера телефону
    private JTextField dateBirthField; //поле введення дати народження
    private JSpinner ratingField; //поле назви рейтингу
    private JComboBox<Tournament> tournamentComboBox; //обрання турніру
    private JButton saveButton; //кнопка "Зберегти"
    private JButton cancelButton; //кнопка "Скасувати"

    //конструктор
    public CreatePlayerView(JFrame owner) {
        super(owner, "Створення нового гравця", true);
        setSize(400, 300);
        setResizable(false);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        //панель
        JPanel mainPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        mainPanel.add(new JLabel("ПІБ:"));
        fullNameField = new JTextField();
        mainPanel.add(fullNameField);

        mainPanel.add(new JLabel("Номер телефону:"));
        phoneNumberField = new JTextField();
        mainPanel.add(phoneNumberField);

        mainPanel.add(new JLabel("Дата народження:"));
        dateBirthField = new JTextField();
        mainPanel.add(dateBirthField);

        mainPanel.add(new JLabel("Рейтинг: "));
        ratingField = new JSpinner();
        mainPanel.add(ratingField);

        mainPanel.add(new JLabel("Обраний турнір: "));
        tournamentComboBox = new JComboBox<>();
        mainPanel.add(tournamentComboBox);

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
    public void fillFields(Player p) {
        setTitle("Редагування гравця: ");
        fullNameField.setText(p.getFullName());
        phoneNumberField.setText(p.getPhoneNumber());
        dateBirthField.setText(p.getDateBirth().toString());
        ratingField.setValue(p.getRating());
        tournamentComboBox.setEnabled(false);
        saveButton.setText("Оновити");
    }

    //геттери для контролеру
    public String getFullName() {return fullNameField.getText();}
    public String getPhoneNumber() {return phoneNumberField.getText();}
    public String getDateBirth() {return dateBirthField.getText();}
    public String getRating() {return ratingField.getValue().toString();}
    public JComboBox<Tournament> getTournamentComboBox() {return tournamentComboBox;}
    public JButton getSaveButton() {return saveButton;}
    public JButton getCancelButton() {return cancelButton;}
}
