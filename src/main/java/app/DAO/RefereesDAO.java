package app.DAO;

import app.entities.participant.Qualification;
import app.entities.participant.Referee;
import app.utils.DataBaseConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RefereesDAO {

    //SQL-запит для збереження судді
    private static final String SAVE_SQL = """
            INSERT INTO referee (full_name, datebirth, phone_number, id_qualification, id_user)
            VALUES (?, ?, ?, ?, ?)
    """;

    //SQL-запит для прив'язки з турніром
    private static final String LINK_TO_TOURNAMENT = """
            INSERT INTO tournament_referee (id_tournament, id_referee)
            VALUES (?, ?)
    """;

    //SQL-запит для видалення судді
    private static final String DELETE_SQL = """
            DELETE FROM referee
            WHERE id = ?
    """;

    //SQL-запит для отримання списку всіх суддів разом із переліком турнірів, на які вони зареєстровані
    private static final String FIND_ALL_WITH_TOURNAMENTS_SQL = """
            SELECT r.id, r.full_name, r.datebirth, r.phone_number, r.id_qualification,
            STRING_AGG(t.name, ', ') AS tournament_names
            FROM referee r
            LEFT JOIN tournament_referee tr ON r.id = tr.id_referee
            LEFT JOIN tournament t ON tr.id_tournament = t.id
            GROUP BY r.id, r.full_name, r.datebirth, r.phone_number, r.id_qualification
    """;

    //SQL-запит для знаходження судді за айді
    private static final String FIND_BY_ID_SQL = """
            SELECT * FROM referee
            WHERE id = ?
    """;

    //SQL-запит для знаходження айді судді за номером телефону
    private static final String FIND_ID_BY_PHONE_SQL = """
             SELECT id FROM referee
             WHERE phone_number = ?
    """;

    //SQL-запит для оновлення судді
    private static final String UPDATE_SQL = """
            UPDATE referee
            SET full_name = ?,  datebirth = ?, phone_number = ?, id_qualification = ?
            WHERE id = ?
    """;

    //SQL-запит для перевірки, чи зареєстрований суддя з таким телефоном на конкретний турнір
    private static final String FIND_BY_PHONE_SQL = """
            SELECT 1
            FROM referee r
            JOIN tournament_referee tr ON r.id = tr.id_referee
            WHERE r.phone_number = ? AND tr.id_tournament = ?
            LIMIT 1
    """;

    //SQL-запит для знаходження всіх вільних суддів конкретного турніру (які ще не мають матчів)
    private static final String FIND_FREE_BY_TOURNAMENT_ID = """
            SELECT r.* FROM referee r
            JOIN tournament_referee tr ON r.id = tr.id_referee
            WHERE tr.id_tournament = ? AND tr.id_match IS NULL
    """;

    //SQL-запит для знаходження всіх турнірів судді
    private static final String FIND_TOURNAMENT_IDS_BY_REFEREE_ID = """
            SELECT id_tournament
            FROM tournament_referee
            WHERE id_referee = ?
    """;

    //SQL-запит для знаходження існування реєстрацій судді на матч
    private static final String IS_REFEREE_ASSIGNED_TO_MATCHES = """
            SELECT 1
            FROM tournament_referee
            WHERE id_referee = ? AND id_match IS NOT NULL
            LIMIT 1
    """;

    //приватний конструктор
    private RefereesDAO() {}

    //Холдер для реалізації синглтону
    private static class Holder {
        private static final RefereesDAO INSTANCE = new RefereesDAO();
    }

    //отримання синглтону
    public static RefereesDAO getInstance() {
        return RefereesDAO.Holder.INSTANCE;
    }

    //зберегти суддю та зареєструвати на турнір
    public Referee saveAndRegisterToTournament(Referee referee, int tournamentId, int userId) {
        Connection connection = null;
        try {
            connection = DataBaseConnector.getConnection();
            connection.setAutoCommit(false);

            int refereeId = -1;
            try(PreparedStatement checkStmt = connection.prepareStatement(FIND_ID_BY_PHONE_SQL)) {
                checkStmt.setString(1, referee.getPhoneNumber());
                ResultSet resultSet = checkStmt.executeQuery();
                if(resultSet.next()) {
                    refereeId = resultSet.getInt("id");
                }
            }

            if(refereeId == -1) {
                try (PreparedStatement statement = connection.prepareStatement(SAVE_SQL, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, referee.getFullName());
                    statement.setDate(2, Date.valueOf(referee.getDateBirth()));
                    statement.setString(3, referee.getPhoneNumber());
                    statement.setInt(4, referee.getQualification().getId());
                    statement.setInt(5, userId);
                    statement.executeUpdate();

                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            refereeId = generatedKeys.getInt(1);
                        }
                    }
                }
            }

                 try (PreparedStatement linkStatement = connection.prepareStatement(LINK_TO_TOURNAMENT)) {
                     linkStatement.setInt(1, tournamentId);
                     linkStatement.setInt(2, refereeId);
                     linkStatement.executeUpdate();
                 }
                 connection.commit();
                 referee.setId(refereeId);
                 return referee;
        } catch (SQLException e) {
            if (connection != null) {
                try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            throw new RuntimeException("Помилка при реєстрації судді на турнір: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                try { connection.setAutoCommit(true); connection.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    //видалити суддю за айді
    public boolean deleteById(int id) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, id);
            int rows = statement.executeUpdate();
            return rows>0;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при видаленні судді за ID=" + id + ": " + e.getMessage(), e);
        }
    }

    //знайти всіх суддів
    public List<Referee> findAll() {
        List<Referee> referees = new ArrayList<>();
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_ALL_WITH_TOURNAMENTS_SQL)) {
            ResultSet resultSet = statement.executeQuery();
            while(resultSet.next()) {
                int qualificationId = resultSet.getInt("id_qualification");
                Qualification qualification = Qualification.fromId(qualificationId);
                Referee referee = (new Referee(
                        resultSet.getInt("id"),
                        resultSet.getString("full_name"),
                        resultSet.getDate("datebirth").toLocalDate(),
                        resultSet.getString("phone_number"),
                        qualification
                ));
                referee.setTournamentNames(resultSet.getString("tournament_names"));
                referees.add(referee);
            }
            return referees;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отриманні списку всіх суддів: " + e.getMessage(), e);
        }
    }

    //знайти суддю за айді
    public Optional<Referee> findById(int id) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                Qualification qualification = Qualification.fromId(resultSet.getInt("id_qualification"));

                return Optional.of(new Referee(
                        resultSet.getInt("id"),
                        resultSet.getString("full_name"),
                        resultSet.getDate("datebirth").toLocalDate(),
                        resultSet.getString("phone_number"),
                        qualification
                ));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при пошуку судді за ID=" + id + ": " + e.getMessage(), e);
        }
    }

    //знайти вільних суддів
    public List<Referee> findFreeByTournamentId(int tournamentId) {
        List<Referee> referees = new ArrayList<>();
        try (Connection connection = DataBaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_FREE_BY_TOURNAMENT_ID)) {
            statement.setInt(1, tournamentId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                referees.add(new Referee(
                        resultSet.getInt("id"),
                        resultSet.getString("full_name"),
                        resultSet.getDate("datebirth").toLocalDate(),
                        resultSet.getString("phone_number"),
                        Qualification.fromId(resultSet.getInt("id_qualification"))
                ));
            }
            return referees;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка отримання вільних суддів: " + e.getMessage(), e);
        }
    }

    //знайти турніри на які суддя зареєстрований
    public List<Integer> findTournamentIdsByRefereeId(int refereeId) {
        List<Integer> ids = new ArrayList<>();
        try (Connection conn = DataBaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_TOURNAMENT_IDS_BY_REFEREE_ID)) {
            stmt.setInt(1, refereeId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) ids.add(rs.getInt("id_tournament"));
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при пошуку турнірів судді: " + e.getMessage(), e);
        }
        return ids;
    }

    //оновлення судді
    public boolean update (Referee referee) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, referee.getFullName());
            statement.setDate(2, Date.valueOf(referee.getDateBirth()));
            statement.setString(3, referee.getPhoneNumber());
            statement.setInt(4, referee.getQualification().getId());
            statement.setInt(5, referee.getId());

            int rows = statement.executeUpdate();
            return rows>0;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при оновленні даних судді ID=" + referee.getId() + ": " + e.getMessage(), e);
        }
    }

    //перевірка наявності матчів у судді
    public boolean isRefereeAssignedToMatches(int refereeId) {
        try (Connection conn = DataBaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(IS_REFEREE_ASSIGNED_TO_MATCHES)) {
            stmt.setInt(1, refereeId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при перевірці наявності матчів у судді: " + e.getMessage(), e);
        }
    }

    //чи існує суддя з таким номером телефону
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
}
