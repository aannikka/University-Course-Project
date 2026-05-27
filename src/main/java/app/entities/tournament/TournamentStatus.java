package app.entities.tournament;

import lombok.Getter;

@Getter
public enum TournamentStatus {
    REGISTRATION_OPEN(1, "Реєстрація відкрита"),
    REGISTRATION_CLOSED(2, "Реєстрація закрита"),
    PLANNED(3, "Заплановано");

    private final int id; //айді
    private final String displayName; //назва для виведення

    //конструктор
    TournamentStatus(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    //отримати статус за айді
    public static TournamentStatus fromId(int id) {
        for (TournamentStatus status : values()) {
            if (status.getId() == id) return status;
        }
        return  REGISTRATION_OPEN; //дефолтне значення
    }
}
