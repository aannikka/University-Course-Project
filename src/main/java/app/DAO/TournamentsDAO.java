package app.DAO;

import app.entities.location.Location;
import app.entities.tournament.Tournament;
import app.entities.tournament.TournamentStatus;
import app.utils.DataBaseConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TournamentsDAO {

    //SQL-запит для збереження
    private static final String SAVE_SQL = """
            INSERT INTO tournament (name, date_start, date_finish, city, min_rating,
            max_quantity_participant, prize_fund, description, id_status, id_location, id_user)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;

    //SQL-запит для оновлення
    private static final String UPDATE_SQL = """
            UPDATE tournament
            SET name = ?, date_start = ?, date_finish = ?, city = ?, min_rating = ?,
            max_quantity_participant = ?, prize_fund = ?, description = ?, id_location = ?
            WHERE id = ?
    """;

    //SQL-запит для видалення
    private static final String DELETE_SQL = """
            DELETE FROM tournament
            WHERE id = ?
    """;

    //SQL-запит для знаходження всіх турнірів
    private static final String FIND_ALL_SQL = """
            SELECT * FROM tournament
    """;

    //SQL-запит для знаходження турніра за айді
    private static final String FIND_BY_ID_SQL = """
            SELECT * FROM tournament
            WHERE id = ?
    """;

    //SQL-запит для перевірки існування турніру з такою ж назвою
    private static final String FIND_BY_NAME_SQL = """
            SELECT 1 FROM tournament
            WHERE name = ? and id != ?
            LIMIT 1
    """;

    //SQL-запит для знаходження турніру за назвою
    private static final String FIND_TOURNAMENT_BY_NAME = """
            SELECT * FROM tournament
            WHERE name = ?
    """;

    //SQL-запит для оновлення статусу турніру
    private static final String UPDATE_STATUS = """
            UPDATE tournament
            SET id_status = ?
            WHERE id = ?
    """;

    //SQL-запит для знаходження кількості зареєстрованих гравців на турнір
    private static final String GET_REGISTERED_PLAYERS_COUNT = """
            SELECT COUNT(*) FROM tournament_player
            WHERE id_tournament = ?
    """;

    //SQL-запит для знаходження кількості зареєстрованих суддів на турнір
    private static final String GET_REGISTERED_REFEREES_COUNT = """
            SELECT COUNT(*) FROM tournament_referee
            WHERE id_tournament = ?
    """;

    //SQL-запит для знаходження кількості створених матчів
    private static final String GET_CREATED_MATCHES_COUNT = """
            SELECT COUNT(*) FROM match
            WHERE id_tournament = ?
    """;

    //приватний конструктор
    private TournamentsDAO() {}

    //Холдер для реалізації синглтону
    private static class Holder {
        private static final TournamentsDAO INSTANCE = new TournamentsDAO();
    }

    //отримання синлгтону
    public static TournamentsDAO getInstance() {
            return Holder.INSTANCE;
    }

    //зберегти турнір
    public Tournament save(Tournament  tournament, int userId) {
        try(Connection connection = DataBaseConnector.getConnection();
            PreparedStatement statement = connection.prepareStatement(SAVE_SQL,  Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, tournament.getName());
            statement.setDate(2, Date.valueOf(tournament.getDateStart()));
            statement.setDate(3, Date.valueOf(tournament.getDateFinish()));
            statement.setString(4, tournament.getCity());
            statement.setInt(5, tournament.getMinRating());
            statement.setInt(6, tournament.getMaxQuantityParticipant());
            statement.setDouble(7, tournament.getPrizeFund());
            statement.setString(8, tournament.getDescription());
            statement.setInt(9, tournament.getStatus().getId());
            statement.setInt(10, tournament.getSelectedLocation().getId());
            statement.setInt(11, userId);

            statement.executeUpdate();

            try(ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if(generatedKeys.next()) {
                    tournament.setId(generatedKeys.getInt(1));
                }
            }
            return tournament;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при збереженні турніру: " + e.getMessage(), e);
        }
    }

    //видалити за айді
    public boolean deleteById (int id) {
        try(Connection connection = DataBaseConnector.getConnection();
            PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, id);
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при видаленні турніру за ID=" + id + ": " + e.getMessage(), e);
        }
    }

    //знаходження всіх турнірів
    public List<Tournament> findAll() {
        List<Tournament> tournaments = new ArrayList<>();
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL)) {
            ResultSet resultSet = statement.executeQuery();
            while(resultSet.next()) {
                tournaments.add(mapResultSetToTournament(resultSet));
            } return tournaments;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отриманні списку турнірів: " + e.getMessage(), e);
        }
    }

    //знаходження турніру за айді
    public Optional<Tournament> findById(int id) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
                statement.setInt(1, id);
                ResultSet resultSet = statement.executeQuery();
                if(resultSet.next()) {
                    return Optional.of(mapResultSetToTournament(resultSet));
                }
                return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при пошуку турніру за ID=" + id + ": " + e.getMessage(), e);
        }
    }

    //знаходження турніру за назвою
    public Optional<Tournament> findByName(String name) {
        try (Connection connection = DataBaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_TOURNAMENT_BY_NAME)) {
            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return Optional.of(mapResultSetToTournament(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при пошуку турніру за назвою: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    //оновленя турніру
    public boolean update(Tournament  tournament) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, tournament.getName());
            statement.setDate(2, Date.valueOf(tournament.getDateStart()));
            statement.setDate(3, Date.valueOf(tournament.getDateFinish()));
            statement.setString(4, tournament.getCity());
            statement.setInt(5,tournament.getMinRating());
            statement.setInt(6, tournament.getMaxQuantityParticipant());
            statement.setDouble(7, tournament.getPrizeFund());
            statement.setString(8, tournament.getDescription());
            statement.setInt(9, tournament.getSelectedLocation().getId());
            statement.setInt(10, tournament.getId());

            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при оновленні даних турніру ID=" + tournament.getId() + ": " + e.getMessage(), e);
        }
    }

    //перевірка, чи існує інший турнір з такою ж назвою
    public boolean isExistByName(String name, int excludeId) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_BY_NAME_SQL)) {
            statement.setString(1, name);
            statement.setInt(2, excludeId);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка: запит не виконано." + e.getMessage(), e);
        }
    }

    //оновити статус турніру
    public boolean updateStatus(int tournamentId, int statusId) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS)) {
            statement.setInt(1, statusId);
            statement.setInt(2, tournamentId);
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при оновленні статусу турніру: " + e.getMessage(), e);
        }
    }

    //отримати кількість зареєстрованих гравців
    public int getRegisteredPlayersCount(int tournamentId) {
        try (Connection connection = DataBaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_REGISTERED_PLAYERS_COUNT)) {
            statement.setInt(1, tournamentId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отримані кількості зареєстрованих гравців: " + e.getMessage(), e);
        }
        return 0;
    }

    //отримати кількість зареєстрованих суддів
    public int getRegisteredRefereesCount(int tournamentId) {
        try (Connection connection  = DataBaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_REGISTERED_REFEREES_COUNT)) {
            statement.setInt(1, tournamentId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) return resultSet.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отримані кількості зареєстрованих суддів: " + e.getMessage(), e);
        }
        return 0;
    }

    //отримати кількість створених матчів
    public int getCreatedMatchesCount(int tournamentId) {
        try (Connection conn = DataBaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_CREATED_MATCHES_COUNT)) {
            stmt.setInt(1, tournamentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отримані кількості створених матчів: " + e.getMessage(), e);
        }
        return 0;
    }

    //перетворення результату з БД у сутність турніру
    private Tournament mapResultSetToTournament(ResultSet resultSet) throws SQLException {
        int locationId = resultSet.getInt("id_location");
        Location location = LocationsDAO.getInstance().findById(locationId).orElse(null);

        TournamentStatus status = TournamentStatus.fromId(resultSet.getInt("id_status"));

        return new Tournament(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getDate("date_start").toLocalDate(),
                resultSet.getDate("date_finish").toLocalDate(),
                resultSet.getString("city"),
                location,
                resultSet.getInt("min_rating"),
                resultSet.getInt("max_quantity_participant"),
                resultSet.getDouble("prize_fund"),
                resultSet.getString("description"),
                status
        );
    }
}