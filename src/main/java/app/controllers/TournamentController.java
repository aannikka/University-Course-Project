package app.controllers;

import app.DAO.LocationsDAO;
import app.DAO.PlayersDAO;
import app.DAO.TournamentsDAO;
import app.entities.location.Location;
import app.entities.tournament.Tournament;
import app.entities.tournament.TournamentStatus;
import app.utils.Session;
import app.views.planner.tournaments.CreateTournamentView;

import javax.swing.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentController {
    private CreateTournamentView view; //форма створення/редагування турніру
    private Tournament tournamentToEdit; //турнір для редагування (null, якщо створення)
    private TournamentsDAO tournamentsDAO = TournamentsDAO.getInstance(); //DAO
    private Map<Integer, String> availableLocationsMap; //мапа вільних місць проведення (id -> назва)
    private int currentUserId = Session.getCurrentUser().getId(); //айді користувача

    //конструктор для режиму створення турніру
    public TournamentController(CreateTournamentView view) {
        this.view = view;
        initController();
    }

    //конструктор для режиму редагування турніру
    public TournamentController(CreateTournamentView view, Tournament tournamentToEdit) {
        this.view = view;
        this.tournamentToEdit = tournamentToEdit;
        initController();

        if (tournamentToEdit != null) {
            view.fillFields(tournamentToEdit);

            loadLocations();
        }
    }

    //ініціалізація слухачів подій для кнопок та полів вводу
    private void initController() {
        view.getCityField().addActionListener(e -> loadLocations());
        view.getSaveButton().addActionListener(e -> handleSave());
        view.getCancelButton().addActionListener(e -> view.dispose());
    }

    //збереження/оновлення турніру
    private void handleSave() {
        //перевірка на порожні поля
        String nameStr = view.getTournamentName().trim();
        String dateStartStr = view.getStartDate().trim();
        String dateEndStr = view.getEndDate().trim();
        String prizeFundStr = view.getPrizeFund().trim();

        List<String> emptyFields = new ArrayList<>();

        if (nameStr.isEmpty()) emptyFields.add("- Назва турніру");
        if (dateStartStr.isEmpty()) emptyFields.add("- Дата початку");
        if (dateEndStr.isEmpty()) emptyFields.add("- Дата закінчення");
        if (prizeFundStr.isEmpty()) emptyFields.add("- Призовий фонд");

        if (!emptyFields.isEmpty()) {
            String message = "Будь ласка, заповніть наступні поля:\n" + String.join("\n", emptyFields);
            JOptionPane.showMessageDialog(view, message, "Увага: пусті поля", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            //зчитування та конвертація даних з форми
            Tournament tournament = (tournamentToEdit == null) ? new Tournament() : tournamentToEdit;

            tournament.setName(view.getTournamentName());
            tournament.setDateStart(LocalDate.parse(view.getStartDate()));
            tournament.setDateFinish(LocalDate.parse(view.getEndDate()));
            tournament.setCity(view.getCity());
            tournament.setMinRating(view.getMinRating());
            tournament.setMaxQuantityParticipant(view.getMaxParticipants());
            tournament.setPrizeFund(Double.parseDouble(view.getPrizeFund()));
            tournament.setDescription(view.getDescription());

            //прив'язка обраного місця проведення
            String selectedLocName = view.getSelectedLocation();
            if (selectedLocName == null || availableLocationsMap == null) {
                JOptionPane.showMessageDialog(view, "Оберіть місце проведення (спочатку введіть місто та натисніть Enter)");
                return;
            }

            int locationId = -1;
            for (Map.Entry<Integer, String> entry : availableLocationsMap.entrySet()) {
                if (entry.getValue().equals(selectedLocName)) {
                    locationId = entry.getKey();
                    break;
                }
            }

            if (locationId != -1) {
                Location loc = new Location();
                loc.setId(locationId);
                tournament.setSelectedLocation(loc);
            }

            //внутрішня бізнес-валідація сутності
            List<String> errors = tournament.validate(tournamentsDAO);
            if (!errors.isEmpty()) {
                JOptionPane.showMessageDialog(view, String.join("\n", errors), "Помилка валідації", JOptionPane.WARNING_MESSAGE);
                return;
            }

            //збереження в базу даних (створення або оновлення)
            if (tournamentToEdit == null) {
                //гілка створення
                tournament.setStatus(TournamentStatus.REGISTRATION_OPEN);
                tournamentsDAO.save(tournament, currentUserId);

                JOptionPane.showMessageDialog(view, "Турнір успішно створено!");
                view.dispose();

            } else {
                //гілка редагування (з перевіркою обмежень щодо вже зареєстрованих учасників)
                int newMaxPlayers = tournament.getMaxQuantityParticipant();
                int currentRegisteredPlayers = tournamentsDAO.getRegisteredPlayersCount(tournamentToEdit.getId());

                if (newMaxPlayers < currentRegisteredPlayers) {
                    JOptionPane.showMessageDialog(view,
                            "Неможливо зменшити кількість учасників до " + newMaxPlayers +
                                    "!\nНа турнір вже зареєстровано " + currentRegisteredPlayers + " гравців.",
                            "Помилка оновлення",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int newMaxReferees = tournament.countReferee();
                int currentRegisteredReferees = tournamentsDAO.getRegisteredRefereesCount(tournamentToEdit.getId());

                if (newMaxReferees < currentRegisteredReferees) {
                    JOptionPane.showMessageDialog(view,
                            "Неможливо зберегти: новий ліміт гравців вимагає " + newMaxReferees + " суддів,\n" +
                                    "але на турнір вже зареєстровано " + currentRegisteredReferees + " суддів!",
                            "Помилка оновлення",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double newMinRating = tournament.getMinRating();
                double lowestCurrentRating = PlayersDAO.getInstance().getLowestRatingInTournament(tournamentToEdit.getId());

                if (newMinRating > lowestCurrentRating) {
                    JOptionPane.showMessageDialog(view,
                            "Неможливо підвищити мінімальний рейтинг до " + newMinRating + "!\n" +
                                    "На турнір вже зареєстрований гравець із рейтингом " + lowestCurrentRating + ".\n" +
                                    "Спочатку скасуйте його реєстрацію.",
                            "Помилка оновлення",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                tournamentsDAO.update(tournament);
                JOptionPane.showMessageDialog(view, "Дані турніру оновлено!");
                view.dispose();
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(view, "Невірний формат дати! Використовуйте YYYY-MM-DD", "Помилка", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(view, "Призовий фонд має бути числом", "Помилка", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(view, "Помилка БД: " + ex.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }

    //динамічне завантаження вільних місць проведення залежно від введеного міста та дат
    private void loadLocations() {
        String city = view.getCity();
        String startStr = view.getStartDate();
        String endStr = view.getEndDate();

        if (city.isEmpty() || startStr.isEmpty() || endStr.isEmpty()) {
            JOptionPane.showMessageDialog(view, "Заповніть місто та обидві дати, щоб побачити вільні локації.");
            return;
        }

        try {
            LocalDate start = LocalDate.parse(startStr);
            LocalDate end = LocalDate.parse(endStr);

            Tournament tempTour = new Tournament();

            //звернення до БД для пошуку вільних локацій
            availableLocationsMap = tempTour.getLocations(city, start, end, LocationsDAO.getInstance());

            view.getLocationComboBox().removeAllItems();

            //збереження поточної локації в списку, якщо в режимі редагування турніру (щоб вона не зникла)
            if (tournamentToEdit != null && tournamentToEdit.getSelectedLocation() != null) {
                String originalCity = tournamentToEdit.getCity();
                if (city.trim().equalsIgnoreCase(originalCity.trim())) {
                    Location currentLoc = tournamentToEdit.getSelectedLocation();

                    if (availableLocationsMap == null) {
                        availableLocationsMap = new HashMap<>();
                    }

                    if (!availableLocationsMap.containsKey(currentLoc.getId())) {
                        availableLocationsMap.put(currentLoc.getId(), currentLoc.getName());
                    }
                }
            }

            //оновлення випадаючого списку (ComboBox)
            if (availableLocationsMap != null && !availableLocationsMap.isEmpty()) {
                for (String name : availableLocationsMap.values()) {
                    view.getLocationComboBox().addItem(name);
                }
                view.getLocationComboBox().setEnabled(true);

                //встановлення попередньо обраної локації для режиму редагування
                if (tournamentToEdit != null && tournamentToEdit.getSelectedLocation() != null) {
                    String originalCity = tournamentToEdit.getCity();
                    if (city.trim().equalsIgnoreCase(originalCity.trim())) {
                        view.getLocationComboBox().setSelectedItem(tournamentToEdit.getSelectedLocation().getName());
                    }
                }
            } else {
                view.getLocationComboBox().setEnabled(false);
                JOptionPane.showMessageDialog(view, "На жаль, вільних місць у цьому місті на вказані дати немає.");
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(view, "Спочатку введіть коректні дати (YYYY-MM-DD)");
        }
    }

    //перевірка наповненості турніру і, якщо ліміт досягнуто, змінює статус на "Реєстрація закрита".
    public static void checkAndCloseRegistration(int tournamentId) {
        TournamentsDAO dao = TournamentsDAO.getInstance();
        Tournament t = dao.findById(tournamentId).orElse(null);

        if (t != null) {
            int currentPlayers = dao.getRegisteredPlayersCount(tournamentId);
            int currentReferees = dao.getRegisteredRefereesCount(tournamentId);

            if (currentPlayers >= t.getMaxQuantityParticipant() && currentReferees >= t.countReferee()) {
                dao.updateStatus(tournamentId, 2);
            }
        }
    }

    //перевірка наповненісті турніру і, якщо місця звільнилися, повертає статус "Реєстрація відкрита".
    public static void checkAndOpenRegistration(int tournamentId) {
        TournamentsDAO dao = TournamentsDAO.getInstance();
        Tournament t = dao.findById(tournamentId).orElse(null);

        if (t != null && t.getStatus() == TournamentStatus.REGISTRATION_CLOSED) {
            int currentPlayers = dao.getRegisteredPlayersCount(tournamentId);
            int currentReferees = dao.getRegisteredRefereesCount(tournamentId);

            if (currentPlayers < t.getMaxQuantityParticipant() || currentReferees < t.countReferee()) {
                dao.updateStatus(tournamentId, TournamentStatus.REGISTRATION_OPEN.getId());
            }
        }
    }

    //перевірка кількості створених матчів і, якщо їх достатньо, змінює статус на "Заплановано".
    public static void checkAndScheduleTournament(int tournamentId) {
        TournamentsDAO dao = TournamentsDAO.getInstance();
        Tournament t = dao.findById(tournamentId).orElse(null);

        if (t != null) {
            int createdMatches = dao.getCreatedMatchesCount(tournamentId);
            int requiredMatches = t.getMaxQuantityParticipant() / 2;

            if (createdMatches >= requiredMatches) {
                dao.updateStatus(tournamentId, 3);
            }
        }
    }

    //перевірка кількості створених матчів при їх видаленні і відкочує статус до "Реєстрація закрита",
    // якщо матчів стало менше за необхідний мінімум.
    public static void checkAndRevertToClosed (int tournamentId) {
        TournamentsDAO dao = TournamentsDAO.getInstance();
        Tournament t = dao.findById(tournamentId).orElse(null);
        if (t != null) {
            int createdMatches = dao.getCreatedMatchesCount(tournamentId);
            int requiredMatches = t.getMaxQuantityParticipant() / 2;

            if(createdMatches < requiredMatches) {
                dao.updateStatus(tournamentId, 2);
            }
        }
    }
}