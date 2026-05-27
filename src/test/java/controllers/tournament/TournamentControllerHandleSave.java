package controllers.tournament;

import app.DAO.TournamentsDAO;
import app.controllers.TournamentController;
import app.entities.user.Role;
import app.entities.user.User;
import app.utils.Session;
import app.views.planner.tournaments.CreateTournamentView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import org.mockito.MockedStatic;
import org.junit.jupiter.api.AfterEach;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public class TournamentControllerHandleSave {
    private CreateTournamentView mockView; //заглушка графічного інтерефейсу
    private TournamentsDAO mockDao; //заглушка DAO
    private TournamentController controller; //тестований контроллер
    private MockedStatic<JOptionPane> mockedJOptionPane; //заглушка для перехоплення діалогових вікон

    //підготовка до всіх тестів
    @BeforeAll
    static void setupSession() {
        //імітація сесії для уникнення NullPointerException
        Role plannerRole = Role.valueOf("PLANNER");
        Session.setCurrentUser(new User(1, "Тест", "testLogin", "pass", plannerRole));
    }

    //підготовка середовища перед кожним тестом
    @BeforeEach
    void setUp() throws Exception {
        mockView = mock(CreateTournamentView.class);
        mockDao = mock(TournamentsDAO.class);

        //заглушки для візуальних компонентів
        when(mockView.getCityField()).thenReturn(new JTextField());
        when(mockView.getSaveButton()).thenReturn(new JButton());
        when(mockView.getCancelButton()).thenReturn(new JButton());

        //базові валідні значення (щоб уникнути NullPointerException на .trim())
        when(mockView.getTournamentName()).thenReturn("Тестовий турнір");
        when(mockView.getStartDate()).thenReturn("2026-10-10");
        when(mockView.getEndDate()).thenReturn("2026-10-15");
        when(mockView.getPrizeFund()).thenReturn("5000");
        when(mockView.getCity()).thenReturn("Одеса");
        when(mockView.getMinRating()).thenReturn(0);
        when(mockView.getMaxParticipants()).thenReturn(8);
        when(mockView.getDescription()).thenReturn("Опис");
        when(mockView.getSelectedLocation()).thenReturn("Корт 1");

        //ініціалізація контролера (режим створення турніру)
        controller = new TournamentController(mockView, null);

        //підміна DAO через рефлексію
        setPrivateField(controller, "tournamentsDAO", mockDao);
        // перехоплення статичних викликів JOptionPane
        mockedJOptionPane = mockStatic(JOptionPane.class);
    }

    //очищення ресурсів після кожного тесту
    @AfterEach
    void tearDown() {
        if (mockedJOptionPane != null) {
            mockedJOptionPane.close();
        }
    }

    //метод для доступу до приватних полів
    private void setPrivateField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    //метод для виклику приватного handleSave()
    private void invokeHandleSave() throws Exception {
        Method method = TournamentController.class.getDeclaredMethod("handleSave");
        method.setAccessible(true);
        method.invoke(controller);
    }

    @Test
    void testPath1_EmptyFields() throws Exception {
        //шлях 1: Порожні поля (Назва турніру відсутня)
        when(mockView.getTournamentName()).thenReturn("");

        invokeHandleSave();

        //очікуваний результат: Збереження не відбувається
        verify(mockDao, never()).save(any(), anyInt());
    }

    @Test
    void testPath2_ParseException() throws Exception {
        //шлях 2: Помилка парсингу (не числове значення)
        when(mockView.getPrizeFund()).thenReturn("abc");

        invokeHandleSave();

        //очікуваний результат: перехоплення NumberFormatException
        verify(mockDao, never()).save(any(), anyInt());
    }

    @Test
    void testPath3_LocationNotSelected() throws Exception {
        //шлях 3: Локація не обрана (null)
        when(mockView.getSelectedLocation()).thenReturn(null);

        invokeHandleSave();

        // Очікуваний результат: Переривання, локація не обрана
        verify(mockDao, never()).save(any(), anyInt());
    }

    @Test
    void testPath4_LocationNotFoundInMap() throws Exception {
        //шлях 4: локація введена, але відсутня у Map (цикл не знаходить збігів)
        when(mockView.getSelectedLocation()).thenReturn("Корт 1");

        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "Одеса-Арена");
        setPrivateField(controller, "availableLocationsMap", map);

        invokeHandleSave();

        //очікуваний результат: locationId залишається -1, валідація сутності не проходить
        verify(mockDao, never()).save(any(), anyInt());
    }

    @Test
    void testPath5_ValidationFails_TwoIterations() throws Exception {
        //шлях 5: цикл виконує 2 ітерації, але валідація дати не проходить
        when(mockView.getSelectedLocation()).thenReturn("Корт 1");
        when(mockView.getStartDate()).thenReturn("2026-10-15");
        when(mockView.getEndDate()).thenReturn("2026-10-10"); // дата завершення раніше за початок

        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "Одеса-Арена");
        map.put(2, "Корт 1"); //збіг на другій ітерації
        setPrivateField(controller, "availableLocationsMap", map);

        invokeHandleSave();

        //очікуваний результат: локація знайдена, але збереження переривається через validate()
        verify(mockDao, never()).save(any(), anyInt());
    }

    @Test
    void testPath6_ValidationFails_OneIteration() throws Exception {
        //шлях 6: цикл виконує 1 ітерацію, валідація дати не проходить
        when(mockView.getSelectedLocation()).thenReturn("Корт 1");
        when(mockView.getStartDate()).thenReturn("2026-10-15");
        when(mockView.getEndDate()).thenReturn("2026-10-10"); //помилка дати

        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "Корт 1"); // Збіг на першій ітерації
        setPrivateField(controller, "availableLocationsMap", map);

        invokeHandleSave();

        //очікуваний результат: локація знайдена, але переривання через помилку моделі
        verify(mockDao, never()).save(any(), anyInt());
    }

    @Test
    void testPath7_Success() throws Exception {
        //шлях 7: Усі дані валідні, локація знайдена на першій ітерації
        when(mockView.getSelectedLocation()).thenReturn("Корт 1");

        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "Корт 1");
        setPrivateField(controller, "availableLocationsMap", map);

        invokeHandleSave();

        //очікуваний результат: виклик tournamentsDAO.save(...) та закриття вікна
        verify(mockDao, times(1)).save(any(), anyInt());
        verify(mockView, times(1)).dispose();
    }
}
