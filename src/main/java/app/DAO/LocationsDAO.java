package app.DAO;

import app.entities.location.Court;
import app.entities.location.Location;
import app.utils.DataBaseConnector;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class LocationsDAO {

    //SQL-запит для збереження місця проведення
    private static final String SAVE_SQL = """
           INSERT INTO location (name, city, address, is_available)
           VALUES (?, ?, ?, ?)
    """;

    //SQL-запит для видалення місця проведення
    private static final String DELETE_SQL = """
            DELETE FROM location
            WHERE id = ?
            """;

    //SQL-запит для знаходження всіх місць проведення зі всіма їх кортами
    private static final String FIND_ALL_WITH_COURTS_SQL = """
          SELECT l.id AS loc_id, l.name AS loc_name, l.city, l.address, l.is_available AS loc_avail,
           c.id AS court_id, c.name AS court_name, c.is_available AS court_avail
           FROM location l
           LEFT JOIN court c ON l.id = c.id_location
    """;

    //SQL-запит знаходження місця проведення за айді зі всіма кортами
    private static final String FIND_BY_ID_WITH_COURTS_SQL = """
           SELECT l.id AS loc_id, l.name AS loc_name, l.city, l.address, l.is_available AS loc_avail,
           c.id AS court_id, c.name AS court_name, c.is_available AS court_avail
           FROM location l
           LEFT JOIN court c ON l.id = c.id_location
           WHERE l.id = ?
    """;

    //SQL-запит для перевірки існування місця проведення за назвою
    private static final String FIND_BY_NAME_SQL = """
            SELECT 1 FROM location
            WHERE name = ?
            LIMIT 1
            """;

    //SQL-запит для знаходження вільних місць проведення у конкретному місті на заданий період дат
    private static final String FIND_AVAILABLE_SQL = """
            SELECT l.id, l.name
                FROM location l
                WHERE l.city = ?
                  AND l.is_available = true
                  AND NOT EXISTS (
                      SELECT 1
                      FROM tournament t
                      WHERE t.id_location = l.id
                        AND ? <= t.date_finish
                        AND ? >= t.date_start
                  )
            ORDER BY l.name;
            """;

    //SQL-запит для оновлення місця проведення
    private static final String UPDATE_SQL = """
            UPDATE location
            SET name = ?, city = ?, address = ?, is_available = ?
            WHERE id = ?
    """;

    //приватний конструктор
    private LocationsDAO() {}

    //Холдер для реалізації синглтону
    private static class Holder {
        private static final LocationsDAO INSTANCE = new LocationsDAO();
    }

    //отримання синглтону
    public static LocationsDAO getInstance() {
        return LocationsDAO.Holder.INSTANCE;
    }

    //збереження місця проведення з кортами
    public Location save(Location location) {
        Connection connection = null;
        try {
            connection = DataBaseConnector.getConnection();

            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement(SAVE_SQL, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, location.getName());
                statement.setString(2, location.getCity());
                statement.setString(3, location.getAddress());
                statement.setBoolean(4, location.getIsAvailable());

                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        location.setId(keys.getInt(1));
                    }
                }
            }

            if (location.getCourts() != null && !location.getCourts().isEmpty()) {
                CourtsDAO.getInstance().saveAll(location.getCourts(), location.getId(), connection);
            }

            connection.commit();
            return location;

        } catch (SQLException e) {
            if (connection != null) {
                try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            throw new RuntimeException("Помилка при збереженні локації та кортів: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    //видалення місця проведення за айді
    public boolean deleteById(int id) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, id);
            int rows = statement.executeUpdate();
            return rows>0;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при видаленні місця проведення за ID=" + id + ": " + e.getMessage(), e);
        }
    }

    //знайти всі місця проведення
    public List<Location> findAll () {
        Map<Integer, Location> locationsMap = new LinkedHashMap<>();
        try (Connection connection = DataBaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_WITH_COURTS_SQL)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int locationId = resultSet.getInt("loc_id");
                Location location = locationsMap.get(locationId);
                if (location == null) {
                    location = new Location(
                            locationId,
                            resultSet.getString("loc_name"),
                            resultSet.getString("city"),
                            resultSet.getString("address"),
                            resultSet.getBoolean("loc_avail"),
                            new ArrayList<>()
                    );
                    locationsMap.put(locationId, location);
                }

                int courtId =  resultSet.getInt("court_id");
                if(!resultSet.wasNull()) {
                    Court court = new Court(
                            courtId,
                            resultSet.getString("court_name"),
                            resultSet.getBoolean("court_avail"),
                            locationId
                    );
                    location.getCourts().add(court);
                }
            }
            return new ArrayList<>(locationsMap.values());

        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отриманні списку всіх місць проведення: " + e.getMessage(), e);
        }
    }

    //знайти місце проведення за айді
    public Optional<Location> findById(int id) {
        Location location = null;
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_WITH_COURTS_SQL)) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                if(location == null) {
                    location = new Location(
                            resultSet.getInt("loc_id"),
                            resultSet.getString("loc_name"),
                            resultSet.getString("city"),
                            resultSet.getString("address"),
                            resultSet.getBoolean("loc_avail"),
                            new ArrayList<>()
                    );
                }
                int courtId = resultSet.getInt("court_id");
                if(!resultSet.wasNull()) {
                    Court court = new Court(
                            courtId,
                            resultSet.getString("court_name"),
                            resultSet.getBoolean("court_avail"),
                            resultSet.getInt("loc_id")
                    );
                    location.getCourts().add(court);
                }
            } return  Optional.ofNullable(location);
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при пошуку місця проведення за ID=" + id + ": " + e.getMessage(), e);
        }
    }

    //оновлення місця проведення
    public boolean update(Location location) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, location.getName());
            statement.setString(2, location.getCity());
            statement.setString(3, location.getAddress());
            statement.setBoolean(4, location.getIsAvailable());
            statement.setInt(5, location.getId());
            int rows = statement.executeUpdate();
            return rows>0;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при оновленні даних місця проведення ID=" + location.getId() + ": " + e.getMessage(), e);
        }
    }

    //перевірка чи існує місце проведення з такою назвою
    public boolean isExistsByName(String name) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_BY_NAME_SQL)) {
            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка: запит не виконано." + e.getMessage(), e);
        }
    }

    //знаходження вільних місць проведення
    public Map<Integer, String> findAvailableLocations (String city, LocalDate start, LocalDate end) {
       Map<Integer, String> locations = new LinkedHashMap<>();
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_AVAILABLE_SQL)) {
            statement.setString(1,city);
            statement.setDate(2, Date.valueOf(start));
            statement.setDate(3, Date.valueOf(end));
            ResultSet resultSet = statement.executeQuery();
            while(resultSet.next()) {
                locations.put(resultSet.getInt("id"),
                               resultSet.getString("name")
                );
            } return  locations;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка: запит не виконано." + e.getMessage(), e);
        }
    }
}
