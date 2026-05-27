package app.utils;

import app.entities.user.User;

public class Session {
    private static User currentUser;

    //встановлює поточного користувача (викликається при успішній авторизації)
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    //повертає об'єкт поточного авторизованого користувача
    public static User getCurrentUser() {
        return currentUser;
    }

    //очищає сесію (викликається при виході з акаунта)
    public static void logout() {
        currentUser = null;
    }
}
