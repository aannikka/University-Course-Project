package app.views.admin.locations;

import app.entities.location.Court;
import app.entities.location.Location;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CreateLocationView extends JDialog {
    private JTextField nameField; //поле вводу назви
    private JTextField cityField; //поле вводу міста
    private JTextField addressField; //поле вводу адреси
    private JCheckBox isAvailableBox; //обрати чи доступна локація

    private JPanel courtsContainer; //панель кортів
    private List<CourtUIBlock> courtBlocks = new ArrayList<>(); //список кортів
    private JButton addCourtBtn; //кнопка "Додати корт"

    private JButton saveButton; //кнопка "Зберегти"
    private JButton cancelButton; //кнопка "Скасувати"

    //конструктор
    public CreateLocationView(Frame owner) {
        super(owner, "Створення нового місця проведення", true);
        setSize(450, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        //панель
        JPanel infoPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Дані локації"));

        infoPanel.add(new JLabel("Назва:"));
        nameField = new JTextField();
        infoPanel.add(nameField);

        infoPanel.add(new JLabel("Місто:"));
        cityField = new JTextField();
        infoPanel.add(cityField);

        infoPanel.add(new JLabel("Адреса:"));
        addressField = new JTextField();
        infoPanel.add(addressField);

        infoPanel.add(new JLabel("Локація доступна:"));
        isAvailableBox = new JCheckBox("", true);
        infoPanel.add(isAvailableBox);

        //панель кортів
        JPanel courtsPanel = new JPanel(new BorderLayout());
        courtsPanel.setBorder(BorderFactory.createTitledBorder("Корти"));

        courtsContainer = new JPanel();
        courtsContainer.setLayout(new BoxLayout(courtsContainer, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(courtsContainer);

        //кнопка "Додати корт"
        addCourtBtn = new JButton("+ Додати корт");
        addCourtBtn.addActionListener(e -> addCourtRow(0,"", true));

        courtsPanel.add(scrollPane, BorderLayout.CENTER);
        courtsPanel.add(addCourtBtn, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(infoPanel, BorderLayout.NORTH);
        centerPanel.add(courtsPanel, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        //кнопки "Зберегти" та "Скасувати"
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveButton = new JButton("Зберегти");
        cancelButton = new JButton("Скасувати");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        addCourtRow(0, "", true);
    }

    //додати рядок для корту
    private void addCourtRow(int id, String name, boolean available) {
        CourtUIBlock block = new CourtUIBlock(id, name, available);
        courtBlocks.add(block);
        courtsContainer.add(block.panel);
        courtsContainer.revalidate();
        courtsContainer.repaint();
    }

    //заповнення полів при редагуванні
    public void fillFields(Location location) {
        setTitle("Редагування місця проведення: ");
        nameField.setText(location.getName());
        cityField.setText(location.getCity());
        addressField.setText(location.getAddress());
        isAvailableBox.setSelected(location.getIsAvailable());

        courtsContainer.removeAll();
        courtBlocks.clear();

        if (location.getCourts() != null) {
            for (Court c : location.getCourts()) {
                addCourtRow(c.getId(), c.getName(), c.getIsAvailable());
            }
        }
        saveButton.setText("Оновити");
    }

    //клас блоку корту
    private class CourtUIBlock {
        int id; //айді
        JTextField nameField; //поле вводу назви корту
        JCheckBox availableBox; //обрання чи доступний
        JPanel panel; //панель

        //конструктор
        CourtUIBlock(int id, String name, boolean isAvailable) {
            this.id = id;
            this.nameField = new JTextField(name, 15);
            this.availableBox = new JCheckBox("Працює", isAvailable);

            this.panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            this.panel.add(new JLabel("Назва:"));
            this.panel.add(nameField);
            this.panel.add(availableBox);
        }
    }

    //отримати всі корти локації
    public List<Court> getCourts() {
        List<Court> list = new ArrayList<>();
        for (CourtUIBlock block : courtBlocks) {
            String name = block.nameField.getText().trim();
            if (!name.isEmpty()) {
                Court c = new Court();
                c.setId(block.id);
                c.setName(name);
                c.setIsAvailable(block.availableBox.isSelected());
                list.add(c);
            }
        }
        return list;
    }

    //геттери для контролеру
    public boolean isLocationAvailable() {
        return isAvailableBox.isSelected();
    }

    public String getName() { return nameField.getText(); }
    public String getCity() { return cityField.getText(); }
    public String getAddress() { return addressField.getText(); }
    public JButton getSaveButton() { return saveButton; }
    public JButton getCancelButton() { return cancelButton; }
}