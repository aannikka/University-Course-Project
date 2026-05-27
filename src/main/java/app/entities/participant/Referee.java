package app.entities.participant;

import app.entities.tournament.Tournament;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Referee extends Participant {
    Qualification qualification; //кваліфікація
    String tournamentNames; //в яких турнірах бере участь (для подальшого виводу в View)

    //конструктор
    public Referee(int id, String fullName, LocalDate dateBirth, String phoneNumber, Qualification qualification) {
        super(id, fullName, dateBirth, phoneNumber);
        this.qualification = qualification;
    }

    //валідація даних
    public List<String> validate(Tournament tournament, boolean isAlreadyRegistered) {
        List<String> errors = super.validate();

        if (qualification == null) {
            errors.add("Необхідно обрати кваліфікацію судді");
        }

        if (tournament == null) {
            errors.add("Турнір для реєстрації не обрано");
            return errors;
        }

        if (isAlreadyRegistered) {
            errors.add("Цей суддя вже зареєстрований на турнір '" + tournament.getName() + "'");
        }

        return errors;
    }

    //валідація для оновлення
    public List<String> validateForUpdate() {
        List<String> errors = super.validate();

        if (qualification == null) {
            errors.add("Необхідно обрати кваліфікацію судді");
        }

        return errors;
    }

    //вивід об'єкту
    @Override
    public String toString() {
        return this.getFullName();
    }
}

