package app.entities.tournament;

import app.DAO.LocationsDAO;
import app.DAO.TournamentsDAO;
import app.entities.location.Location;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Tournament {
    int id; //айді
    String name; //назва
    LocalDate dateStart; //дата початку
    LocalDate dateFinish; //дата закінчення
    String city; //місто
    Location selectedLocation; //місце проведення
    int minRating; //мінімальний рейтинг для участі
    int maxQuantityParticipant; //максимальна кількість гравців
    double prizeFund; //призовий фонд
    String description; //опис
    TournamentStatus status; //статус


    //валідація даних
    public List<String> validate(TournamentsDAO tournamentsDAO) {
        List<String> errors = new ArrayList<>();
        LocalDate today = LocalDate.now();

        if (name == null || name.trim().isEmpty()) {
            errors.add("Назва турніру не може бути порожньою");
        } else if (tournamentsDAO.isExistByName(this.name, this.id)) {
            errors.add("Турнір з такою назвою вже існує");
        }

        if (dateStart == null || dateFinish == null) {
            errors.add("Дати початку та закінчення мають бути вказані");
        } else {
            if (dateStart.isBefore(today)) {
                errors.add("Дата початку повинна бути в майбутньому або сьогодні");
            }
            if (dateFinish.isBefore(dateStart)) {
                errors.add("Дата закінчення повинна бути пізніше дати початку");
            }
        }

        if (selectedLocation == null || selectedLocation.getId() <= 0) {
            errors.add("Будь ласка, оберіть місце проведення");
        }

        if (minRating < 0) {
            errors.add("Мінімальний рейтинг не може бути меншим за 0");
        }

        if (maxQuantityParticipant < 2) {
            errors.add("Мінімальна кількість учасників - 2");
        } else if (maxQuantityParticipant % 2 != 0) {
            errors.add("Кількість учасників повинна бути парною");
        }

        if (prizeFund < 0) {
            errors.add("Призовий фонд не може бути меншим за 0");
        }

        return errors;
    }

    //розрахунок кількості суддів
    public int countReferee () {
        return maxQuantityParticipant/2;
    }

    //отримання локацій
    public Map<Integer, String> getLocations(String city, LocalDate start, LocalDate end, LocationsDAO lc) {
        if (city == null || start == null || end == null) {
            return new LinkedHashMap<>();
        }
        return lc.findAvailableLocations(city, start, end);
    }

    //вивід об'єкту
    @Override
    public String toString() {
        return name;
    }
}
