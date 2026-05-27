package app.DAO;

import app.entities.user.Role;
import app.entities.user.User;
import app.utils.DataBaseConnector;
import app.utils.PasswordHasher;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsersDAO {

    //SQL-запит для збереження користувача
    private static final String SAVE_SQL = """
            INSERT INTO users (full_name, login, password, id_role)
            VALUES (?, ?, ?, ?)
    """;

    //SQL-запит для видаленя користувача
    private static final String DELETE_SQL = """
            DELETE FROM users
            WHERE id = ?
    """;

    //SQL-запит для знаходження всіх користувачів
    private static final String FIND_ALL_SQL = """
            SELECT * FROM users
    """;

    //SQL-запит для знаходження користувача за айді
    private static final String FIND_BY_ID_SQL = """
            SELECT * FROM users
            WHERE id = ?
    """;

    //SQL-запит для перевірки чи існує користувач з заданим логіном
    private static final String FIND_BY_LOGIN_SQL = """
            SELECT 1 FROM users
            WHERE login = ?
            LIMIT 1
    """;

    //SQL-запит для оновлення користувача
    private static final String UPDATE_SQL = """
            UPDATE users
            SET full_name = ?, login = ?, password = ?, id_role = ?
            WHERE id = ?
    """;

    //SQL-запит для авторизації користувача
    private static final String LOGIN_SQL = """
            SELECT * FROM users
            WHERE login = ? AND password = ?
    """;

    //приватний конструктор
    private UsersDAO() {}

    //Холдер для реалізації синглтону
    private static class Holder {
        private static final UsersDAO INSTANCE = new UsersDAO();
    }

    //отримання синглтону
    public static UsersDAO getInstance() {
            return Holder.INSTANCE;
    }

    //збереження користувача
    public User save (User user) {
        try(Connection connection = DataBaseConnector.getConnection();
                PreparedStatement statement = connection.prepareStatement(SAVE_SQL, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, user.getFullName());
                statement.setString(2, user.getLogin());
                statement.setString(3, PasswordHasher.hashPassword(user.getPassword()));
                statement.setInt(4, user.getRole().getId());

                int affectedRows = statement.executeUpdate();

                if (affectedRows == 0) {
                    throw new RuntimeException("Помилка при збереженні: користувача не створено.");
                }

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if(generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
                return user;
        } catch (SQLException e)  {
            throw new RuntimeException("Помилка при збереженні користувача: " + e.getMessage(), e);
        }
    }

    //видалити користувача за айді
    public boolean deleteById(int id) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setInt(1, id);
            int rows = statement.executeUpdate();
            return rows>0;
    } catch (SQLException e) {
        throw new RuntimeException("Помилка при видаленні користувача за ID=" + id + ": " + e.getMessage(), e);}
    }

    //знайти всіх користувачів
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL)) {
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            int roleId = resultSet.getInt("id_role");
            Role userRole = Role.fromId(roleId);
            User user = (new User(
                    resultSet.getInt("id"),
                    resultSet.getString("full_name"),
                    resultSet.getString("login"),
                    resultSet.getString("password"),
                    userRole
            ));
            users.add(user);
        }
        return users;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отриманні списку всіх користувачів: " + e.getMessage(), e);
        }
    }

    //знайти користувача за айді
    public Optional<User> findById(int id) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int roleId = resultSet.getInt("id_role");
                Role userRole = Role.fromId(roleId);
                return Optional.of(new User(
                        resultSet.getInt("id"),
                        resultSet.getString("full_name"),
                        resultSet.getString("login"),
                        resultSet.getString("password"),
                        userRole
                ));
            }
        return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при пошуку користувача за ID=" + id + ": " + e.getMessage(), e);
        }
    }

    //оновлення користувача
    public boolean update (User user) {
        try (Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, user.getFullName());
            statement.setString(2, user.getLogin());
            statement.setString(3, user.getPassword());
            statement.setInt(4, user.getRole().getId());
            statement.setInt(5, user.getId());
            int rows = statement.executeUpdate();
            return rows>0;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при оновленні даних користувача ID=" + user.getId() + ": " + e.getMessage(), e);
        }
    }

    //авторизація користувача
    public Optional<User> login (String login, String password) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(LOGIN_SQL)) {
            statement.setString(1, login);
            statement.setString(2, PasswordHasher.hashPassword(password));

            try (ResultSet resultSet = statement.executeQuery()) {
                if(resultSet.next()) {
                    Role userRole =  Role.fromId(resultSet.getInt("id_role"));
                    User user = new User (
                    resultSet.getInt("id"),
                    resultSet.getString("full_name"),
                    resultSet.getString("login"),
                    resultSet.getString("password"),
                    userRole );
                    return Optional.of(user);
                }
             }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка: вхід не дозволено.");
        }
    }

    //перевірка чи існує користувач з заданим логіном
    public boolean isExistsByLogin (String login) {
        try(Connection connection = DataBaseConnector.getConnection();
        PreparedStatement statement = connection.prepareStatement(FIND_BY_LOGIN_SQL)) {
            statement.setString(1, login);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка: запит не виконано." + e.getMessage(), e);
        }
    }
}
