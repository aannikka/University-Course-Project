package controllers.locations;

import app.DAO.CourtsDAO;
import app.DAO.LocationsDAO;
import app.controllers.LocationController;
import app.entities.location.Court;
import app.entities.location.Location;
import app.entities.user.Role;
import app.entities.user.User;
import app.utils.Session;
import app.views.admin.locations.CreateLocationView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LocationControllerHandleSave {

    private CreateLocationView mockView; //заглушка графічного інтерефейсу
    //заглушки DAO
    private LocationsDAO mockLocationsDao;
    private CourtsDAO mockCourtsDao;
    private LocationController controller; //тестований контроллер

    //заглушки для перехоплення діалогових вікон
    private MockedStatic<JOptionPane> mockedJOptionPane;
    private MockedStatic<LocationsDAO> mockedLocationsDaoStatic;
    private MockedStatic<CourtsDAO> mockedCourtsDaoStatic; // Додано для ізоляції статики

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
        mockView = mock(CreateLocationView.class);
        mockLocationsDao = mock(LocationsDAO.class);
        mockCourtsDao = mock(CourtsDAO.class);

        when(mockView.getName()).thenReturn("Одеса-Арена");
        when(mockView.getCity()).thenReturn("Одеса");
        when(mockView.getAddress()).thenReturn("вул. Спортивна, 1");
        when(mockView.isLocationAvailable()).thenReturn(true);

        //валідний корт з назвою
        List<Court> courtsList = new ArrayList<>();
        Court mockCourt = new Court();
        mockCourt.setId(0);
        mockCourt.setName("Корт №1");
        courtsList.add(mockCourt);
        when(mockView.getCourts()).thenReturn(courtsList);

        //заглушки для візуальних компонентів
        when(mockView.getSaveButton()).thenReturn(new JButton());
        when(mockView.getCancelButton()).thenReturn(new JButton());

        // перехоплення статичних викликів JOptionPane
        mockedJOptionPane = mockStatic(JOptionPane.class);

        mockedLocationsDaoStatic = mockStatic(LocationsDAO.class);
        mockedLocationsDaoStatic.when(LocationsDAO::getInstance).thenReturn(mockLocationsDao);

        mockedCourtsDaoStatic = mockStatic(CourtsDAO.class);
        mockedCourtsDaoStatic.when(CourtsDAO::getInstance).thenReturn(mockCourtsDao);

        //ініціалізація контролера (режим створення місця проведення)
        controller = new LocationController(mockView);
        //підміна DAO через рефлексію
        setPrivateField(controller, "locationsDAO", mockLocationsDao);
    }

    //очищення ресурсів після кожного тесту
    @AfterEach
    void tearDown() {
        if (mockedJOptionPane != null) mockedJOptionPane.close();
        if (mockedLocationsDaoStatic != null) mockedLocationsDaoStatic.close();
        if (mockedCourtsDaoStatic != null) mockedCourtsDaoStatic.close(); // Закриваємо обов'язково
    }

    //метод для доступу до приватних полів
    private void setPrivateField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    //метод для виклику приватного handleSave()
    private void invokeHandleSave(LocationController targetController) throws Exception {
        Method method = LocationController.class.getDeclaredMethod("handleSave");
        method.setAccessible(true);
        method.invoke(targetController);
    }

    @Test
    void testPath1_ValidationFails() throws Exception {
        //шлях 1: Порожні поля (Назва локації відсутня)
        when(mockView.getName()).thenReturn("");

        invokeHandleSave(controller);

        //очікуваний результат: збереження не відбувається, викликається помилка валідації
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(mockView), anyString()), times(1));
        verify(mockLocationsDao, never()).save(any());
    }

    @Test
    void testPath2_SystemException() throws Exception {
        //шлях 2: помилка БД
        doThrow(new RuntimeException("DB Error")).when(mockLocationsDao).save(any());

        invokeSaveDirectly();

        //перевірка UI: чи показало вікно про помилку, форма залишається відкритою
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(mockView), eq("Помилка: DB Error")), times(1));
        verify(mockView, never()).dispose();
    }

    @Test
    void testPath3_EditModeSkipped() throws Exception {
        //шлях 3: Об'єкт locationToEdit штучно передано як не null (імітація режиму редагування)
        Location existingLocation = new Location();
        existingLocation.setId(5);
        existingLocation.setName("Одеса-Арена");
        existingLocation.setCity("Одеса");
        existingLocation.setAddress("вул. Спортивна, 1");

        setPrivateField(controller, "locationToEdit", existingLocation);

        invokeSaveDirectly();

        //перевірка, що save() для створення нової локації НЕ викликався
        verify(mockLocationsDao, never()).save(any());
        //перевірка, що викликався метод оновлення в базі
        verify(mockLocationsDao, times(1)).update(any());
        //форма коректно закривається
        verify(mockView, times(1)).dispose();
    }

    @Test
    void testPath4_Success() throws Exception {
        //шлях 4: всі дані валідні
        invokeSaveDirectly();

        //перевірка UI: чи показало вікно про успішну реєстрацію
        verify(mockLocationsDao, times(1)).save(any(Location.class));
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(mockView), eq("Локацію створено!")), times(1));
        verify(mockView, times(1)).dispose();
    }

    //допоміжний метод для читабельності виклику handleSave
    private void invokeSaveDirectly() throws Exception {
        invokeHandleSave(controller);
    }
}