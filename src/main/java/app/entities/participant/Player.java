package app.entities.participant;

import app.entities.tournament.Tournament;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Player extends Participant {
    Double rating; //рейтинг
    String tournamentNames; //в яких турнірах бере участь (для подальшого виводу в View)

    //конструктор
    public Player(int id, String fullName, LocalDate dateBirth, String phoneNumber, Double rating) {
        super(id, fullName, dateBirth, phoneNumber);
        this.rating = rating;
    }

    //валідація даних
    public List<String> validate(Tournament tournament, boolean isAlreadyRegistered) {
        List<String> errors = super.validate();

        if (rating == null || rating < 0) {
            errors.add("Рейтинг гравця має бути додатним числом");
        }

        if (tournament == null) {
            errors.add("Турнір для реєстрації не обрано");
            return errors;
        }

        if (rating < tournament.getMinRating()) {
            errors.add("Рейтинг гравця (" + rating + ") нижчий за мінімальний для цього турніру (" + tournament.getMinRating() + ")");
        }

        if (isAlreadyRegistered) {
            errors.add("Цей гравець вже зареєстрований на турнір '" + tournament.getName() + "'");
        }

        return errors;
    }

    //валідація для оновлення
    public List<String> validateForUpdate(int maxRequiredRating) {
        List<String> errors = super.validate();

        if (rating == null || rating < 0) {
            errors.add("Рейтинг гравця має бути додатним числом");
        } else if (rating < maxRequiredRating) {
            errors.add("Неможливо зберегти: гравець вже бере участь у турнірі, який вимагає мінімальний рейтинг " + maxRequiredRating + "!");
        }
        return errors;
    }

    //вивід об'єкту
    @Override
    public String toString() {
        return this.getFullName();
    }
}
