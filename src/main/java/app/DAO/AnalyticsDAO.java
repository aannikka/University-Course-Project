package app.DAO;

import app.utils.DataBaseConnector;

import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class AnalyticsDAO {

    //SQL-запит для отримання списку гравців конкретного турніру, відсортованих за рейтингом
    private static final String GET_ALL_PLAYERS_SORT_BY_RATING = """
            SELECT p.full_name AS "ПІБ", p.phone_number AS "Номер телефону", p.rating AS "Рейтинг"
            FROM PLAYER p
            JOIN TOURNAMENT_PLAYER tp ON p.id = tp.id_player
            JOIN TOURNAMENT t ON t.id = tp.id_tournament
            WHERE t.name LIKE ?
            ORDER BY p.rating DESC;
    """;

    //SQL-запит для пошуку гравців за початковими літерами ПІБ разом із переліком їхніх турнірів
    private static final String GET_PLAYER_BY_LETTER = """
            SELECT p.full_name AS "ПІБ", p.phone_number AS "Номер телефону",
            COALESCE(STRING_AGG(t.name, ', '), '-') AS "Турніри"
            FROM PLAYER p
            LEFT JOIN TOURNAMENT_PLAYER tp ON p.id = tp.id_player
            LEFT JOIN TOURNAMENT t ON t.id = tp.id_tournament
            WHERE p.full_name LIKE ?
            GROUP BY p.full_name, p.phone_number;
    """;

    //SQL-запит для знаходження турнірів у заданому діапазоні призового фонду
    private static final String GET_TOURNAMENT_BY_PRIZE_FUND_DIAPAZONE = """
            SELECT name AS "Назва", prize_fund AS "Призовий фонд"
            FROM TOURNAMENT
            WHERE prize_fund BETWEEN ? AND ?;
    """;

    //SQL-запит для підрахунку загального призового фонду поточного року
    private static final String GET_ALL_PRIZE_FUND_IN_THIS_YEAR = """
            SELECT SUM(prize_fund) AS "Призовий фонд цього року"
                    FROM TOURNAMENT
                    WHERE EXTRACT(YEAR FROM date_start) = EXTRACT(YEAR FROM CURRENT_DATE) ;
    """;

    //SQL-запит для підрахунку кількості турнірів, загального та середнього призового фонду в розрізі міст
    private static final String GET_COUNT_TOURNAMENTS_AND_SUM_PRIZE_FUNDS_BY_CITY = """
            SELECT city AS "Місто",
            COUNT(*) AS "Кількість турнірів",
            SUM(prize_fund) AS "Загальний призовий фонд",
            AVG(prize_fund) AS "Середній фонд турніру"
            FROM TOURNAMENT
            GROUP BY city
            ORDER BY "Загальний призовий фонд" DESC;
    """;

    //SQL-запит для знаходження гравців з максимальним рейтингом
    private static final String FIND_PLAYER_BY_MAX_RATING = """
           SELECT full_name AS "ПІБ", phone_number AS "Номер телефону", rating AS "Рейтинг"
           FROM PLAYER
           WHERE rating >= ALL (
                SELECT rating
                FROM PLAYER
           );
    """;

    //SQL-запит для знаходження турніру з найбільшим призовим фондом у кожному місті
    private static final String GET_MAX_PRIZE_FUND_TOURNAMENT_BY_CITY = """
            SELECT t1.name AS "Назва", t1.city AS "Місто", t1.prize_fund AS "Призовий фонд"
            FROM TOURNAMENT t1
            WHERE t1.prize_fund = (
                SELECT MAX(t2.prize_fund)
                FROM TOURNAMENT t2
                WHERE t2.city = t1.city
            );
    """;

    //SQL-запит для знаходження місць проведення, які не використовуються
    private static final String GET_UNUSED_LOCATIONS = """
            SELECT l.name AS "Назва локації", l.address AS "Адреса"
            FROM LOCATION l
            LEFT JOIN TOURNAMENT t ON l.id = t.id_location
            WHERE t.id_location IS NULL;
    """;

    //SQL-запит для отримання журналу дій користувачів
    private static final String GET_JOURNAL_USERS_AND_CREATED_OBJECTS = """
            SELECT u.full_name AS "Користувач", 'Створив гравця' AS "Дія", p.full_name AS "Об'єкт"
            FROM users u
            JOIN player p ON u.id = p.id_user
    
            UNION ALL
    
            SELECT u.full_name, 'Створив суддю', r.full_name
            FROM users u
            JOIN referee r ON u.id = r.id_user
    
            UNION ALL
            SELECT u.full_name, 'Створив турнір', t.name
            FROM users u
            JOIN tournament t ON u.id = t.id_user
            ORDER BY "Користувач";
    """;

    //приватний конструктор
    private AnalyticsDAO() {}

    //Холдер для реалізації синглтону
    private static class Holder {
        private static final AnalyticsDAO INSTANCE = new AnalyticsDAO();
    }

    //отримання синглтону
    public static AnalyticsDAO getInstance() {
        return AnalyticsDAO.Holder.INSTANCE;
    }

    //отримання всіх гравців відсортованих за рейтингом
    public DefaultTableModel getPlayersByTournamentReport(String tournamentName) {
        return executeDynamicQuery(GET_ALL_PLAYERS_SORT_BY_RATING, List.of("%" + tournamentName + "%"));
    }

    //отримання гравця за літерою
    public DefaultTableModel getPlayersByLetterReport(String letter) {
        return executeDynamicQuery(GET_PLAYER_BY_LETTER, List.of(letter + "%"));
    }

    //отримання турнірів у діапазоні призового фонду
    public DefaultTableModel getTournamentsByPrizeFundReport (double minFund, double maxFund) {
        return executeDynamicQuery(GET_TOURNAMENT_BY_PRIZE_FUND_DIAPAZONE, List.of(minFund, maxFund));
    }

    //отримання річного призового фонду
    public DefaultTableModel getAnnualPrizePoolReport() {
        return executeDynamicQuery(GET_ALL_PRIZE_FUND_IN_THIS_YEAR, new ArrayList<>());
    }

    //отримання турнірів и суму призового фонду за містом
    public DefaultTableModel getFinanceByCityReport() {
        return executeDynamicQuery(GET_COUNT_TOURNAMENTS_AND_SUM_PRIZE_FUNDS_BY_CITY, new ArrayList<>());
    }

    //отримання гравців з максимальним рейтингом
    public DefaultTableModel getTopPlayerReport() {
        return executeDynamicQuery(FIND_PLAYER_BY_MAX_RATING, new ArrayList<>());
    }

    //отримання максимального призового фонду за містом
    public DefaultTableModel getMaxFundTournamentByCityReport() {
        return executeDynamicQuery(GET_MAX_PRIZE_FUND_TOURNAMENT_BY_CITY, new ArrayList<>());
    }

    //отримання невикористаних місць проведення
    public DefaultTableModel getUnusedLocations() {
        return executeDynamicQuery(GET_UNUSED_LOCATIONS, new ArrayList<>());
    }

    //отримання журналу активності користувачів
    public DefaultTableModel getJournalUsersReport() {
        return executeDynamicQuery(GET_JOURNAL_USERS_AND_CREATED_OBJECTS, new ArrayList<>());
    }

    //створення динамічного звіту
    private DefaultTableModel executeDynamicQuery(String sql, List<Object> params) {
        try (Connection conn = DataBaseConnector.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = pstmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            Vector<String> columnNames = new Vector<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnLabel(i));
            }

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getObject(i));
                }
                data.add(row);
            }

            return new DefaultTableModel(data, columnNames) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
        } catch (SQLException e) {
            throw new RuntimeException("Помилка генерації звіту: " + e.getMessage());
        }
    }
}
