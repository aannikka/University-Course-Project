package controllers.player;

import app.DAO.PlayersDAO;
import app.DAO.TournamentsDAO;
import app.controllers.PlayerController;
import app.controllers.TournamentController;
import app.entities.tournament.Tournament;
import app.entities.user.Role;
import app.entities.user.User;
import app.utils.Session;
import app.views.registrar.players.CreatePlayerView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class PlayerControllerHandleSave {

    private CreatePlayerView mockView; //заглушка графічного інтерефейсу
    //заглушки DAO
    private PlayersDAO mockPlayersDao;
    private TournamentsDAO mockTournamentsDao;
    private PlayerController controller;  //тестований контроллер
    //заглушки для перехоплення діалогових вікон
    private MockedStatic<JOptionPane> mockedJOptionPane;
    private MockedStatic<TournamentsDAO> mockedTournamentsDaoStatic;
    private MockedStatic<TournamentController> mockedTournamentControllerStatic;

    //підготовка до всіх тестів
    @BeforeAll
    static void setupSession() {
        //імітація сесії для уникнення NullPointerException
        Role registrarRole = Role.valueOf("REGISTRAR");
        Session.setCurrentUser(new User(1, "Реєстратор", "regLogin", "pass", registrarRole));
    }

    //підготовка середовища перед кожним тестом
    @BeforeEach
    void setUp() throws Exception {
        mockView = mock(CreatePlayerView.class);
        mockPlayersDao = mock(PlayersDAO.class);
        mockTournamentsDao = mock(TournamentsDAO.class);

        //заглуши дял візуальних компонентів
        when(mockView.getSaveButton()).thenReturn(new JButton());
        when(mockView.getCancelButton()).thenReturn(new JButton());

        JComboBox mockComboBox = mock(JComboBox.class);
        when(mockView.getTournamentComboBox()).thenReturn(mockComboBox);

        //базові валідні значення (щоб уникнути NullPointerException на .trim())
        when(mockView.getFullName()).thenReturn("Іван Іванов");
        when(mockView.getPhoneNumber()).thenReturn("+380991112233");
        when(mockView.getDateBirth()).thenReturn("2000-01-01");
        when(mockView.getRating()).thenReturn("500.0");

        // перехоплення статичних викликів JOptionPane
        mockedJOptionPane = mockStatic(JOptionPane.class);

        mockedTournamentsDaoStatic = mockStatic(TournamentsDAO.class);
        mockedTournamentsDaoStatic.when(TournamentsDAO::getInstance).thenReturn(mockTournamentsDao);
        mockedTournamentControllerStatic = mockStatic(TournamentController.class);

        //ініціалізація контролера (режим створення турніру)
        controller = new PlayerController(mockView, null);
        //підміна DAO через рефлексію
        setPrivateField(controller, "playersDAO", mockPlayersDao);
    }

    //очищення ресурсів після кожного тесту
    @AfterEach
    void tearDown() {
        if (mockedJOptionPane != null) mockedJOptionPane.close();
        if (mockedTournamentsDaoStatic != null) mockedTournamentsDaoStatic.close();
        if (mockedTournamentControllerStatic != null) mockedTournamentControllerStatic.close();
    }

    //метод для доступу до приватних полів
    private void setPrivateField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    //метод для виклику приватного handleSave()
    private void invokeHandleSave() throws Exception {
        Method method = PlayerController.class.getDeclaredMethod("handleSave");
        method.setAccessible(true);
        method.invoke(controller);
    }

    @Test
    void testPath1_EmptyFields() throws Exception {
        //шлях 1: Порожні поля
        when(mockView.getFullName()).thenReturn("");

        invokeHandleSave();

        //переіврка UI: чи показало вікно про пусті поля
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(
                eq(mockView),
                eq("Будь ласка, заповніть усі поля!"),
                eq("Увага"),
                eq(JOptionPane.WARNING_MESSAGE)
        ), times(1));
        verify(mockPlayersDao, never()).saveAndRegisterToTournament(any(), anyInt(), anyInt());
    }

    @Test
    void testPath2_ParseException() throws Exception {
        //шлях 2: Помилка парсингу (невірний формат дати)
        when(mockView.getDateBirth()).thenReturn("01.01.2000");

        invokeHandleSave();

        //перевірка UI: чи показало вікно про помилку формату дати
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(
                eq(mockView),
                eq("Невірний формат дати! Використовуйте YYYY-MM-DD.")
        ), times(1));
        verify(mockPlayersDao, never()).saveAndRegisterToTournament(any(), anyInt(), anyInt());
    }

    @Test
    void testPath3_TournamentFull() throws Exception {
        //шлях 3: заповнений турнір
        Tournament mockTour = new Tournament();
        mockTour.setId(1);
        mockTour.setMaxQuantityParticipant(8);

        when(mockView.getTournamentComboBox().getSelectedItem()).thenReturn(mockTour);
        when(mockTournamentsDao.getRegisteredPlayersCount(1)).thenReturn(8);

        invokeHandleSave();

        //перевірка UI: чи показало вікно про заповнений турнір
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(
                eq(mockView),
                eq("Неможливо зареєструвати: у цьому турнірі закінчилися вільні місця для гравців!"),
                eq("Турнір заповнений"),
                eq(JOptionPane.ERROR_MESSAGE)
        ), times(1));
        verify(mockPlayersDao, never()).saveAndRegisterToTournament(any(), anyInt(), anyInt());
    }

    @Test
    void testPath4_TournamentNull() throws Exception {
        //шлях 4: Турнір не обрано (null) - перевірки лімітів пропускаються
        when(mockView.getTournamentComboBox().getSelectedItem()).thenReturn(null);

        invokeHandleSave();

        //перевірка UI: чи показало вікно "Помилка реєстрації"
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(
                eq(mockView),
                anyString(),
                eq("Помилка реєстрації"),
                eq(JOptionPane.WARNING_MESSAGE)
        ), times(1));
        verify(mockPlayersDao, never()).saveAndRegisterToTournament(any(), anyInt(), anyInt());
    }

    @Test
    void testPath5_DuplicatePhone() throws Exception {
        //шлях 5: Гравець з таким номером вже існує
        Tournament mockTour = new Tournament();
        mockTour.setId(1);
        mockTour.setMaxQuantityParticipant(16);

        when(mockView.getTournamentComboBox().getSelectedItem()).thenReturn(mockTour);
        when(mockTournamentsDao.getRegisteredPlayersCount(1)).thenReturn(5);
        when(mockPlayersDao.isExistByPhone("+380991112233", 1)).thenReturn(true);

        invokeHandleSave();

        //перевірка UI: вікно помилки валідації
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(
                eq(mockView),
                anyString(),
                eq("Помилка реєстрації"),
                eq(JOptionPane.WARNING_MESSAGE)
        ), times(1));
        verify(mockPlayersDao, never()).saveAndRegisterToTournament(any(), anyInt(), anyInt());
    }

    @Test
    void testPath6_BusinessValidationFails() throws Exception {
        //шлях 6: Валідація не пройдена (від'ємний рейтинг)
        Tournament mockTour = new Tournament();
        mockTour.setId(1);
        mockTour.setMaxQuantityParticipant(16);

        when(mockView.getTournamentComboBox().getSelectedItem()).thenReturn(mockTour);
        when(mockTournamentsDao.getRegisteredPlayersCount(1)).thenReturn(5);
        when(mockPlayersDao.isExistByPhone("+380991112233", 1)).thenReturn(false);
        when(mockView.getRating()).thenReturn("-10.0");

        invokeHandleSave();

        //перевірка UI: вікно помилки валідації
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(
                eq(mockView),
                anyString(),
                eq("Помилка реєстрації"),
                eq(JOptionPane.WARNING_MESSAGE)
        ), times(1));
        verify(mockPlayersDao, never()).saveAndRegisterToTournament(any(), anyInt(), anyInt());
    }

    @Test
    void testPath7_Success() throws Exception {
        //шлях 7: Всі дані валідні
        Tournament mockTour = new Tournament();
        mockTour.setId(1);
        mockTour.setMaxQuantityParticipant(16);

        when(mockView.getTournamentComboBox().getSelectedItem()).thenReturn(mockTour);
        when(mockTournamentsDao.getRegisteredPlayersCount(1)).thenReturn(5);
        when(mockPlayersDao.isExistByPhone("+380991112233", 1)).thenReturn(false);

        invokeHandleSave();

        //перевірка UI: чи показало вікно про успішну реєстрацію
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(
                eq(mockView),
                eq("Гравця успішно створено та зареєстровано!")
        ), times(1));

        verify(mockPlayersDao, times(1)).saveAndRegisterToTournament(any(), eq(1), anyInt());
        verify(mockView, times(1)).dispose();
    }
}