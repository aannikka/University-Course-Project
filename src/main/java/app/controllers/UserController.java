package app.controllers;

import app.DAO.UsersDAO;
import app.entities.user.User;
import app.utils.PasswordHasher;
import app.views.admin.users.CreateUserView;

import javax.swing.*;
import java.util.List;

public class UserController {
    private CreateUserView view; //форма для створення/редагування користувача
    private UsersDAO usersDAO = UsersDAO.getInstance(); //DAO
    private User userToEdit; //користувач для редагування

    //конструктор для режиму створення користувача
    public UserController(CreateUserView view) {
        this.view = view;
        initController();
    }

    //конструктор для режиму редагування користувача
    public UserController(CreateUserView view, User userToEdit) {
        this.view = view;
        this.userToEdit = userToEdit;
        initController();
    }

    //ініціалізація слухачів подій для кнопок та полів вводу
    private void initController() {
        view.getSaveButton().addActionListener(e -> handleSave());
        view.getCancelButton().addActionListener(e -> view.dispose());
    }

    //збереження/оновлення користувача
    private void handleSave() {
        //зчитування та конвертація даних з форми
        User user = (userToEdit == null) ? new User() : userToEdit;

        user.setFullName(view.getFullName());
        user.setLogin(view.getLogin());
        user.setRole(view.getSelectedRole());

        String inputPassword = view.getPassword();

        //перевірка пароля
        boolean isPasswordChanged = false;

        if (userToEdit == null) {
            //створення: не хешуємо пароль
            user.setPassword(inputPassword);
        } else {
            if (inputPassword.isEmpty() || inputPassword.equals(userToEdit.getPassword())) {
                user.setPassword(userToEdit.getPassword()); //старий хеш
            } else {
                user.setPassword(inputPassword); //пароль валідації
                isPasswordChanged = true; // треба захешувати перед БД
            }
        }

        //внутрішня бізнес-валідація сутності
        List<String> errors = user.validate(usersDAO);

        if (errors.isEmpty()) {
            try {
                //збереження в базу даних (створення або оновлення)
                if (userToEdit == null) {
                    //гілка збереження, DAO хешує пароль перед збереженням в БД
                    usersDAO.save(user);
                    JOptionPane.showMessageDialog(view, "Користувача успішно додано!", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    //гілка оновлення: DAO не хешує пароль, якщо його не змінили
                    if (isPasswordChanged) {
                        user.setPassword(PasswordHasher.hashPassword(inputPassword));
                    }
                    usersDAO.update(user);
                    JOptionPane.showMessageDialog(view, "Дані користувача оновлено!", "Успіх", JOptionPane.INFORMATION_MESSAGE);
                }
                view.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(view, "Помилка БД: " + ex.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(view, String.join("\n", errors), "Помилка валідації", JOptionPane.WARNING_MESSAGE);
        }
    }
}