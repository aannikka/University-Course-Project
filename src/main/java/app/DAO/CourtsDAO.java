package app.DAO;

import app.entities.location.Court;
import app.utils.DataBaseConnector;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CourtsDAO {

    //SQL-запит для збереження корту
    private static final String SAVE_SQL = """
            INSERT INTO court (name, is_available, id_location)
            VALUES (?, ?, ?)
    """;

    //SQL-запит для знаходження корту за айді
    private static final String FIND_BY_ID = """
            SELECT * FROM court WHERE id = ?
    """;

    //SQL-запит для знаходження вільних кортів локації, які не зайняті іншими матчами у вказаний час
    private static final String FIND_AVAILABLE_COURTS_SQL = """
            SELECT * FROM court
            WHERE id_location = ?
            AND is_available = true
            AND id NOT IN (
            SELECT id_court FROM match
            WHERE date = ? AND time = ?
    )
    """;

    //SQL-запит для оновлення корту
    private static final String UPDATE_SQL = """
            UPDATE court
            SET name = ?,  is_available = ?
            WHERE id = ?
    """;

    //приватний конструктор
    private CourtsDAO() {}

    //Холдер для реалізації синглтону
    private static class Holder {
        private static final CourtsDAO INSTANCE = new CourtsDAO();
    }

    //отримання синглтону
    public static CourtsDAO getInstance() {
        return CourtsDAO.Holder.INSTANCE;
    }

    //збереження списку кортів локації
    public void saveAll(List<Court> courts, int locationsId, Connection connection) {
        try(PreparedStatement statement = connection.prepareStatement(SAVE_SQL, Statement.RETURN_GENERATED_KEYS)) {
            for (Court court : courts) {
                statement.setString(1, court.getName());
                statement.setBoolean(2, court.getIsAvailable());
                statement.setInt(3, locationsId);
                statement.addBatch();
                court.setLocationId(locationsId);
            }
            statement.executeBatch();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                int index = 0;
                while (keys.next()) {
                    courts.get(index).setId(keys.getInt(1));
                    index++;
                }
            }
        } catch(SQLException e) {
            throw new RuntimeException("Помилка при збереженні кортів: " + e.getMessage(), e);
        }
    }

    //знайти всі вільні корти у конкретній локації на задану дату та час
    public List<Court> findAvailable(int locationId, LocalDate date, LocalTime time) {
        List<Court> courts = new ArrayList<>();
        try (Connection connection = DataBaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_AVAILABLE_COURTS_SQL)) {

            statement.setInt(1, locationId);
            statement.setDate(2, Date.valueOf(date));
            statement.setTime(3, Time.valueOf(time));

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                courts.add(new Court(
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getBoolean("is_available"),
                        resultSet.getInt("id_location")
                ));
            }
            return courts;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отриманні вільних кортів: " + e.getMessage(), e);
        }
    }

    //знайти корт за айді
    public Optional<Court> findById(int id) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_BY_ID)) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                return Optional.of(new Court(
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getBoolean("is_available"),
                        resultSet.getInt("id_location")
                ));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при пошуку корту за ID=" + id + ": " + e.getMessage(), e);
        }
    }

    //оновлення корту
    public boolean update(Court court) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, court.getName());
            statement.setBoolean(2, court.getIsAvailable());
            statement.setInt(3, court.getId());
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при оновленні даних корту ID=" + court.getId() + ": " + e.getMessage(), e);
        }
    }

    //зберегти корт
    public Court save(Court court) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(SAVE_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, court.getName());
            statement.setBoolean(2, court.getIsAvailable());
            statement.setInt(3, court.getLocationId());

            int affectedRows =  statement.executeUpdate();

            if (affectedRows == 0) {
                throw new RuntimeException("Помилка при збереженні: корт не створено.");
            }

            try(ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if(generatedKeys.next()) {
                    court.setId(generatedKeys.getInt(1));
                }
            }
            return court;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при збереженні корту: " + e.getMessage(), e);
        }
    }
}
