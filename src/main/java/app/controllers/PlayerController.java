package app.controllers;

import app.DAO.PlayersDAO;
import app.DAO.TournamentsDAO;
import app.entities.participant.Player;
import app.entities.tournament.Tournament;
import app.utils.Session;
import app.views.registrar.players.CreatePlayerView;

import javax.swing.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class PlayerController {
    private CreatePlayerView view; //форма створення(редагування) гравця
    private Player playerToEdit; //гравець для редагування
    private PlayersDAO playersDAO = PlayersDAO.getInstance(); //DAO
    private int currentUserId = Session.getCurrentUser().getId(); //айді користувача

    //конструктора для режиму створення гравця
    public PlayerController(CreatePlayerView view) {
        this.view = view;
        initController();
        loadTournaments();
    }

    //конструктора для режиму редагування гравця
    public PlayerController(CreatePlayerView view, Player playerToEdit) {
        this.view = view;
        this.playerToEdit = playerToEdit;
        initController();
        loadTournaments();
    }

    //ініціалізація слухачів подій для кнопок та полів вводу
    private void initController() {
        view.getSaveButton().addActionListener(e -> handleSave());
        view.getCancelButton().addActionListener(e -> view.dispose());
    }

    //динамічне завантаження списку доступних турнірів у випадаючий список
    private void loadTournaments() {
        List<Tournament> tournaments = TournamentsDAO.getInstance().findAll();
        view.getTournamentComboBox().removeAllItems();
        for (Tournament t : tournaments) {
            view.getTournamentComboBox().addItem(t);
        }
    }

    //збереження/оновлення гравця
    private void handleSave() {
        //перевірка на порожні поля
        String rawName = view.getFullName().trim();
        String rawPhone = view.getPhoneNumber().trim();
        String rawDate = view.getDateBirth().trim();
        String rawRating = view.getRating().trim();

        if (rawName.isEmpty() || rawPhone.isEmpty() || rawDate.isEmpty() || rawRating.isEmpty()) {
            JOptionPane.showMessageDialog(view, "Будь ласка, заповніть усі поля!", "Увага", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            //зчитування та конвертація даних з форми
            Player player = (playerToEdit == null) ? new Player() : playerToEdit;

            player.setFullName(view.getFullName());
            player.setPhoneNumber(view.getPhoneNumber());
            player.setDateBirth(LocalDate.parse(view.getDateBirth()));

            Double rating = Double.valueOf(view.getRating());
            player.setRating(rating);

            //перевірка лімітів обраного турніру (тільки при створенні)
            Tournament selectedTournament = (Tournament) view.getTournamentComboBox().getSelectedItem();

            if(selectedTournament != null) {
                int currentCount = TournamentsDAO.getInstance().getRegisteredPlayersCount(selectedTournament.getId());

                if(playerToEdit == null && currentCount >= selectedTournament.getMaxQuantityParticipant()) {
                    JOptionPane.showMessageDialog(view,
                            "Неможливо зареєструвати: у цьому турнірі закінчилися вільні місця для гравців!",
                            "Турнір заповнений",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            //перевірка чи гравця вже не зареєстровано на обраний турнір
            boolean isAlreadyRegistered = false;
            if (selectedTournament != null) {
                isAlreadyRegistered = playersDAO.isExistByPhone(player.getPhoneNumber(), selectedTournament.getId());
            }

            List<String> errors;

                //збереження в базу даних (створення або оновлення)
                if (playerToEdit == null) {
                    //гілка створення
                    //внутрішня бізнес-валідація сутності
                    errors = player.validate(selectedTournament, isAlreadyRegistered);
                    if(errors.isEmpty()) {
                        playersDAO.saveAndRegisterToTournament(player, selectedTournament.getId(), currentUserId);
                        TournamentController.checkAndCloseRegistration(selectedTournament.getId());
                        JOptionPane.showMessageDialog(view, "Гравця успішно створено та зареєстровано!");
                        view.dispose();
                    } else {
                        JOptionPane.showMessageDialog(view, String.join("\n", errors), "Помилка реєстрації", JOptionPane.WARNING_MESSAGE);
                    }
                } else {
                    //гілка редагування
                    List<Integer> activeTournaments = playersDAO.findTournamentIdsByPlayerId(playerToEdit.getId());
                    int maxRequiredRating = 0;

                    //знаходження максимального прохідного рейтингу серед усіх турнірів
                    for (Integer tId : activeTournaments) {
                        Tournament t = TournamentsDAO.getInstance().findById(tId).orElse(null);
                        if (t != null && t.getMinRating() > maxRequiredRating) {
                            maxRequiredRating = t.getMinRating();
                        }
                    }
                    errors = player.validateForUpdate(maxRequiredRating);
                    if (errors.isEmpty()) {
                        playersDAO.update(player);
                        JOptionPane.showMessageDialog(view, "Дані гравця оновлено!");
                        view.dispose();
                    } else {
                    JOptionPane.showMessageDialog(view, String.join("\n", errors), "Помилка редагування", JOptionPane.WARNING_MESSAGE);
                    }
                }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(view, "Невірний формат дати! Використовуйте YYYY-MM-DD.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(view, "Помилка: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}