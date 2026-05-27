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

public class UserControllerHandleSave {

    private CreateUserView mockView; //заглушка графічного інтерефейсу
    private UsersDAO mockUsersDao; //заглушка DAO
    private UserController controller; //тестований контроллер
    //заглушка для перехоплення діалогових вікон
    private MockedStatic<JOptionPane> mockedJOptionPane;
    private MockedStatic<UsersDAO> mockedUsersDaoStatic;

    //підготовка до всіх тестів
    @BeforeAll
    static void setupSession() {
        //імітація сесії для уникнення NullPointerException
        Role adminRole = Role.valueOf("ADMIN");
        Session.setCurrentUser(new User(1, "Адмін", "adminLogin", "pass", adminRole));
    }

    //підготовка середовища перед кожним тестом
    @BeforeEach
    void setUp() throws Exception {
        mockView = mock(CreateUserView.class);
        mockUsersDao = mock(UsersDAO.class);

        //вхідні дані
        when(mockView.getFullName()).thenReturn("Іванов Іван");
        when(mockView.getLogin()).thenReturn("ivanov");
        when(mockView.getPassword()).thenReturn("pass123");
        when(mockView.getSelectedRole()).thenReturn(Role.valueOf("ADMIN"));

        //заглушки для візуальних компонентів
        when(mockView.getSaveButton()).thenReturn(new JButton());
        when(mockView.getCancelButton()).thenReturn(new JButton());

        //перехоплення статичних викликів JOptionPane
        mockedJOptionPane = mockStatic(JOptionPane.class);
        mockedUsersDaoStatic = mockStatic(UsersDAO.class);
        mockedUsersDaoStatic.when(UsersDAO::getInstance).thenReturn(mockUsersDao);

        //ініціалізація контролера (режим створення турніру)
        controller = new UserController(mockView);
        //підміна DAO через рефлексію
        setPrivateField(controller, "usersDAO", mockUsersDao);
    }

    //очищення ресурсів після кожного тесту
    @AfterEach
    void tearDown() {
        if (mockedJOptionPane != null) mockedJOptionPane.close();
        if (mockedUsersDaoStatic != null) mockedUsersDaoStatic.close();
    }

    //метод для доступу до приватних полів
    private void setPrivateField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    //метод для виклику приватного handleSave()
    private void invokeHandleSave(UserController targetController) throws Exception {
        Method method = UserController.class.getDeclaredMethod("handleSave");
        method.setAccessible(true);
        method.invoke(targetController);
    }

    @Test
    void testPath1_ValidationFails() throws Exception {
        //шлях 1: Порожні поля (порожній логін)
        when(mockView.getLogin()).thenReturn("");

        invokeHandleSave(controller);

        //перевірка UI: чи показало вікно про пусті поля
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(
                eq(mockView),
                anyString(),
                eq("Помилка валідації"),
                eq(JOptionPane.WARNING_MESSAGE)
        ), times(1));
        verify(mockUsersDao, never()).save(any());
    }

    @Test
    void testPath2_SystemException() throws Exception {
        //шлях 2: помилка БД
        doThrow(new RuntimeException("Connection lost")).when(mockUsersDao).save(any());

        invokeHandleSave(controller);

        //перевірка UI: чи показало вікно про помилку
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(
                eq(mockView),
                eq("Помилка БД: Connection lost"),
                eq("Помилка"),
                eq(JOptionPane.ERROR_MESSAGE)
        ), times(1));

        //форма залишається відкритою
        verify(mockView, never()).dispose();
    }

    @Test
    void testPath3_EditModeSkipped() throws Exception {
        //шлях 3: Об'єкт userToEdit штучно передано як не null (імітація режиму редагування)
        User existingUser = new User();
        existingUser.setId(5);
        existingUser.setLogin("oldLogin");

        setPrivateField(controller, "userToEdit", existingUser);

        invokeHandleSave(controller);

        //перевірка, що save() для створення нового користувача НЕ викликався
        verify(mockUsersDao, never()).save(any());
        //форма коректно закривається
        verify(mockView, times(1)).dispose();
    }

    @Test
    void testPath4_Success() throws Exception {
        //шлях 4: всі дані валідні
        invokeHandleSave(controller);

        //перевірка виклику збереження
        verify(mockUsersDao, times(1)).save(any(User.class));

        //перевірка UI: чи показало вікно про успішну реєстрацію
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(
                eq(mockView),
                eq("Користувача успішно додано!"),
                eq("Успіх"),
                eq(JOptionPane.INFORMATION_MESSAGE)
        ), times(1));

        //форма успішно закривається
        verify(mockView, times(1)).dispose();
    }
}