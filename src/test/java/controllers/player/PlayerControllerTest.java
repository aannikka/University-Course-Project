package controllers.player;

import app.DAO.PlayersDAO;
import app.DAO.TournamentsDAO;
import app.controllers.PlayerController;
import app.controllers.TournamentController;
import app.entities.participant.Player;
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
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PlayerControllerTest {

    private CreatePlayerView mockView; //заглушка графічного інтерефейсу
    private PlayersDAO mockPlayersDao; //заглушка DAO
    private TournamentsDAO mockTournamentsDao;
    private JComboBox<Tournament> mockComboBox;
    //заглушки для перехоплення діалогових вікон
    private MockedStatic<JOptionPane> mockedJOptionPane;
    private MockedStatic<TournamentsDAO> mockedTournamentsDaoStatic;
    private MockedStatic<PlayersDAO> mockedPlayersDaoStatic;
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
    @SuppressWarnings("unchecked")
    void setUp() {
        mockView = mock(CreatePlayerView.class);
        mockPlayersDao = mock(PlayersDAO.class);
        mockTournamentsDao = mock(TournamentsDAO.class);

        //ініціалізація заглушок для елементів UI
        when(mockView.getSaveButton()).thenReturn(mock(JButton.class));
        when(mockView.getCancelButton()).thenReturn(mock(JButton.class));

        //cтворення Mock для ComboBox, щоб стежити за його методами
        mockComboBox = mock(JComboBox.class);
        when(mockView.getTournamentComboBox()).thenReturn(mockComboBox);

        //вхідні дані
        when(mockView.getFullName()).thenReturn("Іван Іванов");
        when(mockView.getPhoneNumber()).thenReturn("+380991112233");
        when(mockView.getDateBirth()).thenReturn("2000-01-01");
        when(mockView.getRating()).thenReturn("500.0");

        // перехоплення статичних викликів JOptionPane
        mockedJOptionPane = mockStatic(JOptionPane.class);

        //підміна статичних викликів getInstance() для DAO
        mockedTournamentsDaoStatic = mockStatic(TournamentsDAO.class);
        mockedTournamentsDaoStatic.when(TournamentsDAO::getInstance).thenReturn(mockTournamentsDao);

        mockedPlayersDaoStatic = mockStatic(PlayersDAO.class);
        mockedPlayersDaoStatic.when(PlayersDAO::getInstance).thenReturn(mockPlayersDao);

        mockedTournamentControllerStatic = mockStatic(TournamentController.class);
    }

    //очищення ресурсів після кожного тесту
    @AfterEach
    void tearDown() {
        if (mockedJOptionPane != null) mockedJOptionPane.close();
        if (mockedTournamentsDaoStatic != null) mockedTournamentsDaoStatic.close();
        if (mockedPlayersDaoStatic != null) mockedPlayersDaoStatic.close();
        if (mockedTournamentControllerStatic != null) mockedTournamentControllerStatic.close();
    }

    //метод для виклику приватного handleSave()
    private void invokeHandleSave(PlayerController controller) throws Exception {
        Method method = PlayerController.class.getDeclaredMethod("handleSave");
        method.setAccessible(true);
        method.invoke(controller);
    }

    @Test
    void testVariant1_Initialization_LoadTournaments() {
        //повертаємо два об'єкти турнірів T1 та T2
        Tournament t1 = new Tournament();
        t1.setId(1);
        Tournament t2 = new Tournament();
        t2.setId(2);
        when(mockTournamentsDao.findAll()).thenReturn(Arrays.asList(t1, t2));

        //ініціалізацію контролеру
        new PlayerController(mockView);

        //перевірка очікуваного результату:
        verify(mockComboBox, times(1)).removeAllItems();
        verify(mockComboBox, times(1)).addItem(t1);
        verify(mockComboBox, times(1)).addItem(t2);
    }

    @Test
    void testVariant2_HandleSave_Success() throws Exception {
        //налаштування умов
        Tournament mockTour = new Tournament();
        mockTour.setId(1);
        mockTour.setMaxQuantityParticipant(16);

        when(mockComboBox.getSelectedItem()).thenReturn(mockTour);
        when(mockTournamentsDao.getRegisteredPlayersCount(1)).thenReturn(5);
        when(mockPlayersDao.isExistByPhone("+380991112233", 1)).thenReturn(false);

        PlayerController controller = new PlayerController(mockView);
        invokeHandleSave(controller);

        // Перевірка очікуваного результату:
        //1. Запис і реєстрація в БД для ID=1
        verify(mockPlayersDao, times(1)).saveAndRegisterToTournament(any(Player.class), eq(1), anyInt());

        //2. Виклик статусів для перевірки заповненості турніру
        mockedTournamentControllerStatic.verify(() -> TournamentController.checkAndCloseRegistration(1), times(1));

        //3. Візуальне підтвердження користувачу через JOptionPane
        mockedJOptionPane.verify(() -> JOptionPane.showMessageDialog(
                eq(mockView),
                eq("Гравця успішно створено та зареєстровано!")
        ), times(1));

        //4. Закриття форми
        verify(mockView, times(1)).dispose();
    }
}