package controllers.matches;

import app.DAO.CourtsDAO;
import app.DAO.PlayersDAO;
import app.DAO.RefereesDAO;
import app.DAO.ScheduleDAO;
import app.controllers.MatchesController;
import app.controllers.TournamentController;
import app.entities.location.Court;
import app.entities.location.Location;
import app.entities.participant.Player;
import app.entities.participant.Referee;
import app.entities.tournament.Tournament;
import app.entities.user.Role;
import app.entities.user.User;
import app.utils.Session;
import app.views.planner.matches.CreateMatchView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class MatchesControllerTest {

    private CreateMatchView mockView; //заглушка графічного інтерефейсу
    //заглушки DAO
    private ScheduleDAO mockMatchesDao;
    private CourtsDAO mockCourtsDao;
    private PlayersDAO mockPlayersDao;
    private RefereesDAO mockRefereesDao;

    private Tournament mockTournament;
    private MatchesController controller;

    //заглушки для перехоплення діалогових вікон
    private MockedStatic<JOptionPane> mockedJOptionPane;
    private MockedStatic<TournamentController> mockedTournamentControllerStatic;
    private MockedStatic<PlayersDAO> mockedPlayersDaoStatic;
    private MockedStatic<RefereesDAO> mockedRefereesDaoStatic;
    private MockedStatic<CourtsDAO> mockedCourtsDaoStatic;

    //підготовка до всіх тестів
    @BeforeAll
    static void setupSession() {
        //імітація сесії для уникнення NullPointerException
        Role plannerRole = Role.valueOf("PLANNER");
        Session.setCurrentUser(new User(1, "Планувальник", "planLogin", "pass", plannerRole));
    }

    //підготовка середовища перед кожним тестом
    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        mockView = mock(CreateMatchView.class);
        mockMatchesDao = mock(ScheduleDAO.class);
        mockCourtsDao = mock(CourtsDAO.class);
        mockPlayersDao = mock(PlayersDAO.class);
        mockRefereesDao = mock(RefereesDAO.class);

        //налаштовання турніру
        mockTournament = new Tournament();
        mockTournament.setId(1);
        mockTournament.setDateStart(LocalDate.of(2026, 10, 1));
        mockTournament.setDateFinish(LocalDate.of(2026, 10, 30));

        //локація турніру, щоб уникнути NullPointerException!
        Location mockLocation = new Location();
        mockLocation.setId(1);
        mockTournament.setSelectedLocation(mockLocation);

        //вхідні дані
        JTextField dateField = new JTextField("2026-10-10");
        JTextField timeField = new JTextField("12:00");
        when(mockView.getDateField()).thenReturn(dateField);
        when(mockView.getTimeField()).thenReturn(timeField);

        //ініціалізація заглушок для елементів UI
        when(mockView.getCourtComboBox()).thenReturn(mock(JComboBox.class));
        when(mockView.getFirstPlayerComboBox()).thenReturn(mock(JComboBox.class));
        when(mockView.getSecondPlayerComboBox()).thenReturn(mock(JComboBox.class));
        when(mockView.getRefereeComboBox()).thenReturn(mock(JComboBox.class));

        when(mockView.getSaveButton()).thenReturn(mock(JButton.class));
        when(mockView.getCancelButton()).thenReturn(mock(JButton.class));

        //перехоплення статичних викликів JOptionPane
        mockedJOptionPane = mockStatic(JOptionPane.class);
        mockedTournamentControllerStatic = mockStatic(TournamentController.class);

        //підміна статичних викликів getInstance() для DAO
        mockedPlayersDaoStatic = mockStatic(PlayersDAO.class);
        mockedPlayersDaoStatic.when(PlayersDAO::getInstance).thenReturn(mockPlayersDao);

        mockedRefereesDaoStatic = mockStatic(RefereesDAO.class);
        mockedRefereesDaoStatic.when(RefereesDAO::getInstance).thenReturn(mockRefereesDao);

        mockedCourtsDaoStatic = mockStatic(CourtsDAO.class);
        mockedCourtsDaoStatic.when(CourtsDAO::getInstance).thenReturn(mockCourtsDao);

        //ініціалізація контролерів
        controller = new MatchesController(mockView, mockTournament);
        setPrivateField(controller, "matchesDAO", mockMatchesDao);
        setPrivateField(controller, "courtsDAO", mockCourtsDao);
        setPrivateField(controller, "playersDAO", mockPlayersDao);
        setPrivateField(controller, "refereeDAO", mockRefereesDao);
    }

    //очищення ресурсів після кожного тесту
    @AfterEach
    void tearDown() {
        if (mockedJOptionPane != null) mockedJOptionPane.close();
        if (mockedTournamentControllerStatic != null) mockedTournamentControllerStatic.close();
        if (mockedPlayersDaoStatic != null) mockedPlayersDaoStatic.close();
        if (mockedRefereesDaoStatic != null) mockedRefereesDaoStatic.close();
        if (mockedCourtsDaoStatic != null) mockedCourtsDaoStatic.close();
    }

    //налаштування приватних полів
    private void setPrivateField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    @Test
    void testVariant1_UpdateAvailableResources() throws Exception {
        //налаштування об'єктів
        Court mockCourt = new Court();
        Player mockPlayer = new Player();
        Referee mockReferee = new Referee();

        //по 1 вільному ресурсу з DAO
        when(mockCourtsDao.findAvailable(anyInt(), any(), any())).thenReturn(Collections.singletonList(mockCourt));
        when(mockPlayersDao.findFreeByTournamentId(1)).thenReturn(Collections.singletonList(mockPlayer));
        when(mockRefereesDao.findFreeByTournamentId(1)).thenReturn(Collections.singletonList(mockReferee));

        //виклик updateAvailableResources
        Method method = MatchesController.class.getDeclaredMethod("updateAvailableResources");
        method.setAccessible(true);
        method.invoke(controller);

        //перевірка очікуваного результату:
        verify(mockView.getCourtComboBox(), times(1)).removeAllItems();
        verify(mockView.getFirstPlayerComboBox(), times(1)).removeAllItems();

        verify(mockView.getCourtComboBox(), times(1)).addItem(mockCourt);
        verify(mockView.getFirstPlayerComboBox(), times(1)).addItem(mockPlayer);
        verify(mockView.getRefereeComboBox(), times(1)).addItem(mockReferee);

        verify(mockView.getCourtComboBox(), times(1)).setEnabled(true);
        verify(mockView.getFirstPlayerComboBox(), times(1)).setEnabled(true);
        verify(mockView.getRefereeComboBox(), times(1)).setEnabled(true);
    }

    @Test
    void testVariant2_HandleSave_Success() throws Exception {
        //створення двох різних гравців
        Player p1 = new Player(); p1.setId(10);
        Player p2 = new Player(); p2.setId(20);

        when(mockView.getSelectedCourt()).thenReturn(new Court());
        when(mockView.getFirstPlayer()).thenReturn(p1);
        when(mockView.getSecondPlayer()).thenReturn(p2);
        when(mockView.getReferee()).thenReturn(new Referee());

        //виклик handleSave
        Method method = MatchesController.class.getDeclaredMethod("handleSave");
        method.setAccessible(true);
        method.invoke(controller);

        //перевірка збереження матчу
        verify(mockMatchesDao, times(1)).save(any(), eq(1), anyInt());

        //перевірка чи був виклик планувальника
        mockedTournamentControllerStatic.verify(() -> TournamentController.checkAndScheduleTournament(1), times(1));

        //перевірка UI: повідомлення та закриття
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(mockView), eq("Матч успішно створено!")), times(1));
        verify(mockView, times(1)).dispose();
    }
}