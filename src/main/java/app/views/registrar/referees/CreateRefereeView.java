package app.views.registrar.referees;

import app.entities.participant.Qualification;
import app.entities.participant.Referee;
import app.entities.tournament.Tournament;

import javax.swing.*;
import java.awt.*;

public class CreateRefereeView extends JDialog {
    private JTextField fullNameField; //поле введення ПІБ
    private JTextField phoneNumberField; //поле введення номера телефону
    private JTextField dateBirthField; //поле введення дати народження
    private JComboBox<Qualification> qualificationComboBox; //обрання кваліфікації
    private JComboBox<Tournament> tournamentComboBox; //обрання турніру
    private JButton saveButton; //кнопка "Зберегти"
    private JButton cancelButton; //кнопка "Скасувати"

    //конструктор
    public CreateRefereeView(JFrame owner) {
        super(owner, "Створення нового судді", true);
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

        mainPanel.add(new JLabel("Кваліфікація: "));
        qualificationComboBox = new JComboBox<>(Qualification.values());
        mainPanel.add(qualificationComboBox);

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

    //заповнення полів при редагуванні
    public void fillFields(Referee r) {
        setTitle("Редагування судді: ");
        fullNameField.setText(r.getFullName());
        phoneNumberField.setText(r.getPhoneNumber());
        dateBirthField.setText(r.getDateBirth().toString());
        qualificationComboBox.setSelectedItem(r.getQualification().getDisplayName());
        tournamentComboBox.setEnabled(false);
        saveButton.setText("Оновити");
    }

    //геттери для контролеру
    public String getFullName() {return fullNameField.getText();}
    public String getPhoneNumber() {return phoneNumberField.getText();}
    public String getDateBirth() {return dateBirthField.getText();}
    public JComboBox<Qualification> getQualificationComboBox() {return qualificationComboBox;}
    public JComboBox<Tournament> getTournamentComboBox() {return tournamentComboBox;}
    public JButton getSaveButton() {return saveButton;}
    public JButton getCancelButton() {return cancelButton;}
}
