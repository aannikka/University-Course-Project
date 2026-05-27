package app.entities.match;

import app.DAO.ScheduleDAO;
import app.entities.location.Court;
import app.entities.participant.Player;
import app.entities.participant.Referee;
import app.entities.tournament.Tournament;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Match {
    int id; //айді
    int tournamentId; //айді турніру
    Player firstPlayer; //перший гравець
    Player secondPlayer; //другий гравець
    Referee referee; //суддя
    LocalDate date; //дата матчу
    LocalTime startTime; //час початку
    Court selectedCourt; //обраний корт

    //валідація даних
    public List<String> validate(Tournament tournament, ScheduleDAO scheduleDAO) {
        List<String> errors = new ArrayList<>();

        if (firstPlayer == null || secondPlayer == null || referee == null ||
                selectedCourt == null || date == null || startTime == null) {
            errors.add("Усі поля матчу мають бути заповнені");
            return errors;
        }

        if (firstPlayer.getId() == secondPlayer.getId()) {
            errors.add("Обрано одного і того ж гравця двічі");
        }

        if (date.isBefore(tournament.getDateStart()) || date.isAfter(tournament.getDateFinish())) {
            errors.add("Дата матчу має бути в межах турніру: "
                    + tournament.getDateStart() + " - " + tournament.getDateFinish());
        }

        if (date.isBefore(LocalDate.now())) {
            errors.add("Не можна планувати матч на минулу дату");
        }

        if (scheduleDAO.isPlayerBusy(firstPlayer.getId(), date, startTime, this.id)) {
            errors.add("Гравець " + firstPlayer.getFullName() + " вже зайнятий у цей час");
        }

        if (scheduleDAO.isPlayerBusy(secondPlayer.getId(), date, startTime, this.id)) {
            errors.add("Гравець " + secondPlayer.getFullName() + " вже зайнятий у цей час");
        }

        if (scheduleDAO.isRefereeBusy(referee.getId(), date, startTime, this.id)) {
            errors.add("Суддя " + referee.getFullName() + " вже зайнятий у цей час");
        }

        if (scheduleDAO.isCourtBusy(selectedCourt.getId(), date, startTime, this.id)) {
            errors.add("Корт '" + selectedCourt.getName() + "' вже зайнятий у цей час");
        }
        return errors;
    }
}
