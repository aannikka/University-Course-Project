package controllers.users;

import app.DAO.UsersDAO;
import app.controllers.UserController;
import app.entities.user.Role;
import app.entities.user.User;
import app.utils.Session;
import app.views.admin.users.CreateUserView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserControllerTest {

    private CreateUserView mockView; //заглушка графічного інтерефейсу
    private UsersDAO mockUsersDao; //заглушка DAO
    private UserController controller;
    //заглушки для перехоплення діалогових вікон
    private MockedStatic<JOptionPane> mockedJOptionPane;
    private MockedStatic<UsersDAO> mockedUsersDaoStatic;

    @BeforeAll
    static void setupSession() {
        Role adminRole = Role.valueOf("ADMIN");
        Session.setCurrentUser(new User(1, "Адмін", "adminLogin", "pass", adminRole));
    }

    //підготовка середовища перед кожним тестом
    @BeforeEach
    void setUp() throws Exception {
        mockView = mock(CreateUserView.class);
        mockUsersDao = mock(UsersDAO.class);

        //вхідні умови
        when(mockView.getFullName()).thenReturn("Іванов Іван");
        when(mockView.getLogin()).thenReturn("ivanov");
        when(mockView.getPassword()).thenReturn("pass123");
        when(mockView.getSelectedRole()).thenReturn(Role.valueOf("ADMIN")); // Використовуємо існуючу роль!

        when(mockView.getSaveButton()).thenReturn(new JButton());
        when(mockView.getCancelButton()).thenReturn(new JButton());

        //перехоплення статичних викликів JOptionPane
        mockedJOptionPane = mockStatic(JOptionPane.class);
        mockedUsersDaoStatic = mockStatic(UsersDAO.class);
        mockedUsersDaoStatic.when(UsersDAO::getInstance).thenReturn(mockUsersDao);

        //ініціалізація контролеру (режим створення)
        controller = new UserController(mockView);

        //підміна DAO
        setPrivateField(controller, "usersDAO", mockUsersDao);
    }

    //очищення ресурсів після кожного тесту
    @AfterEach
    void tearDown() {
        if (mockedJOptionPane != null) mockedJOptionPane.close();
        if (mockedUsersDaoStatic != null) mockedUsersDaoStatic.close();
    }

    //налаштування приватних полів
    private void setPrivateField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    @Test
    void testVariant1_HandleSave_Success() throws Exception {
        //виклик handleSave()
        Method method = UserController.class.getDeclaredMethod("handleSave");
        method.setAccessible(true);
        method.invoke(controller);

        //1: Інтеграція з базою даних (DAO)
        verify(mockUsersDao, times(1)).save(any(User.class));
        verify(mockUsersDao, never()).update(any()); // Переконуємось, що це саме створення

        //2: Взаємодія з користувачем (UI)
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(
                eq(mockView),
                eq("Користувача успішно додано!"),
                eq("Успіх"),
                eq(JOptionPane.INFORMATION_MESSAGE)
        ), times(1));

        //3: Закриття форми
        verify(mockView, times(1)).dispose();
    }
}