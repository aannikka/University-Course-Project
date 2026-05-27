package app.entities.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Court {
    int id; //айді
    String name; //назва
    Boolean isAvailable; //чи працює
    int locationId; //айді місця проведення

    //вивід об'єкта
    @Override
    public String toString() {
        return this.name;
    }
}
