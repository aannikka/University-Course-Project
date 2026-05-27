package app.entities.participant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Participant {
    int id; //айді
    String fullName; //ПІБ
    LocalDate dateBirth; //дата народження
    String phoneNumber; //номер телефону

    //валідація даних
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (fullName == null || !fullName.matches("^[a-zA-Zа-яА-ЯіїєґІЇЄҐ'\\s-]+$")) {
            errors.add("ПІБ має містити тільки літери");
        }

        if (phoneNumber == null || !phoneNumber.matches("^\\+?\\d{10,13}$")) {
            errors.add("Невірний формат номера телефону (має бути 10-13 цифр)");
        }

        if (dateBirth == null) {
            errors.add("Дату народження не вказано");
        } else {
            if (dateBirth.isAfter(LocalDate.now())) {
                errors.add("Дата народження не може бути в майбутньому");
            } else {
                int age = Period.between(dateBirth, LocalDate.now()).getYears();
                if (age < 18 || age > 70) {
                    errors.add("Вік учасника має бути від 18 до 70 років (зараз: " + age + ")");
                }
            }
        }
        return errors;
    }
}
