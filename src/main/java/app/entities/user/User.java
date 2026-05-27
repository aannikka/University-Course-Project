package app.entities.user;

import app.DAO.UsersDAO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    int id; //айді
    String fullName; //ПІБ
    String login; //логін
    String password; //пароль
    Role role; //роль в системі

    //валідація даних
    public List<String> validate(UsersDAO usersDAO) {
        List<String> errors = new ArrayList<>();

        if (fullName == null || fullName.trim().isEmpty()) {
            errors.add("ПІБ не може бути порожнім");
        } else if (!fullName.matches("^[a-zA-Zа-яА-ЯіїєґІЇЄҐ'\\s-]+$")) {
            errors.add("ПІБ не може містити цифри або спеціальні символи");
        }

        if (login == null || login.trim().isEmpty()) {
            errors.add("Логін обов'язковий для заповнення");
        } else if (id == 0 && usersDAO.isExistsByLogin(login)) {
            errors.add("Цей логін вже зайнятий");
        }

        if (password == null || password.length() < 5) {
            errors.add("Пароль має бути не менше 5 символів");
        }

        if (role == null) {
            errors.add("Користувачу має бути призначена роль (Планувальник або Реєстратор)");
        }
        return errors;
    }
}
