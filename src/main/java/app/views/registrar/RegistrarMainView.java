package app.views.registrar;

import app.DAO.PlayersDAO;
import app.DAO.RefereesDAO;
import app.controllers.*;
import app.entities.participant.Player;
import app.entities.participant.Referee;
import app.utils.Session;
import app.views.auth.LoginView;
import app.views.registrar.players.CreatePlayerView;
import app.views.registrar.referees.CreateRefereeView;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class RegistrarMainView extends JFrame {

    private JTable playersTable, refereesTable; //таблиці гравців та суддів
    private DefaultTableModel playersTableModel, refereesTableModel; //панелі
    //кнопки додавання, редагування, видалення гравців
    private JButton addPlayerBtn, updatePlayerBtn, deletePlayerBtn;
    //кнопки додавання, редагування, видалення суддів
    private JButton addRefereeBtn, updateRefereeBtn, deleteRefereeBtn;
    private JButton logoutBtn; //кнопка "Вийти з акаунту"

    //конструктор
    public RegistrarMainView() {
        setTitle("Панель Реєстратора - " + Session.getCurrentUser().getFullName());
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        //панель гравців
        JPanel playersPanel = createPlayersPanel();

        //панель суддів
        JPanel refereesPanel = createRefereesPanel();

        //вкладка звітів
        JPanel reportsPanel = createReportsPanel();

        tabbedPane.addTab("Гравці", playersPanel);
        tabbedPane.addTab("Судді", refereesPanel);
        tabbedPane.addTab("Аналітика", reportsPanel);

        add(tabbedPane, BorderLayout.CENTER);

        //нижня панель виходу
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logoutBtn = new JButton("Вийти з акаунта");
        setupLogoutListener();
        bottomPanel.add(logoutBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        //початкове завантаження даних
        refreshPlayersTable();
        refreshRefereesTable();
    }

    //створення панелі гравців
    private JPanel createPlayersPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //назви стовпців
        String[] cols = {"ID", "ПІБ", "Номер телефону", "Дата народження", "Рейтинг", "Турніри"};
        playersTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {return false;}
        };
        playersTable = new JTable(playersTableModel);
        hideIdColumn(playersTable);
        panel.add(new JScrollPane(playersTable), BorderLayout.CENTER);

        //кнопки дій
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addPlayerBtn = new JButton("Додати гравця");
        updatePlayerBtn = new JButton("Редагувати");
        deletePlayerBtn = new JButton("Видалити");

        //кольори для кнопок
        styleGreenButton(addPlayerBtn);
        styleRedButton(deletePlayerBtn);

        //налаштування слухачів для кнопок
        setupPlayersListeners();

        actions.add(addPlayerBtn);
        actions.add(updatePlayerBtn);
        actions.add(deletePlayerBtn);
        panel.add(actions, BorderLayout.NORTH);
        return panel;
    }

    //створення панелі суддів
    private JPanel createRefereesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //назви стовпців
        String[] cols = {"ID", "ПІБ", "Номер телефону", "Дата народження", "Кваліфікація", "Турніри"};
        refereesTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {return false;}
        };
        refereesTable = new JTable(refereesTableModel);
        hideIdColumn(refereesTable);
        panel.add(new JScrollPane(refereesTable), BorderLayout.CENTER);

        //кнопки дій
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addRefereeBtn = new JButton("Додати суддю");
        updateRefereeBtn = new JButton("Редагувати");
        deleteRefereeBtn = new JButton("Видалити");

        //колір для кнопок
        styleGreenButton(addRefereeBtn);
        styleRedButton(deleteRefereeBtn);

        //налаштування слухачів кнопок
        setupRefereesListeners();

        actions.add(addRefereeBtn);
        actions.add(updateRefereeBtn);
        actions.add(deleteRefereeBtn);
        panel.add(actions, BorderLayout.NORTH);

        return panel;
    }

    //створити панель звітів
    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        //види звітів для Combobox
        String[] reportNames = {
                "Оберіть звіт...",
                "Список гравців турніру",
                "Пошук гравця (за літерою/ім'ям)",
                "Найкращі гравці (за рейтингом)"
        };
        JComboBox<String> reportComboBox = new JComboBox<>(reportNames);

        JLabel paramLabel = new JLabel("Параметр:");
        JTextField paramField = new JTextField(15);
        JButton generateBtn = new JButton("Сформувати");

        generateBtn.setBackground(new Color(60, 179, 113));
        generateBtn.setForeground(Color.BLACK);

        paramLabel.setVisible(false);
        paramField.setVisible(false);

        topPanel.add(new JLabel("Тип звіту:"));
        topPanel.add(reportComboBox);
        topPanel.add(paramLabel);
        topPanel.add(paramField);
        topPanel.add(generateBtn);

        //зчитування значень з полів
        reportComboBox.addActionListener(e -> {
            int selectedIndex = reportComboBox.getSelectedIndex();
            paramField.setText("");

            if (selectedIndex == 1) {
                paramLabel.setText("Введіть назву турніру:");
                paramLabel.setVisible(true);
                paramField.setVisible(true);
            } else if (selectedIndex == 2) {
                paramLabel.setText("Введіть літеру/ПІБ:");
                paramLabel.setVisible(true);
                paramField.setVisible(true);
            } else {
                paramLabel.setVisible(false);
                paramField.setVisible(false);
            }
        });

        //центральна панель (таблиця)
        JTable reportsTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(reportsTable);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        //слухач для кнопки генерування звіту
        generateBtn.addActionListener(e -> {
            AnalyticsController.generateRegistrarReport(
                    this, reportComboBox.getSelectedIndex(), paramField.getText().trim(), reportsTable
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

    //оновлення таблиці гравців
    public void refreshPlayersTable() {
        playersTableModel.setRowCount(0);
        List<Player> players = PlayersDAO.getInstance().findAll();

        for(Player player : players) {
           playersTableModel.addRow(new Object[]{
                   player.getId(),
                   player.getFullName(),
                   player.getPhoneNumber(),
                   player.getDateBirth(),
                   player.getRating(),
                   player.getTournamentNames() != null ? player.getTournamentNames() : "-"
           });
        }
    }

    //оновлення таблиці суддів
    public void refreshRefereesTable() {
        refereesTableModel.setRowCount(0);
        List<Referee> referees = RefereesDAO.getInstance().findAll();
        for (Referee referee : referees) {
            refereesTableModel.addRow(new Object[]{
               referee.getId(),
               referee.getFullName(),
               referee.getPhoneNumber(),
               referee.getDateBirth(),
               referee.getQualification().getDisplayName(),
               referee.getTournamentNames() != null ? referee.getTournamentNames() : "-"
            });
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

    //налаштування слухачів для кнопок дій гравців
    private void setupPlayersListeners() {
        //додати гравця
        addPlayerBtn.addActionListener(e -> {
            CreatePlayerView view = new CreatePlayerView(this);
            new PlayerController(view);
            view.setVisible(true);
            refreshPlayersTable();
        });

        //оновити гравця
        updatePlayerBtn.addActionListener(e -> {
            int selectedRow = playersTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Спершу оберіть гравця у таблиці!", "Увага", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int playerId = (int) playersTableModel.getValueAt(selectedRow, 0);
            Player playerToEdit = PlayersDAO.getInstance().findById(playerId).orElse(null);
            if (playerToEdit != null) {
                CreatePlayerView editView = new CreatePlayerView(this);
                editView.fillFields(playerToEdit);
                new PlayerController(editView, playerToEdit);
                editView.setVisible(true);
                refreshPlayersTable();
            }
        });

        //видалити гравця
        deletePlayerBtn.addActionListener(e -> {
            int selectedRow = playersTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Оберіть гравця!");
                return;
            }

            int playerId = (int) playersTable.getValueAt(selectedRow, 0);

            if (PlayersDAO.getInstance().isPlayerAssignedToMatches(playerId)) {
                JOptionPane.showMessageDialog(this,
                        "Неможливо видалити гравця! Він уже призначений на матчі. \nСпочатку скасуйте ці матчі в панелі планувальника.",
                        "Помилка видалення", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Видалити? Це також скасує його реєстрацію у всіх турнірах.",
                    "Підтвердження", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    List<Integer> affectedTournaments = PlayersDAO.getInstance().findTournamentIdsByPlayerId(playerId);
                    PlayersDAO.getInstance().deleteById(playerId);
                    for(Integer tournamentId : affectedTournaments) {
                        TournamentController.checkAndOpenRegistration(tournamentId);
                    }
                    refreshPlayersTable();
                    JOptionPane.showMessageDialog(this, "Гравця видалено успішно!");
                } catch (RuntimeException ex) {
                    JOptionPane.showMessageDialog(this, "Помилка бази даних: " + ex.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    //налаштування слухачів для кнопок дій суддів
    private void setupRefereesListeners() {
        //додати суддю
        addRefereeBtn.addActionListener(e -> {
            CreateRefereeView view = new CreateRefereeView(this);
            new RefereeController(view);
            view.setVisible(true);
            refreshRefereesTable();
        });

        //оновити суддю
        updateRefereeBtn.addActionListener(e -> {
            int selectedRow = refereesTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Спершу оберіть суддю у таблиці!", "Увага", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int refereeId = (int) refereesTableModel.getValueAt(selectedRow, 0);
            Referee refereeToEdit = RefereesDAO.getInstance().findById(refereeId).orElse(null);
            if (refereeToEdit != null) {
                CreateRefereeView editView = new CreateRefereeView(this);
                editView.fillFields(refereeToEdit);
                new RefereeController(editView, refereeToEdit);
                editView.setVisible(true);
                refreshRefereesTable();
            }
        });

        //видалити суддю
        deleteRefereeBtn.addActionListener(e -> {
            int selectedRow = refereesTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Оберіть суддю!");
                return;
            }

            int refereeId = (int) refereesTable.getValueAt(selectedRow, 0);

            // --- ПЕРЕВІРКА НА МАТЧІ ---
            if (RefereesDAO.getInstance().isRefereeAssignedToMatches(refereeId)) {
                JOptionPane.showMessageDialog(this,
                        "Цей суддя вже судить заплановані матчі! Видалення неможливе.",
                        "Помилка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this, "Видалити?", "Підтвердження", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    List<Integer> affectedTournaments = RefereesDAO.getInstance().findTournamentIdsByRefereeId(refereeId);
                    RefereesDAO.getInstance().deleteById(refereeId);
                    for(Integer tournamentId : affectedTournaments) {
                        TournamentController.checkAndOpenRegistration(tournamentId);
                    }
                    refreshRefereesTable();
                    JOptionPane.showMessageDialog(this, "Суддю видалено успішно!");
                } catch (RuntimeException ex) {
                    JOptionPane.showMessageDialog(this, "Помилка бази даних: " + ex.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
}
