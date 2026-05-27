package controllers.matches;

import app.DAO.CourtsDAO;
import app.DAO.PlayersDAO;
import app.DAO.RefereesDAO;
import app.DAO.ScheduleDAO;
import app.controllers.MatchesController;
import app.controllers.TournamentController;
import app.entities.location.Court;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class MatchesControllerHandleSave {

    private CreateMatchView mockView; //заглушка графічного інтерефейсу
    private ScheduleDAO mockMatchesDao; //заглушка DAO
    private Tournament mockTournament; //заглушка для контролеру турнірів
    private MatchesController controller; //тестований контроллер

    //заглушки для перехоплення діалогових вікон
    private MockedStatic<JOptionPane> mockedJOptionPane;
    private MockedStatic<TournamentController> mockedTournamentControllerStatic;
    private MockedStatic<PlayersDAO> mockedPlayersDaoStatic;
    private MockedStatic<RefereesDAO> mockedRefereesDaoStatic;
    private MockedStatic<CourtsDAO> mockedCourtsDaoStatic;

    //підготовка до всіх тестів
    @BeforeAll
    static void setupSession() {
        Role plannerRole = Role.valueOf("PLANNER");
        Session.setCurrentUser(new User(1, "Планувальник", "planLogin", "pass", plannerRole));
    }

    //підготовка середовища перед кожним тестом
    @BeforeEach
    void setUp() throws Exception {
        mockView = mock(CreateMatchView.class);
        mockMatchesDao = mock(ScheduleDAO.class);

        mockTournament = new Tournament();
        mockTournament.setId(1);
        mockTournament.setDateStart(LocalDate.of(2026, 10, 1));
        mockTournament.setDateFinish(LocalDate.of(2026, 10, 30));

        //заглушки для візуальних компонентів
        JTextField dateField = new JTextField("2026-10-10");
        JTextField timeField = new JTextField("12:00");
        when(mockView.getDateField()).thenReturn(dateField);
        when(mockView.getTimeField()).thenReturn(timeField);

        when(mockView.getSaveButton()).thenReturn(new JButton());
        when(mockView.getCancelButton()).thenReturn(new JButton());

        Player mockPlayer1 = new Player(); mockPlayer1.setId(1);
        Player mockPlayer2 = new Player(); mockPlayer2.setId(2);
        when(mockView.getSelectedCourt()).thenReturn(new Court());
        when(mockView.getFirstPlayer()).thenReturn(mockPlayer1);
        when(mockView.getSecondPlayer()).thenReturn(mockPlayer2);
        when(mockView.getReferee()).thenReturn(new Referee());

        // перехоплення статичних викликів JOptionPane
        mockedJOptionPane = mockStatic(JOptionPane.class);
        mockedTournamentControllerStatic = mockStatic(TournamentController.class);

        mockedPlayersDaoStatic = mockStatic(PlayersDAO.class);
        mockedPlayersDaoStatic.when(PlayersDAO::getInstance).thenReturn(mock(PlayersDAO.class));

        mockedRefereesDaoStatic = mockStatic(RefereesDAO.class);
        mockedRefereesDaoStatic.when(RefereesDAO::getInstance).thenReturn(mock(RefereesDAO.class));

        mockedCourtsDaoStatic = mockStatic(CourtsDAO.class);
        mockedCourtsDaoStatic.when(CourtsDAO::getInstance).thenReturn(mock(CourtsDAO.class));

        //ініціалізація контролера (режим створення матчу)
        controller = new MatchesController(mockView, mockTournament);
        //підміна DAO через рефлексію
        setPrivateField(controller, "matchesDAO", mockMatchesDao);
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

    //метод для доступу до приватних полів
    private void setPrivateField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    //метод для виклику приватного handleSave()
    private void invokeHandleSave() throws Exception {
        Method method = MatchesController.class.getDeclaredMethod("handleSave");
        method.setAccessible(true);
        method.invoke(controller);
    }

    @Test
    void testPath1_EmptyFields() throws Exception {
        //шлях 1: Порожні поля (дата та час відсутні)
        mockView.getDateField().setText("");

        invokeHandleSave();

        //переіврка UI: чи показало вікно про пусті поля
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(
                eq(mockView),
                eq("Заповніть дату та час!")
        ), times(1));
        verify(mockMatchesDao, never()).save(any(), anyInt(), anyInt());
    }

    @Test
    void testPath2_ParseException() throws Exception {
        //шлях 2: Помилка парсингу (дата некоректно введена)
        mockView.getDateField().setText("2026/10/10");

        invokeHandleSave();

        //перевірка UI: чи показало вікно про помилку формату дати
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(
                eq(mockView),
                eq("Невірний формат дати! Використовуйте YYYY-MM-DD.")
        ), times(1));
        verify(mockMatchesDao, never()).save(any(), anyInt(), anyInt());
    }

    @Test
    void testPath3_BusinessValidationFails() throws Exception {
        //шлях 3: Обрано два однакових гравця
        Player samePlayer = new Player();
        samePlayer.setId(1);
        when(mockView.getFirstPlayer()).thenReturn(samePlayer);
        when(mockView.getSecondPlayer()).thenReturn(samePlayer);

        invokeHandleSave();

        //перевірка UI: вікно помилки валідації
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(
                eq(mockView),
                anyString(),
                eq("Помилка"),
                eq(JOptionPane.WARNING_MESSAGE)
        ), times(1));
        verify(mockMatchesDao, never()).save(any(), anyInt(), anyInt());
    }

    @Test
    void testPath4_Success() throws Exception {
        //шлях 4: Всі дані валідні
        invokeHandleSave();

        //перевірка UI: чи показало вікно про успішне створення матча
        verify(mockMatchesDao, times(1)).save(any(), eq(1), anyInt());
        mockedTournamentControllerStatic.verify(() -> TournamentController.checkAndScheduleTournament(1), times(1));
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(
                eq(mockView),
                eq("Матч успішно створено!")
        ), times(1));
        verify(mockView, times(1)).dispose();
    }
}