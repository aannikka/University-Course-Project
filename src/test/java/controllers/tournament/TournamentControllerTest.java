package controllers.tournament;

import app.DAO.LocationsDAO;
import app.DAO.TournamentsDAO;
import app.controllers.TournamentController;
import app.entities.tournament.Tournament;
import app.entities.user.Role;
import app.entities.user.User;
import app.utils.Session;
import app.views.planner.tournaments.CreateTournamentView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TournamentControllerTest {

    private CreateTournamentView mockView; //заглушка графічного інтерефейсу
    private TournamentsDAO mockTournamentsDao; //заглушка DAO
    private LocationsDAO mockLocationsDao; //заглушка DAO
    //заглушки для перехоплення діалогових вікон
    private MockedStatic<JOptionPane> mockedJOptionPane;
    private MockedStatic<TournamentsDAO> mockedTournamentsDaoStatic;
    private MockedStatic<LocationsDAO> mockedLocationsDaoStatic;

    //підготовка до всіх тестів
    @BeforeAll
    static void setupSession() {
        //імітація сесії для уникнення NullPointerException
        Role plannerRole = Role.valueOf("PLANNER");
        Session.setCurrentUser(new User(1, "Тест", "testLogin", "pass", plannerRole));
    }

    //підготовка середовища перед кожним тестом
    @BeforeEach
    void setUp() {
        mockView = mock(CreateTournamentView.class);
        mockTournamentsDao = mock(TournamentsDAO.class);
        mockLocationsDao = mock(LocationsDAO.class);

        //ініціалізація заглушок для елементів UI
        when(mockView.getCityField()).thenReturn(new JTextField());
        when(mockView.getSaveButton()).thenReturn(new JButton());
        when(mockView.getCancelButton()).thenReturn(new JButton());

        JComboBox mockComboBox = mock(JComboBox.class);
        when(mockView.getLocationComboBox()).thenReturn(mockComboBox);

        when(mockView.getCity()).thenReturn("");
        when(mockView.getStartDate()).thenReturn("");
        when(mockView.getEndDate()).thenReturn("");

        // перехоплення статичних викликів JOptionPane
        mockedJOptionPane = mockStatic(JOptionPane.class);

        //підміна статичних викликів getInstance() для DAO
        mockedTournamentsDaoStatic = mockStatic(TournamentsDAO.class);
        mockedTournamentsDaoStatic.when(TournamentsDAO::getInstance).thenReturn(mockTournamentsDao);

        mockedLocationsDaoStatic = mockStatic(LocationsDAO.class);
        mockedLocationsDaoStatic.when(LocationsDAO::getInstance).thenReturn(mockLocationsDao);
    }

    //очищення ресурсів після кожного тесту
    @AfterEach
    void tearDown() {
        if (mockedJOptionPane != null) mockedJOptionPane.close();
        if (mockedTournamentsDaoStatic != null) mockedTournamentsDaoStatic.close();
        if (mockedLocationsDaoStatic != null) mockedLocationsDaoStatic.close();
    }

    @Test
    void testVariant1_Initialization() {
        //1.1 Створення (null)
        TournamentController controllerCreate = new TournamentController(mockView, null);
        verify(mockView, never()).fillFields(any());

        //1.2 Редагування (існуючий турнір)
        Tournament existingTournament = new Tournament();
        TournamentController controllerEdit = new TournamentController(mockView, existingTournament);

        //очікуємо виклик fillFields для заповнення форми
        verify(mockView, times(1)).fillFields(existingTournament);
    }

    @Test
    void testVariant2_LoadLocations_EmptyDates() throws Exception {
        //імітація порожніх дат
        when(mockView.getCity()).thenReturn("Київ");
        when(mockView.getStartDate()).thenReturn("");
        when(mockView.getEndDate()).thenReturn("");

        TournamentController controller = new TournamentController(mockView, null);

        //виклик приватного методу loadLocations через рефлексію
        Method method = TournamentController.class.getDeclaredMethod("loadLocations");
        method.setAccessible(true);
        method.invoke(controller);

        //очікуємо, що ComboBox не був розблокований (залишився вимкненим)
        verify(mockView.getLocationComboBox(), never()).setEnabled(true);
    }

    @Test
    void testVariant3_LoadLocations_ValidDates() throws Exception {
        //вхідні дані: місто = "Одеса", валідні дати
        when(mockView.getCity()).thenReturn("Одеса");
        when(mockView.getStartDate()).thenReturn("2026-10-10");
        when(mockView.getEndDate()).thenReturn("2026-10-15");

        TournamentController controller = new TournamentController(mockView, null);

        //виклик приватного методу loadLocations
        Method method = TournamentController.class.getDeclaredMethod("loadLocations");
        method.setAccessible(true);
        method.invoke(controller);

        //перевірка, що вікно з помилкою про порожні дати НЕ викликалося
        //(оскільки дати валідні, програма шукає корти в DAO)
        mockedJOptionPane.verify(
                () -> JOptionPane.showMessageDialog(eq(mockView), eq("Заповніть місто та обидві дати, щоб побачити вільні локації.")),
                never()
        );
    }

    @Test
    void testVariant4_HandleSave_Success() throws Exception {
        //базові валідні значення форми
        when(mockView.getTournamentName()).thenReturn("Тестовий турнір");
        when(mockView.getStartDate()).thenReturn("2026-10-10");
        when(mockView.getEndDate()).thenReturn("2026-10-15");
        when(mockView.getPrizeFund()).thenReturn("5000");
        when(mockView.getCity()).thenReturn("Одеса");
        when(mockView.getMinRating()).thenReturn(0);
        when(mockView.getMaxParticipants()).thenReturn(8);
        when(mockView.getDescription()).thenReturn("Опис");
        when(mockView.getSelectedLocation()).thenReturn("Корт 1");

        TournamentController controller = new TournamentController(mockView, null);

        //мапа локацій
        Map<Integer, String> map = new java.util.LinkedHashMap<>();
        map.put(1, "Корт 1");
        Field field = TournamentController.class.getDeclaredField("availableLocationsMap");
        field.setAccessible(true);
        field.set(controller, map);

        //DAO
        Field daoField = TournamentController.class.getDeclaredField("tournamentsDAO");
        daoField.setAccessible(true);
        daoField.set(controller, mockTournamentsDao);

        //збереження
        Method method = TournamentController.class.getDeclaredMethod("handleSave");
        method.setAccessible(true);
        method.invoke(controller);

        //перевірка, що збереження відбулося і вікно закрилося
        verify(mockTournamentsDao, times(1)).save(any(), anyInt());
        verify(mockView, times(1)).dispose();
    }

    @Test
    void testVariant5_StaticMethods() {
        //налаштування імітації турніру (максимум 10 гравців)
        Tournament mockTour = new Tournament();
        mockTour.setMaxQuantityParticipant(10);

        //імітація, що DAO завжди повертає цей турнір для ID = 1
        when(mockTournamentsDao.findById(1)).thenReturn(Optional.of(mockTour));


        //4.1 Перевірка закриття реєстрації (Відкритий -> Закритий)
        mockTour.setStatus(app.entities.tournament.TournamentStatus.REGISTRATION_OPEN);
        when(mockTournamentsDao.getRegisteredPlayersCount(1)).thenReturn(10); // Ліміт вичерпано
        when(mockTournamentsDao.getRegisteredRefereesCount(1)).thenReturn(5); // Достатньо суддів

        TournamentController.checkAndCloseRegistration(1);

        //перевірка, що статус оновився на "Реєстрація закрита"
        verify(mockTournamentsDao, times(1)).updateStatus(1, 2);

        //4.2 Перевірка відкриття реєстрації (Закритий -> Відкритий)
        mockTour.setStatus(app.entities.tournament.TournamentStatus.REGISTRATION_CLOSED);
        when(mockTournamentsDao.getRegisteredPlayersCount(1)).thenReturn(9); // Хтось виписався (9 < 10)

        TournamentController.checkAndOpenRegistration(1);

        //перевірка, що статус оновився на "Реєстрація відкрита"
        verify(mockTournamentsDao, times(1)).updateStatus(1, app.entities.tournament.TournamentStatus.REGISTRATION_OPEN.getId());


        //4.3 Перевірка переходу до планування (Закритий -> Запланований)
        when(mockTournamentsDao.getCreatedMatchesCount(1)).thenReturn(5);

        TournamentController.checkAndScheduleTournament(1);

        //перевірка, що статус оновився на "Заплановано"
        verify(mockTournamentsDao, times(1)).updateStatus(1, 3);

        //4.4 Перевірка повернення із планування (Запланований -> Закритий)
        when(mockTournamentsDao.getCreatedMatchesCount(1)).thenReturn(4);

        TournamentController.checkAndRevertToClosed(1);

        //перевірка, що статус знову оновився на "Реєстрація закрита"
        verify(mockTournamentsDao, times(2)).updateStatus(1, 2);
    }
}