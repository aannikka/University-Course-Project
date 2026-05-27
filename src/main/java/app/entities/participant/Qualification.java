package app.entities.participant;

import lombok.Getter;

@Getter
public enum Qualification {
    NATIONAL(1,"Національний"),
    WHITE_BADGE(2,"Білий значок"),
    BRONZE_BADGE(3,"Бронзовий значок"),
    SILVER_BADGE(4,"Срібний значок"),
    GOLD_BADGE(5,"Золотий значок");

    private final int id; //айді
    private final String displayName; //назва для виводу

    //конструктор
    Qualification(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    //отримати кваліфікацію за айді
    public static Qualification fromId(int id) {
        for (Qualification q : values()) {
            if (q.getId() == id) return q;
        }
        throw new IllegalArgumentException("Невідомий ID кваліфікації: " + id);
    }

    //вивід об'єкта
    @Override
    public String toString() {
        return displayName;
    }
}
