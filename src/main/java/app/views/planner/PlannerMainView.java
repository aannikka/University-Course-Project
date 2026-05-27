package app.views.planner;

import app.DAO.ScheduleDAO;
import app.DAO.TournamentsDAO;
import app.controllers.AnalyticsController;
import app.controllers.LoginController;
import app.controllers.MatchesController;
import app.entities.match.Match;
import app.entities.tournament.Tournament;
import app.utils.Session;
import app.controllers.TournamentController;
import app.views.auth.LoginView;
import app.views.planner.matches.CreateMatchView;
import app.views.planner.tournaments.CreateTournamentView;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PlannerMainView extends JFrame {

    private JTable tournamentsTable, matchesTable; //таблиці турнірів та матчів
    private DefaultTableModel tournamentsTableModel, matchesTableModel; //панелі
    //кнопки додавання, редагування, видалення турнірів
    private JButton addTournamentBtn, updateTournamentBtn, deleteTournamentBtn;
    //кнопки додавання, редагування, видалення матчів
    private JButton addMatchBtn, updateMatchBtn, deleteMatchBtn;
    private JComboBox<String> tournamentFilter; //фільтр турнірів
    private JButton logoutBtn; //кнопка "Вийти з акаунту"
    private JLabel matchStatsLabel; //кількість матчів

    //конструктор
    public PlannerMainView() {
        setTitle("Панель Планувальника - " + Session.getCurrentUser().getFullName());
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        //вкладка турнірів
        JPanel tournamentsPanel = createTournamentsPanel();

        //вкладка матчів
        JPanel matchesPanel = createMatchesPanel();

        //вкладка звітів
        JPanel reportsPanel = createReportsPanel();

        tabbedPane.addTab("Керування турнірами", tournamentsPanel);
        tabbedPane.addTab("Розклад матчів", matchesPanel);
        tabbedPane.addTab("Аналітика", reportsPanel);

        add(tabbedPane, BorderLayout.CENTER);

        //нижня панель виходу
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logoutBtn = new JButton("Вийти з акаунта");
        setupLogoutListener();
        bottomPanel.add(logoutBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        //початкове завантаження даних
        refreshTournamentsTable();
        refreshMatchesTable();
    }

    //створення панелі турнірів
    private JPanel createTournamentsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //назви стовпців
        String[] cols = {"ID", "Назва", "Місто", "Місце проведення", "Початок", "Кінець", "Статус", "Кількість зареєстрованих гравців", "Кількість зареєстрованих суддів"};
        tournamentsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tournamentsTable = new JTable(tournamentsTableModel);
        hideIdColumn(tournamentsTable);
        panel.add(new JScrollPane(tournamentsTable), BorderLayout.CENTER);

        //кнопки дій
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addTournamentBtn = new JButton("Створити турнір");
        updateTournamentBtn = new JButton("Редагувати");
        deleteTournamentBtn = new JButton("Видалити");

        //кольори для кнопок
        styleGreenButton(addTournamentBtn);
        styleRedButton(deleteTournamentBtn);

        //налаштування слухачів для кнопок
        setupTournamentListeners();

        actions.add(addTournamentBtn);
        actions.add(updateTournamentBtn);
        actions.add(deleteTournamentBtn);
        panel.add(actions, BorderLayout.NORTH);

        return panel;
    }

    //створення панелі матчів
    private JPanel createMatchesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //фільтр турнірів
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Обрати турнір:"));
        tournamentFilter = new JComboBox<>(new String[]{"Всі турніри"});
        filterPanel.add(tournamentFilter);

        matchStatsLabel = new JLabel("Оберіть турнір для перегляду статистики");
        matchStatsLabel.setFont(new Font("Arial", Font.BOLD, 12));
        filterPanel.add(Box.createHorizontalStrut(20)); // Відступ
        filterPanel.add(matchStatsLabel);

        panel.add(filterPanel, BorderLayout.NORTH);

        //назви стовпців
        String[] mCols = {"ID", "Корт", "Гравець 1", "Гравець 2", "Cуддя", "Дата", "Час"};
        matchesTableModel = new DefaultTableModel(mCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        matchesTable = new JTable(matchesTableModel);
        hideIdColumn(matchesTable);
        panel.add(new JScrollPane(matchesTable), BorderLayout.CENTER);

        //кнопки дій
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addMatchBtn = new JButton("Сформувати матч");
        updateMatchBtn = new JButton("Редагувати матч");
        deleteMatchBtn = new JButton("Скасувати матч");

        //колір для кнопок
        styleGreenButton(addMatchBtn);
        styleRedButton(deleteMatchBtn);

        //завантаження турнірів
        loadTournamentFilter();
        //налаштування слухачів кнопок
        setupMatchListeners();

        actions.add(addMatchBtn);
        actions.add(updateMatchBtn);
        actions.add(deleteMatchBtn);
        panel.add(actions, BorderLayout.SOUTH);

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
                "Турніри за призовим фондом (від і до)",
                "Вільні локації (без турнірів)"
        };
        JComboBox<String> reportComboBox = new JComboBox<>(reportNames);

        JLabel minLabel = new JLabel("Фонд ВІД:");
        JTextField minField = new JTextField(7);
        JLabel maxLabel = new JLabel("ДО:");
        JTextField maxField = new JTextField(7);
        JButton generateBtn = new JButton("Сформувати");

        generateBtn.setBackground(new Color(255, 165, 0));
        generateBtn.setForeground(Color.BLACK);

        minLabel.setVisible(false); minField.setVisible(false);
        maxLabel.setVisible(false); maxField.setVisible(false);

        topPanel.add(new JLabel("Тип звіту:"));
        topPanel.add(reportComboBox);
        topPanel.add(minLabel);
        topPanel.add(minField);
        topPanel.add(maxLabel);
        topPanel.add(maxField);
        topPanel.add(generateBtn);

        //зчитування значень з полів
        reportComboBox.addActionListener(e -> {
            int selectedIndex = reportComboBox.getSelectedIndex();
            minField.setText("");
            maxField.setText("");

            if (selectedIndex == 1) {
                minLabel.setVisible(true); minField.setVisible(true);
                maxLabel.setVisible(true); maxField.setVisible(true);
            } else {
                minLabel.setVisible(false); minField.setVisible(false);
                maxLabel.setVisible(false); maxField.setVisible(false);
            }
        });

        //центральна панель (таблиця)
        JTable reportsTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(reportsTable);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        //слухач для кнопки генерування звіту
        generateBtn.addActionListener(e -> {
            AnalyticsController.generatePlannerReport(
                    this, reportComboBox.getSelectedIndex(), minField.getText().trim(), maxField.getText().trim(), reportsTable
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

    //завантаження турнірів
    private void loadTournamentFilter() {
        List<Tournament> tournaments = TournamentsDAO.getInstance().findAll();
        tournamentFilter.removeAllItems();
        tournamentFilter.addItem("Всі турніри");
        for (Tournament t : tournaments) {
            if (t.getStatus().getId() >= 2) {
                tournamentFilter.addItem(t.getName());
            }
        }
        tournamentFilter.addActionListener(e -> refreshMatchesTable());
    }

    //оновлення таблиці турнірів
    public void refreshTournamentsTable() {
        tournamentsTableModel.setRowCount(0);
        List<Tournament> tournaments = TournamentsDAO.getInstance().findAll();

        for (Tournament t : tournaments) {
            int currentPlayers = TournamentsDAO.getInstance().getRegisteredPlayersCount(t.getId());
            int currentReferees = TournamentsDAO.getInstance().getRegisteredRefereesCount(t.getId());
            String playersStatus = currentPlayers + " / " + t.getMaxQuantityParticipant();
            String refereesStatus = currentReferees + " / " + t.countReferee();
            tournamentsTableModel.addRow(new Object[]{
                    t.getId(),
                    t.getName(),
                    t.getCity(),
                    t.getSelectedLocation(),
                    t.getDateStart(),
                    t.getDateFinish(),
                    t.getStatus().getDisplayName(),
                    playersStatus,
                    refereesStatus
            });
        }
    }

    //оновлення таблиці матчів
    public void refreshMatchesTable() {
        matchesTableModel.setRowCount(0);
        String selectedName = (String) tournamentFilter.getSelectedItem();

        if (selectedName == null || selectedName.equals("Всі турніри")) {
            matchStatsLabel.setText("Статистика доступна тільки для конкретного турніру");
            List<Match> matches = ScheduleDAO.getInstance().findAll();
            return;
        }

        Tournament t = TournamentsDAO.getInstance().findByName(selectedName).orElse(null);
        if (t != null) {
            int created = TournamentsDAO.getInstance().getCreatedMatchesCount(t.getId());

            int totalNeeded = t.getMaxQuantityParticipant() / 2;

            matchStatsLabel.setText(String.format("Матчі: %d / %d (Залишилось: %d)",
                    created, totalNeeded, (totalNeeded - created)));

            if (created >= totalNeeded) {
                matchStatsLabel.setForeground(new Color(0, 128, 0));
            } else {
                matchStatsLabel.setForeground(Color.BLUE);
            }

            List<Match> matches = ScheduleDAO.getInstance().findByTournamentId(t.getId());
            for (Match m : matches) {
                matchesTableModel.addRow(new Object[]{
                        m.getId(),
                        m.getSelectedCourt().getName(),
                        m.getFirstPlayer().getFullName(),
                        m.getSecondPlayer().getFullName(),
                        m.getReferee().getFullName(),
                        m.getDate(),
                        m.getStartTime()
                });
            }
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

    //налаштування слухачів для кнопок дій турніру
    private void setupTournamentListeners() {
        //додати турнір
        addTournamentBtn.addActionListener(e -> {
            CreateTournamentView view = new CreateTournamentView(this);
            new TournamentController(view);
            view.setVisible(true);
            refreshTournamentsTable();
        });

        //оновити турнір
        updateTournamentBtn.addActionListener(e -> {
            int selectedRow = tournamentsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Спершу оберіть турнір у таблиці!", "Увага", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int tournamentId = (int) tournamentsTableModel.getValueAt(selectedRow, 0);
            Tournament tournamentToEdit = TournamentsDAO.getInstance().findById(tournamentId).orElse(null);
            if (tournamentToEdit != null) {
                CreateTournamentView editView = new CreateTournamentView(this);
                editView.fillFields(tournamentToEdit);
                new TournamentController(editView, tournamentToEdit);
                editView.setVisible(true);
                refreshTournamentsTable();
            }
        });

        //видалити турнір
        deleteTournamentBtn.addActionListener(e -> {
            int selectedRow = tournamentsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Оберіть турнір!");
                return;
            }

            int tournamentId = (int) tournamentsTableModel.getValueAt(selectedRow, 0);

            int confirm = JOptionPane.showConfirmDialog(this, "Видалити?", "Підтвердження", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    TournamentsDAO.getInstance().deleteById(tournamentId);
                    refreshTournamentsTable();
                    loadTournamentFilter();
                    refreshMatchesTable();
                    JOptionPane.showMessageDialog(this, "Турнір видалено успішно!");
                } catch (RuntimeException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Помилка видалення турніру: " +  ex.getMessage(),
                            "Помилка бази даних",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

    }

    //налаштування слухачів для кнопок дій з матчами
    private void setupMatchListeners() {
        //додати матч
        addMatchBtn.addActionListener(e -> {
            String selectedName = (String) tournamentFilter.getSelectedItem();
            if (selectedName == null || selectedName.equals("Всі турніри")) {
                JOptionPane.showMessageDialog(this, "Спочатку оберіть конкретний турнір у фільтрі!");
                return;
            }

            Tournament t = TournamentsDAO.getInstance().findByName(selectedName).orElse(null);
            if (t != null) {
                CreateMatchView matchView = new CreateMatchView(this);
                new MatchesController(matchView, t);
                matchView.setVisible(true);
                refreshMatchesTable();
                refreshTournamentsTable();
            }
        });

        //оновити матч
        updateMatchBtn.addActionListener(e -> {
            int selectedRow = matchesTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Спершу оберіть матч у таблиці!");
                return;
            }

            int matchId = (int) matchesTableModel.getValueAt(selectedRow, 0);
            Match matchToEdit = ScheduleDAO.getInstance().findById(matchId).orElse(null);
            if (matchToEdit != null) {
                String selectedTournamentName = (String) tournamentFilter.getSelectedItem();
                Tournament t = TournamentsDAO.getInstance()
                        .findByName(selectedTournamentName).orElse(null);

                if (t != null) {
                    CreateMatchView editView = new CreateMatchView(this);
                    editView.fillFields(matchToEdit);
                    new MatchesController(editView, matchToEdit, t);
                    editView.setVisible(true);
                    refreshMatchesTable();
                }
            }
        });

        //видалити матч
        deleteMatchBtn.addActionListener(e -> {
            int selectedRow = matchesTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Оберіть матч для скасування!");
                return;
            }

            int matchId = (int) matchesTableModel.getValueAt(selectedRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "Скасувати цей матч?", "Підтвердження", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                Match match = ScheduleDAO.getInstance().findById(matchId).orElse(null);
                if (match != null) {
                    int tId = match.getTournamentId();
                    ScheduleDAO.getInstance().deleteById(matchId);

                    TournamentController.checkAndRevertToClosed(tId);

                    refreshMatchesTable();
                    refreshTournamentsTable();
                    JOptionPane.showMessageDialog(this, "Матч скасовано!");
                }
            }
        });
    }
}