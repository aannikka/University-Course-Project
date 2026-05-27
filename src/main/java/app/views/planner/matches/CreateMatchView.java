package app.views.planner.matches;

import app.entities.location.Court;
import app.entities.match.Match;
import app.entities.participant.Player;
import app.entities.participant.Referee;

import javax.swing.*;
import java.awt.*;

public class CreateMatchView extends JDialog {
    private JComboBox<Court> courtComboBox; //обрання корту
    private JTextField dateField, timeField; //поля вводу дати та часу
    private JComboBox<Player> firstPlayerComboBox, secondPlayerComboBox; //обрання гравців
    private JComboBox<Referee> refereeComboBox; //обрання судді
    private JButton saveButton, cancelButton; //кнопки "Зберегти" та "Скасувати"

    //конструктор
    public CreateMatchView(Frame owner) {
        super(owner, "Створення матчу", true);
        setSize(500, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        //панель
        JPanel mainPanel = new JPanel(new GridLayout(7, 2, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        mainPanel.add(new JLabel("Дата (YYYY-MM-DD):"));
        dateField = new JTextField();
        mainPanel.add(dateField);

        mainPanel.add(new JLabel("Час:"));
        timeField = new JTextField();
        mainPanel.add(timeField);

        mainPanel.add(new JLabel("Корт:"));
        courtComboBox = new JComboBox<>();
        courtComboBox.setEnabled(false);
        mainPanel.add(courtComboBox);

        mainPanel.add(new JLabel("Гравець №1:"));
        firstPlayerComboBox = new JComboBox<>();
        firstPlayerComboBox.setEnabled(false);
        mainPanel.add(firstPlayerComboBox);

        mainPanel.add(new JLabel("Гравець №2:"));
        secondPlayerComboBox = new JComboBox<>();
        secondPlayerComboBox.setEnabled(false);
        mainPanel.add(secondPlayerComboBox);

        mainPanel.add(new JLabel("Суддя"));
        refereeComboBox = new JComboBox<>();
        refereeComboBox.setEnabled(false);
        mainPanel.add(refereeComboBox);

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
    public void fillFields(Match m) {
        setTitle("Редагування матчу");
        dateField.setText(m.getDate().toString());
        timeField.setText(m.getStartTime().toString());
        saveButton.setText("Оновити");
    }

    //геттери для контролеру
    public JTextField getDateField() { return dateField; }
    public JTextField getTimeField() { return timeField; }

    public JComboBox<Court> getCourtComboBox() { return courtComboBox; }
    public JComboBox<Player> getFirstPlayerComboBox() { return firstPlayerComboBox; }
    public JComboBox<Player> getSecondPlayerComboBox() { return secondPlayerComboBox; }
    public JComboBox<Referee> getRefereeComboBox() { return refereeComboBox; }

    public Court getSelectedCourt() { return (Court) courtComboBox.getSelectedItem(); }
    public Player getFirstPlayer() { return (Player) firstPlayerComboBox.getSelectedItem(); }
    public Player getSecondPlayer() { return (Player) secondPlayerComboBox.getSelectedItem(); }
    public Referee getReferee() { return (Referee) refereeComboBox.getSelectedItem(); }

    public JButton getSaveButton() { return saveButton; }
    public JButton getCancelButton() { return cancelButton; }
}
