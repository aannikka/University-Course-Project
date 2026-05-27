package controllers.locations;

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

public class LocationControllerTest {

    private CreateLocationView mockView; //заглушка графічного інтерефейсу
    private LocationsDAO mockLocationsDao; //заглушка DAO
    private LocationController controller;
    //заглушки для перехоплення діалогових вікон
    private MockedStatic<JOptionPane> mockedJOptionPane;
    private MockedStatic<LocationsDAO> mockedLocationsDaoStatic;

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

        //вхідні умови
        when(mockView.getName()).thenReturn("Одеса-Арена");
        when(mockView.getCity()).thenReturn("Одеса");
        when(mockView.getAddress()).thenReturn("вул. Спортивна, 1");
        when(mockView.isLocationAvailable()).thenReturn(true);

        //валідний корт
        List<Court> courtsList = new ArrayList<>();
        Court mockCourt = new Court();
        mockCourt.setId(0);
        mockCourt.setName("Корт №1");
        courtsList.add(mockCourt);
        when(mockView.getCourts()).thenReturn(courtsList);

        when(mockView.getSaveButton()).thenReturn(new JButton());
        when(mockView.getCancelButton()).thenReturn(new JButton());

        // перехоплення статичних викликів JOptionPane
        mockedJOptionPane = mockStatic(JOptionPane.class);
        mockedLocationsDaoStatic = mockStatic(LocationsDAO.class);
        mockedLocationsDaoStatic.when(LocationsDAO::getInstance).thenReturn(mockLocationsDao);

        //ініціалізація контролеру (режим створення)
        controller = new LocationController(mockView);

        //підміна DAO
        setPrivateField(controller, "locationsDAO", mockLocationsDao);
    }

    //очищення ресурсів після кожного тесту
    @AfterEach
    void tearDown() {
        if (mockedJOptionPane != null) mockedJOptionPane.close();
        if (mockedLocationsDaoStatic != null) mockedLocationsDaoStatic.close();
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
        Method method = LocationController.class.getDeclaredMethod("handleSave");
        method.setAccessible(true);
        method.invoke(controller);

        //1: інтеграція з базою даних (DAO)
        verify(mockLocationsDao, times(1)).save(any(Location.class));

        //2: взаємодія з користувачем (UI)
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(
                eq(mockView),
                eq("Локацію створено!")
        ), times(1));

        //3: закриття форми
        verify(mockView, times(1)).dispose();
    }
}