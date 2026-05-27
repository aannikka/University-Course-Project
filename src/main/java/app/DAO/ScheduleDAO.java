package app.DAO;

import app.entities.match.Match;
import app.utils.DataBaseConnector;


import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ScheduleDAO {

    //SQL-запит для збереження матчу
    private static final String SAVE_SQL = """
            INSERT INTO match (date, time, id_court, id_tournament, id_user)
            VALUES(?, ?, ?, ?, ?)
    """;

    //SQL-запит для оновлення матчу
    private static final String UPDATE_SQL = """
                UPDATE match
                SET date = ?, time = ?,  id_court = ?
                WHERE id = ?
    """;

    //SQL-запит для видалення матчу
    private static final String DELETE_SQL = """
                DELETE FROM match
                WHERE id = ?
    """;

    //SQL-запит для знаходження всіх матчів
    private static final String FIND_ALL_SQL = """
                SELECT * FROM match
    """;

    //SQL-запит для знаходження матчу за айді
    private static final String FIND_BY_ID = """
               SELECT * FROM match
               WHERE id = ?
    """;

    //SQL-запит для знаходження матчів за конкретним турніром
    private static final String FIND_BY_TOURNAMENT_ID = """
                SELECT * FROM match
                WHERE id_tournament = ?
    """;

    //SQL-запит для прив'язки гравця до матчу
    private static final String LINK_PLAYER_SQL = """
                UPDATE tournament_player
                SET id_match = ?
                WHERE id_tournament = ? AND id_player = ?
    """;

    //SQL-запит для прив'язки судді до матчу
    private static final String LINK_REFEREE_SQL = """
                UPDATE tournament_referee
                SET id_match = ?
                WHERE id_tournament = ? AND id_referee = ?
    """;

    //SQL-запит для перевірки зайнятості корту іншим матчем у вказаний час
    private static final String CHECK_COURT_SQL = """
                SELECT 1 FROM match
                WHERE id_court = ? AND date = ? AND time = ? AND id != ?
                LIMIT 1
    """;

    //SQL-запит для перевірки зайнятості гравця
    private static final String CHECK_PLAYER_BUSY_SQL = """
                SELECT 1
                FROM match m
                JOIN tournament_player tp ON m.id = tp.id_match
                WHERE tp.id_player = ? AND m.date = ? AND m.time = ? AND m.id != ?
                LIMIT 1
    """;

    //SQL-запит для перевірки зайнятості судді
    private static final String CHECK_REFEREE_BUSY_SQL = """
                SELECT 1
                FROM match m
                JOIN tournament_referee tr ON m.id = tr.id_match
                WHERE tr.id_referee = ? AND m.date = ? AND m.time = ? AND m.id != ?
                LIMIT 1
    """;

    //SQL-запит для знаходження гравців на матч
    private static final String FIND_PLAYERS_FOR_MATCH = """
                SELECT id_player
                FROM tournament_player
                WHERE id_match = ?
    """;

    //SQL-запит для знаходження судді на матч
    private static final String FIND_REFEREE_FOR_MATCH = """
                SELECT id_referee
                FROM tournament_referee
                WHERE id_match = ?
    """;

    // SQL-запит для відв'язки гравців від матчу (використовується при оновленні та видаленні)
    private static final String UNLINK_PLAYERS = """
            UPDATE tournament_player
            SET id_match = NULL
            WHERE id_match = ?
    """;

    // SQL-запит для відв'язки суддів від матчу (використовується при оновленні та видаленні)
    private static final String UNLINK_REFEREES = """
            UPDATE tournament_referee
            SET id_match = NULL
            WHERE id_match = ?
    """;


    //приватний конструктор
    private ScheduleDAO() {}

    //Холдер для реалізації синглтону
    private static class Holder {
        private static final ScheduleDAO INSTANCE = new ScheduleDAO();
    }

    //отримання синглтону
    public static ScheduleDAO getInstance() {
        return ScheduleDAO.Holder.INSTANCE;
    }

    //збереження матчу з гравцями та суддею
    public Match save(Match match, int tournamentId, int userId) {
        Connection connection = null;
        try {connection = DataBaseConnector.getConnection();
            connection.setAutoCommit(false);
            int matchId;
            try(PreparedStatement statement = connection.prepareStatement(SAVE_SQL, Statement.RETURN_GENERATED_KEYS)) {
                statement.setDate(1, Date.valueOf(match.getDate()));
                statement.setTime(2, Time.valueOf(match.getStartTime()));
                statement.setInt(3, match.getSelectedCourt().getId());
                statement.setInt(4, tournamentId);
                statement.setInt(5, userId);
                statement.executeUpdate();


                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        matchId = generatedKeys.getInt(1);
                        match.setId(matchId);
                    } else throw new SQLException("ID матчу не отримано.");
                }
            }

            try(PreparedStatement linkPlayerStatement = connection.prepareStatement(LINK_PLAYER_SQL)) {
                linkPlayerStatement.setInt(1, matchId);
                linkPlayerStatement.setInt(2, tournamentId);
                linkPlayerStatement.setInt(3, match.getFirstPlayer().getId());
                linkPlayerStatement.executeUpdate();

                linkPlayerStatement.setInt(3, match.getSecondPlayer().getId());
                linkPlayerStatement.executeUpdate();
            }

            try(PreparedStatement linkRefereeStatement = connection.prepareStatement(LINK_REFEREE_SQL)) {
                linkRefereeStatement.setInt(1, matchId);
                linkRefereeStatement.setInt(2, tournamentId);
                linkRefereeStatement.setInt(3, match.getReferee().getId());
                linkRefereeStatement.executeUpdate();
            }

            connection.commit();
            return match;


        } catch (SQLException e) {
            if (connection != null) try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw new RuntimeException("Помилка створення матчу: " + e.getMessage(), e);
        } finally {
            if (connection != null) try { connection.setAutoCommit(true); connection.close(); }
            catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    //видалити матч за айді
    public boolean deleteById(int id) {
        Connection connection = null;
        try {
            connection = DataBaseConnector.getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement unlinkP = connection.prepareStatement(UNLINK_PLAYERS)) {
                unlinkP.setInt(1, id);
                unlinkP.executeUpdate();
            }

            try (PreparedStatement unlinkR = connection.prepareStatement(UNLINK_REFEREES)) {
                unlinkR.setInt(1, id);
                unlinkR.executeUpdate();
            }

            try (PreparedStatement deleteMatch = connection.prepareStatement(DELETE_SQL)) {
                deleteMatch.setInt(1, id);
                int rows = deleteMatch.executeUpdate();

                connection.commit();
                return rows > 0;
            }

        } catch (SQLException e) {
            if (connection != null) {
                try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            throw new RuntimeException("Помилка при видаленні матчу за ID=" + id + ": " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    //оновлення матчу
    public boolean update(Match match) {
        Connection connection = null;
        try {
            connection = DataBaseConnector.getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement up = connection.prepareStatement(UNLINK_PLAYERS);
                 PreparedStatement ur = connection.prepareStatement(UNLINK_REFEREES)) {
                up.setInt(1, match.getId());
                ur.setInt(1, match.getId());
                up.executeUpdate();
                ur.executeUpdate();
            }

            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                statement.setDate(1, Date.valueOf(match.getDate()));
                statement.setTime(2, Time.valueOf(match.getStartTime()));
                statement.setInt(3, match.getSelectedCourt().getId());
                statement.setInt(4, match.getId());
                statement.executeUpdate();
            }

            try (PreparedStatement lp = connection.prepareStatement(LINK_PLAYER_SQL)) {
                lp.setInt(1, match.getId());
                lp.setInt(2, match.getTournamentId());

                lp.setInt(3, match.getFirstPlayer().getId());
                lp.executeUpdate();

                lp.setInt(3, match.getSecondPlayer().getId());
                lp.executeUpdate();
            }

            try (PreparedStatement lr = connection.prepareStatement(LINK_REFEREE_SQL)) {
                lr.setInt(1, match.getId());
                lr.setInt(2, match.getTournamentId());
                lr.setInt(3, match.getReferee().getId());
                lr.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            if (connection != null) try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw new RuntimeException("Помилка оновлення матчу: " + e.getMessage(), e);
        } finally {
            if (connection != null) try { connection.setAutoCommit(true); connection.close(); }
            catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    //знайти матч за айді
    public Optional<Match> findById(int id) {
        try (Connection connection = DataBaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID)) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return Optional.of(mapResultSetToMatch(resultSet, connection));
            } return Optional.empty();
        } catch(SQLException e){
            throw new RuntimeException("Помилка при пошуку матчу за ID=" + id + ": " + e.getMessage(), e);
        }
    }

    //знайти всі матчі
    public List<Match> findAll() {
        List<Match> matches = new ArrayList<>();
        try (Connection connection = DataBaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                matches.add(mapResultSetToMatch(resultSet, connection));
            }
            return matches;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отриманні списку всіх матчів: " + e.getMessage(), e);
        }
    }

    //перевірка зайнятості корту
    public boolean isCourtBusy(int courtId, LocalDate date, LocalTime time, int excludeMatchId) {
        try (Connection connection = DataBaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(CHECK_COURT_SQL)) {
            statement.setInt(1, courtId);
            statement.setDate(2, Date.valueOf(date));
            statement.setTime(3, Time.valueOf(time));
            statement.setInt(4, excludeMatchId);
            return statement.executeQuery().next();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка перевірки корту: " + e.getMessage(), e);
        }
    }

    //знайти всі матчі турніру
    public List<Match> findByTournamentId(int tournamentId) {
        List<Match> matches = new ArrayList<>();
        try (Connection connection = DataBaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_TOURNAMENT_ID)) {
            statement.setInt(1, tournamentId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                matches.add(mapResultSetToMatch(resultSet, connection));
            }
            return matches;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка отримання розкладу: " + e.getMessage(), e);
        }
    }

    //перевірка зайнятості гравця
    public boolean isPlayerBusy(int playerId, LocalDate date, LocalTime time, int excludeMatchId) {
        try (Connection connection = DataBaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(CHECK_PLAYER_BUSY_SQL)) {
            statement.setInt(1, playerId);
            statement.setDate(2, Date.valueOf(date));
            statement.setTime(3, Time.valueOf(time));
            statement.setInt(4, excludeMatchId);
            return statement.executeQuery().next();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка перевірки зайнятості гравця: " + e.getMessage(), e);
        }
    }

    //перевірка зайнятості судді
    public boolean isRefereeBusy(int refereeId, LocalDate date, LocalTime time, int excludeMatchId) {
        try (Connection connection = DataBaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(CHECK_REFEREE_BUSY_SQL)) {
            statement.setInt(1, refereeId);
            statement.setDate(2, Date.valueOf(date));
            statement.setTime(3, Time.valueOf(time));
            statement.setInt(4, excludeMatchId);
            return statement.executeQuery().next();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка перевірки зайнятості судді: " + e.getMessage(), e);
        }
    }

    //перетворення результату з БД у сутність матчу
    private Match mapResultSetToMatch(ResultSet rs, Connection connection) throws SQLException {
        Match match = new Match();

        int matchId = rs.getInt("id");
        match.setId(matchId);
        match.setDate(rs.getDate("date").toLocalDate());
        match.setStartTime(rs.getTime("time").toLocalTime());
        match.setTournamentId(rs.getInt("id_tournament"));

        int courtId = rs.getInt("id_court");
        CourtsDAO.getInstance().findById(courtId).ifPresent(match::setSelectedCourt);

        try (PreparedStatement playerStmt = connection.prepareStatement(FIND_PLAYERS_FOR_MATCH)) {
            playerStmt.setInt(1, matchId);
            ResultSet playerRs = playerStmt.executeQuery();
            if (playerRs.next()) {
                PlayersDAO.getInstance().findById(playerRs.getInt("id_player")).ifPresent(match::setFirstPlayer);
            }
            if (playerRs.next()) {
                PlayersDAO.getInstance().findById(playerRs.getInt("id_player")).ifPresent(match::setSecondPlayer);
            }
        }

        try (PreparedStatement refStmt = connection.prepareStatement(FIND_REFEREE_FOR_MATCH)) {
            refStmt.setInt(1, matchId);
            ResultSet refRs = refStmt.executeQuery();
            if (refRs.next()) {
                RefereesDAO.getInstance().findById(refRs.getInt("id_referee")).ifPresent(match::setReferee);
            }
        }

        return match;
    }
}

