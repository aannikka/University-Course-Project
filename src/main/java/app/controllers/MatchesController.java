package app.controllers;

import app.DAO.CourtsDAO;
import app.DAO.PlayersDAO;
import app.DAO.RefereesDAO;
import app.DAO.ScheduleDAO;
import app.entities.location.Court;
import app.entities.match.Match;
import app.entities.participant.Player;
import app.entities.participant.Referee;
import app.entities.tournament.Tournament;
import app.utils.Session;
import app.views.planner.matches.CreateMatchView;

import javax.swing.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public class MatchesController {
    private CreateMatchView view; //форма для створення(редагування) матчу
    private Match matchToEdit; //матч для редагування
    private Tournament currentTournament; //турнір

    //DAO-класи
    private ScheduleDAO matchesDAO = ScheduleDAO.getInstance();
    private PlayersDAO playersDAO = PlayersDAO.getInstance();
    private RefereesDAO refereeDAO = RefereesDAO.getInstance();
    private CourtsDAO courtsDAO = CourtsDAO.getInstance();

    private int currentUserId = Session.getCurrentUser().getId(); //айді користувача

    //конструктор для режиму створення матчу
    public MatchesController(CreateMatchView view, Tournament tournament) {
        this.view = view;
        this.currentTournament = tournament;
        initController();
    }

    //конструктор для режиму редагування матчу
    public MatchesController(CreateMatchView view, Match matchToEdit, Tournament tournament) {
        this.view =  view;
        this.matchToEdit = matchToEdit;
        this.currentTournament = tournament;
        initController();

        if (matchToEdit != null) {
            view.fillFields(matchToEdit);

            updateAvailableResources(); //завантаження вільних ресурсів на ці дату і час

            //встановлення поточних значень у ComboBox-ах
            view.getCourtComboBox().setSelectedItem(matchToEdit.getSelectedCourt());
            view.getFirstPlayerComboBox().setSelectedItem(matchToEdit.getFirstPlayer());
            view.getSecondPlayerComboBox().setSelectedItem(matchToEdit.getSecondPlayer());
            view.getRefereeComboBox().setSelectedItem(matchToEdit.getReferee());
        }
    }

    //ініціалізація слухачів подій для кнопок та полів вводу
    private void initController() {
        view.getTimeField().addActionListener(e -> updateAvailableResources());
        view.getSaveButton().addActionListener(e -> handleSave());
        view.getCancelButton().addActionListener(e->view.dispose());
    }

    //динамічне знаходження вільних кортів, гравців та суддей
    private void updateAvailableResources() {
        //перевірка на порожні поля
        String dateStr = view.getDateField().getText();
        String timeStr = view.getTimeField().getText();

        if (dateStr.isEmpty() || timeStr.isEmpty()) {
            JOptionPane.showMessageDialog(view, "Заповніть дату та час!");
            return;
        }

        //очищення списків перед новим пошуком
        view.getCourtComboBox().removeAllItems();
        view.getFirstPlayerComboBox().removeAllItems();
        view.getSecondPlayerComboBox().removeAllItems();
        view.getRefereeComboBox().removeAllItems();

        try {
            //зчитування та конвертація даних з форми
            //перевірка дати на відповідність рамкам турніру
            LocalDate date = LocalDate.parse(dateStr);

            if (date.isBefore(currentTournament.getDateStart()) || date.isAfter(currentTournament.getDateFinish())) {
                JOptionPane.showMessageDialog(view,
                        "Дата матчу повинна бути в межах турніру!\n" +
                                "Турнір проходить з " + currentTournament.getDateStart() + " по " + currentTournament.getDateFinish(),
                        "Невірна дата",
                        JOptionPane.WARNING_MESSAGE);

                view.getCourtComboBox().removeAllItems();
                view.getFirstPlayerComboBox().removeAllItems();
                return;
            }

            LocalTime time = LocalTime.parse(timeStr);

            //знаходження вільних кортів
            List<Court> courts = courtsDAO.findAvailable(currentTournament.getSelectedLocation().getId(), date, time);
            if (courts != null && !courts.isEmpty()) {
                courts.forEach(view.getCourtComboBox()::addItem);
                view.getCourtComboBox().setEnabled(true);
            } else {
                view.getCourtComboBox().setEnabled(false);
                JOptionPane.showMessageDialog(view, "На жаль, вільних кортів на вказану дату і час немає.");
            }

            //знаходження вільних гравців
            List<Player> players = playersDAO.findFreeByTournamentId(currentTournament.getId());
            // Якщо це редагування, повертаємо поточних гравців матчу до списку доступних
            if (matchToEdit != null) {
                players.add(matchToEdit.getFirstPlayer());
                players.add(matchToEdit.getSecondPlayer());
            }

            if (players != null && !players.isEmpty()) {
                players.forEach(player -> {
                    view.getFirstPlayerComboBox().addItem(player);
                    view.getSecondPlayerComboBox().addItem(player);
                });
                view.getFirstPlayerComboBox().setEnabled(true);
                view.getSecondPlayerComboBox().setEnabled(true);
            } else {
                view.getFirstPlayerComboBox().setEnabled(false);
                view.getSecondPlayerComboBox().setEnabled(false);
                JOptionPane.showMessageDialog(view, "Немає вільних гравців!");
            }

            //знаходження вільних суддей
            List<Referee> referees = refereeDAO.findFreeByTournamentId(currentTournament.getId());
            // Якщо це редагування, повертаємо поточного суддю матчу до списку доступних
            if (matchToEdit != null) {
                referees.add(matchToEdit.getReferee());
            }

            if (referees != null && !referees.isEmpty()) {
                referees.forEach(referee -> {
                    view.getRefereeComboBox().addItem(referee);
                });
                view.getRefereeComboBox().setEnabled(true);
            } else {
                view.getRefereeComboBox().setEnabled(false);
                JOptionPane.showMessageDialog(view, "Вільних суддів немає!");
            }

        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(view, "Спочатку введіть коректні дату (YYYY-MM-DD) та час (HH:MM)");
        }
    }

    //збереження/оновлення матчу
    private void handleSave() {
        //перевірка на порожні поля
        String dateStr = view.getDateField().getText();
        String timeStr = view.getTimeField().getText();

        if (dateStr.isEmpty() || timeStr.isEmpty()) {
            JOptionPane.showMessageDialog(view, "Заповніть дату та час!");
            return;
        }

        try {
            //зчитування та конвертація даних з форми
            Match match = (matchToEdit == null) ? new Match() : matchToEdit;

            match.setTournamentId(currentTournament.getId());
            match.setDate(LocalDate.parse(view.getDateField().getText()));
            match.setStartTime(LocalTime.parse(view.getTimeField().getText()));
            match.setSelectedCourt(view.getSelectedCourt());
            match.setFirstPlayer(view.getFirstPlayer());
            match.setSecondPlayer(view.getSecondPlayer());
            match.setReferee(view.getReferee());

            //внутрішня бізнес-валідація сутності
            List<String> errors = match.validate(currentTournament, matchesDAO);
            if(errors.isEmpty()) {
                //збереження в базу даних (створення або оновлення)
                if(matchToEdit==null) {
                    //гілка створення
                    matchesDAO.save(match, currentTournament.getId(), currentUserId);
                    JOptionPane.showMessageDialog(view, "Матч успішно створено!");
                } else {
                    //гілка оновлення
                    matchesDAO.update(match);
                    JOptionPane.showMessageDialog(view, "Матч оновлено!");
                }
                //перевірка, чи достатньо матчів для переходу турніру в статус "Заплановано"
                TournamentController.checkAndScheduleTournament(currentTournament.getId());
                view.dispose();
            } else {
                JOptionPane.showMessageDialog(view, String.join("\n", errors), "Помилка", JOptionPane.WARNING_MESSAGE);
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(view, "Невірний формат дати! Використовуйте YYYY-MM-DD.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(view, "Помилка: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
