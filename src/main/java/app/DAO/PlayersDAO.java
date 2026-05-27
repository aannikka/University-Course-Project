package app.DAO;

import app.entities.participant.Player;
import app.utils.DataBaseConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlayersDAO {

    //SQL-запит для збереження гравця
    private static final String SAVE_SQL = """
            INSERT INTO player (full_name, datebirth, phone_number, rating, id_user)
            VALUES (?, ?, ?, ?, ?)
    """;

    //SQL-запит для прив'язки до турніру
    private static final String LINK_TO_TOURNAMENT = """
            INSERT INTO tournament_player (id_tournament, id_player)
            VALUES (?, ?)
    """;

    //SQL-запит для видалення гравця
    private static final String DELETE_SQL = """
            DELETE FROM player
            WHERE id = ?
    """;

    //SQL-запит для отримання списку всіх гравців разом із переліком турнірів, на які вони зареєстровані
    private static final String FIND_ALL_WITH_TOURNAMENTS_SQL = """
    SELECT p.id, p.full_name, p.datebirth, p.phone_number, p.rating,
           STRING_AGG(t.name, ', ') AS tournament_names
    FROM player p
    LEFT JOIN tournament_player tp ON p.id = tp.id_player
    LEFT JOIN tournament t ON tp.id_tournament = t.id
    GROUP BY p.id, p.full_name, p.datebirth, p.phone_number, p.rating
    """;

    //SQL-запит для знаходження гравця за айді
    private static final String FIND_BY_ID_SQL = """
            SELECT * FROM player
            WHERE id = ?
    """;

    //SQL-запит для знаходження айді гравця за номером телефону
    private static final String FIND_ID_BY_PHONE_SQL = """ 
             SELECT id FROM player
             WHERE phone_number = ?
    """;

    //SQL-запит для оновлення гравця
    private static final String UPDATE_SQL = """
            UPDATE player
            SET full_name = ?, datebirth = ?, phone_number = ?, rating = ?
            WHERE id = ?
    """;

    //SQL-запит для перевірки, чи зареєстрований гравець з таким телефоном на конкретний турнір
    private static final String FIND_BY_PHONE_SQL = """
            SELECT 1
            FROM player p
            JOIN tournament_player tp ON p.id = tp.id_player
            WHERE p.phone_number = ? AND tp.id_tournament = ?
            LIMIT 1
    """;

    //SQL-запит для знаходження всіх вільних гравців конкретного турніру (які ще не мають матчів)
    private static final String FIND_FREE_BY_TOURNAMENT_ID = """
            SELECT p.* FROM player p
            JOIN tournament_player tp ON p.id = tp.id_player
            WHERE tp.id_tournament = ? AND tp.id_match IS NULL
    """;

    //SQL-запит для знаходження всіх турнірів гравця
    private static final String FIND_TOURNAMENT_IDS_BY_PLAYER_ID = """
        SELECT id_tournament FROM tournament_player
        WHERE id_player = ?
    """;

    //SQL-запит для знаходження існування реєстрацій гравця на матч
    private static final String IS_PLAYER_ASSIGNED_TO_MATCHES = """
         SELECT 1 FROM tournament_player
         WHERE id_player = ? AND id_match IS NOT NULL
         LIMIT 1
    """;

    //SQL-запит для знаходження мінімального значення рейтингу серед усіх гравців турніру
    private static final String GET_LOWEST_RATING_IN_TOURNAMENT = """
            SELECT MIN(p.rating)
            FROM player p
            JOIN tournament_player tp ON p.id = tp.id_player
            WHERE tp.id_tournament = ?
    """;

    //приватний конструктор
    private PlayersDAO() {}

    //Холдер для реалізації синглтону
    private static class Holder {
        private static final PlayersDAO INSTANCE = new PlayersDAO();
    }

    //отримання синглтону
    public static PlayersDAO getInstance() {
        return PlayersDAO.Holder.INSTANCE;
    }

    //зберегти гравця та зареєструвати на турнір
    public Player saveAndRegisterToTournament(Player player, int tournamentId, int userId) {
        Connection connection = null;
        try {
            connection = DataBaseConnector.getConnection();
            connection.setAutoCommit(false);

            int playerId = -1;

            try (PreparedStatement checkStmt = connection.prepareStatement(FIND_ID_BY_PHONE_SQL)) {
                checkStmt.setString(1, player.getPhoneNumber());
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    playerId = rs.getInt("id");
                }
            }

            if (playerId == -1) {
                try (PreparedStatement statement = connection.prepareStatement(SAVE_SQL, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, player.getFullName());
                    statement.setDate(2, Date.valueOf(player.getDateBirth()));
                    statement.setString(3, player.getPhoneNumber());
                    statement.setDouble(4, player.getRating());
                    statement.setInt(5, userId);
                    statement.executeUpdate();

                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            playerId = generatedKeys.getInt(1);
                        }
                    }
                }
            }

            try (PreparedStatement linkStatement = connection.prepareStatement(LINK_TO_TOURNAMENT)) {
                linkStatement.setInt(1, tournamentId);
                linkStatement.setInt(2, playerId);
                linkStatement.executeUpdate();
            }

            connection.commit();
            player.setId(playerId);
            return player;
        } catch (SQLException e) {
            if (connection != null) {
                try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            throw new RuntimeException("Помилка при реєстрації гравця на турнір: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                try { connection.setAutoCommit(true); connection.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    //видалити гравця за айді
    public boolean deleteById(int id) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, id);
            int rows = statement.executeUpdate();
            return rows>0;
        } catch(SQLException e) {
            throw new RuntimeException("Помилка при видаленні гравця за ID=" + id + ": " + e.getMessage(), e);
        }
    }

    //знайти всіх гравців
    public List<Player> findAll() {
        List<Player> players = new ArrayList<>();
        try (Connection connection = DataBaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_WITH_TOURNAMENTS_SQL)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Player player = new Player(
                        resultSet.getInt("id"),
                        resultSet.getString("full_name"),
                        resultSet.getDate("datebirth").toLocalDate(),
                        resultSet.getString("phone_number"),
                        resultSet.getDouble("rating")
                );
                player.setTournamentNames(resultSet.getString("tournament_names"));
                players.add(player);
            }
            return players;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отриманні списку всіх гравців: " + e.getMessage(), e);
        }
    }

    //знайти гравця за айді
    public Optional<Player> findById(int id) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                return Optional.of(new Player(
                        resultSet.getInt("id"),
                        resultSet.getString("full_name"),
                        resultSet.getDate("datebirth").toLocalDate(),
                        resultSet.getString("phone_number"),
                        resultSet.getDouble("rating")
                ));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при пошуку гравця за ID=" + id + ": " + e.getMessage(), e);
        }
    }

    //знайти вільних гравців
    public List<Player> findFreeByTournamentId(int tournamentId) {
        List<Player> players = new ArrayList<>();
        try (Connection connection = DataBaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_FREE_BY_TOURNAMENT_ID)) {
            statement.setInt(1, tournamentId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                players.add(new Player(
                        resultSet.getInt("id"),
                        resultSet.getString("full_name"),
                        resultSet.getDate("datebirth").toLocalDate(),
                        resultSet.getString("phone_number"),
                        resultSet.getDouble("rating")
                ));
            }
            return players;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка отримання вільних гравців: " + e.getMessage(), e);
        }
    }

    //оновлення гравця
    public boolean update (Player player) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, player.getFullName());
            statement.setDate(2, Date.valueOf(player.getDateBirth()));
            statement.setString(3, player.getPhoneNumber());
            statement.setDouble(4, player.getRating());
            statement.setInt(5, player.getId());
            int rows = statement.executeUpdate();
            return rows>0;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при оновленні даних гравця ID=" + player.getId() + ": " + e.getMessage(), e);
        }
    }

    //знайти турніри на які гравець зареєстрований
    public List<Integer> findTournamentIdsByPlayerId(int playerId) {
        List<Integer> ids = new ArrayList<>();
        try (Connection conn = DataBaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_TOURNAMENT_IDS_BY_PLAYER_ID)) {
            stmt.setInt(1, playerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) ids.add(rs.getInt("id_tournament"));
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при пошуку турнірів гравця: " + e.getMessage(), e);
        }
        return ids;
    }

    //перевірка наявності матчів у гравця
    public boolean isPlayerAssignedToMatches(int playerId) {
        try (Connection conn = DataBaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(IS_PLAYER_ASSIGNED_TO_MATCHES)) {
            stmt.setInt(1, playerId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при перевірці наявності матчів у гравця: " + e.getMessage(), e);
        }
    }

    //чи існує гравець з таким номером телефону
    public boolean isExistByPhone (String phoneNumber, int tournamentId) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_BY_PHONE_SQL)) {
            statement.setString(1, phoneNumber);
            statement.setInt(2, tournamentId);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка: запит не виконано." + e.getMessage(), e);
        }
    }

    //отримати найнижчий рейтинг гравця у турнірі
    public Double getLowestRatingInTournament(int tournamentId) {
        try (java.sql.Connection connection = DataBaseConnector.getConnection();
             java.sql.PreparedStatement statement = connection.prepareStatement(GET_LOWEST_RATING_IN_TOURNAMENT)) {
            statement.setInt(1, tournamentId);
            java.sql.ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                double minRating = resultSet.getDouble(1);
                return resultSet.wasNull() ? Double.MAX_VALUE : minRating;
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Помилка при пошуку мінімального значення рейтингу у турнірі: " + e.getMessage(), e);
        }
        return Double.MAX_VALUE;
    }
}
