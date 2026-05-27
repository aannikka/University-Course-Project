package app.entities.location;

import app.DAO.LocationsDAO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Location {
    int id; //айді
    String name; //назва
    String city; //місто
    String address; //адреса
    Boolean isAvailable; //чи працює
    List<Court> courts; //список наявних кортів

    //валідація даних
    public List<String> validate(LocationsDAO locationsDAO) {
        List<String> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add("Назва локації обов'язкова");
        } else if (id == 0 && locationsDAO.isExistsByName(name)) {
            errors.add("Локація з такою назвою вже існує");
        }

        if (city == null || city.trim().isEmpty()) {
            errors.add("Місто не вказано");
        }
        if (address == null || address.trim().isEmpty()) {
            errors.add("Адреса обов'язкова");
        }

        if (courts == null || courts.isEmpty()) {
            errors.add("Необхідно додати хоча б один корт");
        } else {
            for (int i = 0; i < courts.size(); i++) {
                Court court = courts.get(i);
                if (court.getName() == null || court.getName().trim().isEmpty()) {
                    errors.add("Корт №" + (i + 1) + " повинен мати назву");
                }
            }
        }
        return errors;
    }

    //вивід об'єкту
    @Override
    public String toString() {
        return this.getName();
    }
}
