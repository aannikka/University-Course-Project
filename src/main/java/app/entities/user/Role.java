package app.entities.user;

import lombok.Getter;

@Getter
public enum Role {
    PLANNER(1, "Планувальник"),
    REGISTRAR(2, "Реєстратор"),
    ADMIN(3, "Адміністратор");

    private final int id; //айді
    private final String displayName; //назва для виводу

    //конструктор
    Role(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    //отримати роль за айді
    public static Role fromId(int id) {
        for (Role role : values()) {
            if (role.getId() == id) return role;
        }
        return ADMIN;
    }

    //вивід об'єкту
    @Override
    public String toString() {
        return displayName;
    }
}
