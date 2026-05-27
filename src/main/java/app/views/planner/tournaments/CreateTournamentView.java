package app.views.planner.tournaments;

import app.entities.tournament.Tournament;
import javax.swing.*;
import java.awt.*;

public class CreateTournamentView extends JDialog {
    //поля введення назви, дати початку, дати закінчення, міста, призового фонду
    private JTextField nameField, startDateField, endDateField, cityField, prizeFundField;
    private JComboBox<String> locationComboBox; //обрання місця проведення
    //введення мінімального рейтингу та максимальної кількості учасників
    private JSpinner minRatingSpinner, maxParticipantsSpinner;
    //введення опису
    private JTextArea descriptionArea;
    private JButton saveButton, cancelButton; //кнопки "Зберегти" та "Скасувати"

    //конструктор
    public CreateTournamentView(Frame owner) {
        super(owner, "Створення турніру", true);
        setSize(500, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        //панель
        JPanel mainPanel = new JPanel(new GridLayout(9, 2, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        mainPanel.add(new JLabel("Назва турніру:"));
        nameField = new JTextField();
        mainPanel.add(nameField);

        mainPanel.add(new JLabel("Дата початку (YYYY-MM-DD):"));
        startDateField = new JTextField();
        mainPanel.add(startDateField);

        mainPanel.add(new JLabel("Дата закінчення (YYYY-MM-DD):"));
        endDateField = new JTextField();
        mainPanel.add(endDateField);

        mainPanel.add(new JLabel("Місто:"));
        cityField = new JTextField();
        mainPanel.add(cityField);

        mainPanel.add(new JLabel("Місце проведення:"));
        locationComboBox = new JComboBox<>();
        locationComboBox.setEnabled(false);
        mainPanel.add(locationComboBox);

        mainPanel.add(new JLabel("Мін. рейтинг:"));
        minRatingSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 5000, 50));
        mainPanel.add(minRatingSpinner);

        mainPanel.add(new JLabel("Макс. учасників:"));
        maxParticipantsSpinner = new JSpinner(new SpinnerNumberModel(2, 2, 128, 2));
        mainPanel.add(maxParticipantsSpinner);

        mainPanel.add(new JLabel("Призовий фонд:"));
        prizeFundField = new JTextField("0.0");
        mainPanel.add(prizeFundField);

        mainPanel.add(new JLabel("Опис:"));
        descriptionArea = new JTextArea(3, 20);
        mainPanel.add(new JScrollPane(descriptionArea));

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
    public void fillFields(Tournament t) {
        setTitle("Редагування турніру");
        nameField.setText(t.getName());
        startDateField.setText(t.getDateStart().toString());
        endDateField.setText(t.getDateFinish().toString());
        cityField.setText(t.getCity());
        minRatingSpinner.setValue(t.getMinRating());
        maxParticipantsSpinner.setValue(t.getMaxQuantityParticipant());
        prizeFundField.setText(String.valueOf(t.getPrizeFund()));
        descriptionArea.setText(t.getDescription());
        saveButton.setText("Оновити");
    }

    //геттери для контролеру
    public String getTournamentName() { return nameField.getText(); }
    public String getStartDate() { return startDateField.getText(); }
    public String getEndDate() { return endDateField.getText(); }
    public String getCity() { return cityField.getText(); }
    public JComboBox<String> getLocationComboBox() { return locationComboBox; }
    public String getSelectedLocation() { return (String) locationComboBox.getSelectedItem(); }
    public int getMinRating() { return (int) minRatingSpinner.getValue(); }
    public int getMaxParticipants() { return (int) maxParticipantsSpinner.getValue(); }
    public String getPrizeFund() { return prizeFundField.getText(); }
    public String getDescription() { return descriptionArea.getText(); }
    public JButton getSaveButton() { return saveButton; }
    public JButton getCancelButton() { return cancelButton; }
    public JTextField getCityField() { return cityField; }
}