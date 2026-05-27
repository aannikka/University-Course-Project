package app.controllers;

import app.DAO.RefereesDAO;

import app.DAO.TournamentsDAO;
import app.entities.participant.Qualification;
import app.entities.participant.Referee;
import app.entities.tournament.Tournament;
import app.utils.Session;
import app.views.registrar.referees.CreateRefereeView;

import javax.swing.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class RefereeController {
    private CreateRefereeView view; //форма створення(редагування) судді
    private Referee refereeToEdit; //суддя для редагування
    private RefereesDAO refereesDAO = RefereesDAO.getInstance(); //DAO
    private int currentUserId = Session.getCurrentUser().getId(); //айді користувача

    //конструктора для режиму створення судді
    public RefereeController(CreateRefereeView view) {
        this.view = view;
        initController();
        loadTournaments();
    }

    //конструктора для режиму редагування судді
    public RefereeController(CreateRefereeView view, Referee refereeToEdit) {
        this.view = view;
        this.refereeToEdit = refereeToEdit;
        initController();
        loadTournaments();
    }

    //ініціалізація слухачів подій для кнопок та полів вводу
    private void initController() {
        view.getSaveButton().addActionListener(e->handleSave());
        view.getCancelButton().addActionListener(e->view.dispose());
    }

    //динамічне завантаження списку доступних турнірів у випадаючий список
    private void loadTournaments() {
        List<Tournament> tournaments = TournamentsDAO.getInstance().findAll();
        view.getTournamentComboBox().removeAllItems();
        for(Tournament t : tournaments) {
            view.getTournamentComboBox().addItem(t);
        }
    }

    //збереження/оновлення судді
    private void handleSave() {
        //перевірка на порожні поля
        String rawName = view.getFullName().trim();
        String rawPhone = view.getPhoneNumber().trim();
        String rawDate = view.getDateBirth().trim();

        if (rawName.isEmpty() || rawPhone.isEmpty() || rawDate.isEmpty()) {
            JOptionPane.showMessageDialog(view, "Будь ласка, заповніть усі поля!", "Увага", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            //зчитування та конвертація даних з форми
            Referee referee = (refereeToEdit == null) ? new Referee() : refereeToEdit;

            referee.setFullName(view.getFullName());
            referee.setPhoneNumber(view.getPhoneNumber());
            referee.setDateBirth(LocalDate.parse(view.getDateBirth()));
            Qualification qualification = (Qualification) view.getQualificationComboBox().getSelectedItem();
            referee.setQualification(qualification);

            //перевірка лімітів обраного турніру (тільки при створенні)
            Tournament selectedTournament = (Tournament) view.getTournamentComboBox().getSelectedItem();

            if (selectedTournament != null) {
                int currentCount = TournamentsDAO.getInstance().getRegisteredRefereesCount(selectedTournament.getId());

                int maxReferees = selectedTournament.countReferee();

                if (refereeToEdit == null && currentCount >= maxReferees) {
                    JOptionPane.showMessageDialog(view,
                            "Неможливо зареєструвати: у цьому турнірі закінчилися вільні місця для суддів!",
                            "Турнір заповнений",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            //перевірка чи суддю вже не зареєстровано на обраний турнір
            boolean isAlreadyRegistered = false;
            if (selectedTournament != null) {
                isAlreadyRegistered = refereesDAO.isExistByPhone(referee.getPhoneNumber(), selectedTournament.getId());
            }

            List<String> errors;

            //збереження в базу даних (створення або оновлення)
            if (refereeToEdit == null) {
                //гілка створення
                //внутрішня бізнес-валідація сутності
                errors = referee.validate(selectedTournament, isAlreadyRegistered);
                if (errors.isEmpty()) {
                    refereesDAO.saveAndRegisterToTournament(referee, selectedTournament.getId(), currentUserId);
                    TournamentController.checkAndCloseRegistration(selectedTournament.getId());
                    JOptionPane.showMessageDialog(view, "Суддю успішно створено та зареєстровано!");
                    view.dispose();
                } else {
                    JOptionPane.showMessageDialog(view, String.join("\n", errors), "Помилка реєстрації", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                //гілка редагування
                errors = referee.validateForUpdate();
                if (errors.isEmpty()) {
                    refereesDAO.update(referee);
                    JOptionPane.showMessageDialog(view, "Дані судді оновлено!");
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
