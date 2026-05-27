package app.controllers;

import app.DAO.UsersDAO;
import app.entities.user.User;
import app.utils.Session;
import app.views.admin.AdminMainView;
import app.views.auth.LoginView;
import app.views.planner.PlannerMainView;
import app.views.registrar.RegistrarMainView;

import java.util.Optional;

public class LoginController {

    private LoginView view; //форма авторизації

    //конструктор
    public LoginController(LoginView view) {
        this.view = view;
        initController();
    }

    //ініціалізація слухачів подій для кнопок та полів вводу
    private void initController() {
        view.getLoginButton().addActionListener(e -> handleLogin());
        view.getExitButton().addActionListener(e -> System.exit(0));
    }

    //процес авторизації
    private void handleLogin() {
        //зчитування з полів форми
        String login = view.getLogin();
        String password = view.getPassword();

        //перевірка на порожні поля
        if (login.isEmpty() || password.isEmpty()) {
            view.showError("Будь ласка, введіть логін та пароль!");
            return;
        }

        //пошук чи існує такий користувач
        Optional<User> userOpt = UsersDAO.getInstance().login(login, password);

        //обробка результату авторизації
        if (userOpt.isPresent()) {
            //доступ дозволено: створюємо сесію
            User user = userOpt.get();
            Session.setCurrentUser(user);
            view.showSuccess("Вітаємо, " + user.getFullName() + "! Ваша роль: " + user.getRole().getDisplayName());
            view.dispose();
            openNextScreen();
        } else {
            //доступ заборонено
            view.showError("Невірний логін або пароль!");
        }
    }

    //відкривання необхідного вікна в залежності від ролі користувача
    private void openNextScreen() {
        User currentUser = Session.getCurrentUser();

        int roleId = currentUser.getRole().getId();

        switch (roleId) {
            //якщо планувальник
            case 1:
                PlannerMainView plannerView = new PlannerMainView();
                plannerView.setVisible(true);
                break;
            //якщо реєстратор
            case 2:
                RegistrarMainView registrarView = new RegistrarMainView();
                registrarView.setVisible(true);
                break;
            //якщо адміністратор
            case 3:
                AdminMainView adminView = new AdminMainView();
                adminView.setVisible(true);
                break;

            default:
                view.showError("Невідома роль користувача!");
                break;
        }
    }
}