package app.views.admin;

import app.DAO.UsersDAO;
import app.controllers.AnalyticsController;
import app.controllers.LocationController;
import app.controllers.LoginController;
import app.controllers.UserController;
import app.entities.user.User;
import app.utils.Session;
import app.views.admin.locations.CreateLocationView;
import app.views.admin.users.CreateUserView;
import app.DAO.LocationsDAO;
import app.entities.location.Location;
import app.views.auth.LoginView;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AdminMainView extends JFrame {

    private JTable usersTable, locationsTable; //таблиці користувачів та локацій
    private DefaultTableModel usersTableModel, locationsTableModel; //панелі
    private JButton addUserBtn, updateUserBtn, deleteUserBtn; //кнопки додавання, редагування, видалення користувачів
    private JButton addLocationBtn, updateLocationBtn, deleteLocationBtn; //кнопки додавання, редагування, видалення локацій
    private JButton logoutBtn; //кнопка "Вийти з акаунту"

    //конструктор
    public AdminMainView() {
        setTitle("Панель Адміністратора - " + Session.getCurrentUser().getFullName());
        setSize(950, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        //вкладка користувачів
        JPanel usersPanel = createUsersPanel();

        //вкладка локацій
        JPanel locationsPanel = createLocationsPanel();

        //вкладка аналітики
        JPanel analyticsPanel = createAnalyticsPanel();

        tabbedPane.addTab("Користувачі системи", usersPanel);
        tabbedPane.addTab("Довідник локацій", locationsPanel);
        tabbedPane.addTab("Аналітика", analyticsPanel);

        add(tabbedPane, BorderLayout.CENTER);

        //нижня панель виходу
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logoutBtn = new JButton("Вийти з акаунта");
        setupLogoutListener();
        bottomPanel.add(logoutBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        //початкове завантаження даних
        refreshUsersTable();
        refreshLocationsTable();
    }

    //створення панелі користувачів
    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //назви стовпців
        String[] uCols = {"ID", "ПІБ", "Логін", "Роль"};
        usersTableModel = new DefaultTableModel(uCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        usersTable = new JTable(usersTableModel);
        hideIdColumn(usersTable);
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);

        //кнопки дій
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addUserBtn = new JButton("Додати користувача");
        updateUserBtn = new JButton("Редагувати");
        deleteUserBtn = new JButton("Видалити");

        //кольори для кнопок
        styleGreenButton(addUserBtn);
        styleRedButton(deleteUserBtn);

        //налаштування слухачів для кнопок
        setupUserListeners();

        actions.add(addUserBtn);
        actions.add(updateUserBtn);
        actions.add(deleteUserBtn);
        panel.add(actions, BorderLayout.NORTH);

        return panel;
    }

    //створення панелі локацій
    private JPanel createLocationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //назви стовпців
        String[] lCols = {"ID", "Назва", "Місто", "Адреса", "Кількість кортів", "Статус"};
        locationsTableModel = new DefaultTableModel(lCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        locationsTable = new JTable(locationsTableModel);
        hideIdColumn(locationsTable);
        panel.add(new JScrollPane(locationsTable), BorderLayout.CENTER);

        //кнопки дій
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addLocationBtn = new JButton("Додати локацію");
        updateLocationBtn = new JButton("Редагувати");
        deleteLocationBtn = new JButton("Видалити");

        //колір для кнопок
        styleGreenButton(addLocationBtn);
        styleRedButton(deleteLocationBtn);

        //налаштування слухачів кнопок
        setupLocationsListeners();

        actions.add(addLocationBtn);
        actions.add(updateLocationBtn);
        actions.add(deleteLocationBtn);
        panel.add(actions, BorderLayout.NORTH);

        return panel;
    }

    //створити панель аналітики
    private JPanel createAnalyticsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        //види звітів для Combobox
        String[] reportNames = {
                "Оберіть звіт...",
                "Загальний призовий фонд (поточний рік)",
                "Фінансова аналітика по містах",
                "Найбагатші турніри по містах",
                "Журнал активності користувачів"
        };

        JComboBox<String> reportComboBox = new JComboBox<>(reportNames);
        JButton generateBtn = new JButton("Сформувати");

        generateBtn.setBackground(new Color(0, 123, 255));
        generateBtn.setForeground(Color.WHITE);

        topPanel.add(new JLabel("Тип звіту:"));
        topPanel.add(reportComboBox);
        topPanel.add(generateBtn);

        //центральна панель (таблиця)
        JTable analyticsTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(analyticsTable);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        //слухач для кнопки генерування звіту
        generateBtn.addActionListener(e -> {
            AnalyticsController.generateAdminReport(
                    this, reportComboBox.getSelectedIndex(), analyticsTable
            );
        });

        return panel;
    }

    //приховання стовпця id
    private void hideIdColumn(JTable table) {
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setPreferredWidth(0);
    }

    //оновлення таблиці локацій
    public void refreshLocationsTable() {
        locationsTableModel.setRowCount(0);
        List<Location> locations = LocationsDAO.getInstance().findAll();

        for (Location loc : locations) {
            int countCourts = (loc.getCourts() != null) ? loc.getCourts().size() : 0;

            locationsTableModel.addRow(new Object[]{
                    loc.getId(),
                    loc.getName(),
                    loc.getCity(),
                    loc.getAddress(),
                    countCourts,
                    loc.getIsAvailable() ? "Доступна" : "Закрита"
            });
        }
    }

    //оновлення таблиці користувачів
    public void refreshUsersTable() {
        usersTableModel.setRowCount(0);
        List<User> users = UsersDAO.getInstance().findAll();
        for (User u : users) {
            usersTableModel.addRow(new Object[]{
                    u.getId(),
                    u.getFullName(),
                    u.getLogin(),
                    u.getRole()});
        }
    }

    //зелений колір для кнопки
    private void styleGreenButton(JButton btn) {
        btn.setBackground(new Color(60, 179, 113));
        btn.setForeground(Color.black);
    }

    //червоний колір для кнопки
    private void styleRedButton(JButton btn) {
        btn.setBackground(new Color(220, 53, 69));
        btn.setForeground(Color.black);
    }

    //налаштування слухачів для кнопки виходу
    private void setupLogoutListener() {
        logoutBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Вийти?", "Вихід", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                Session.logout();
                this.dispose();
                LoginView loginView = new LoginView();
                new LoginController(loginView);
                loginView.setVisible(true);
            }
        });
    }

    //налаштування слухачів для кнопок дій з користувачами
    private void setupUserListeners() {
        //додати користувача
        addUserBtn.addActionListener(e -> {
            CreateUserView view = new CreateUserView(this);
            new UserController(view);
            view.setVisible(true);
            refreshUsersTable();
        });

        //оновити користувача
        updateUserBtn.addActionListener(e -> {
            int selectedRow = usersTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Спершу оберіть користувача у таблиці!", "Увага", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int userId = (int) usersTableModel.getValueAt(selectedRow, 0);
            User userToEdit = UsersDAO.getInstance().findById(userId).orElse(null);
            if (userToEdit != null) {
                CreateUserView editView = new CreateUserView(this);
                editView.fillFields(userToEdit);
                new UserController(editView, userToEdit);
                editView.setVisible(true);
                refreshUsersTable();
            }
        });

        //видалити користувача
        deleteUserBtn.addActionListener(e -> {
            int selectedRow = usersTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Оберіть користувача!");
                return;
            }

            int userId = (int) usersTableModel.getValueAt(selectedRow, 0);

            int confirm = JOptionPane.showConfirmDialog(this, "Видалити?", "Підтвердження", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    UsersDAO.getInstance().deleteById(userId);
                    refreshUsersTable();
                    JOptionPane.showMessageDialog(this, "Користувача видалено успішно!");
                } catch (RuntimeException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Помилка видалення: цей користувач фігурує у cтворених об'єктах системи.\nСпочатку видаліть об'єкти, де він вказаний.",
                            "Помилка бази даних",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    //налаштування слухачів для кнопок дій з локаціями
    private void setupLocationsListeners() {
        //додати локацію
        addLocationBtn.addActionListener(e -> {
                CreateLocationView view = new CreateLocationView(this);
                new LocationController(view);
                view.setVisible(true);
                refreshLocationsTable();
            });

        //оновити локацію
        updateLocationBtn.addActionListener(e -> {
            int selectedRow = locationsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Спершу оберіть місце проведення у таблиці!", "Увага", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int locationId = (int) locationsTableModel.getValueAt(selectedRow, 0);
            Location locationToEdit = LocationsDAO.getInstance().findById(locationId).orElse(null);
            if (locationToEdit != null) {
                CreateLocationView editView = new CreateLocationView(this);
                editView.fillFields(locationToEdit);
                new LocationController(editView, locationToEdit);
                editView.setVisible(true);
                refreshLocationsTable();
            }
        });

        //видалити локацію
        deleteLocationBtn.addActionListener(e -> {
            int selectedRow = locationsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Оберіть місце проведення!");
                return;
            }

            int locationId = (int) locationsTableModel.getValueAt(selectedRow, 0);

            int confirm = JOptionPane.showConfirmDialog(this, "Видалити?", "Підтвердження", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    LocationsDAO.getInstance().deleteById(locationId);
                    refreshLocationsTable();
                    JOptionPane.showMessageDialog(this, "Місце проведення видалено успішно!");
                } catch (RuntimeException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Помилка видалення: це місце проведення фігурує у турнірах.\nСпочатку видаліть або змініть турніри, де він вказаний.",
                            "Помилка бази даних",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
}