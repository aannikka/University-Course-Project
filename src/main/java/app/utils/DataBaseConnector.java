package app.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DataBaseConnector {

    private static final HikariDataSource dataSource;

    //статичний блок ініціалізації: виконується один раз при завантаженні класу
    static {
        try {
            Properties properties = new Properties();

            //завантаження налаштувань БД з файлу properties
            try (InputStream input = DataBaseConnector.class.getClassLoader()
                    .getResourceAsStream("db.properties")) {

                if (input == null) {
                    throw new RuntimeException("db.properties not found");
                }

                properties.load(input);
            }

            //налаштування пулу з'єднань HikariCP
            HikariConfig config = new HikariConfig();

            //основні параметри підключення
            config.setJdbcUrl(properties.getProperty("db.url"));
            config.setUsername(properties.getProperty("db.user"));
            config.setPassword(properties.getProperty("db.password"));

            //налаштування пулу
            config.setMaximumPoolSize(10); //максимальна кількість з'єднань у пулі
            config.setMinimumIdle(2); //мінімальна кількість вільних з'єднань, які завжди підтримуються
            config.setIdleTimeout(30000); //час (у мс), після якого вільне з'єднання закривається (30 сек)
            config.setConnectionTimeout(20000); //максимальний час очікування вільного з'єднання (20 сек)
            config.setMaxLifetime(1800000); //максимальний час життя з'єднання (30 хв), щоб уникнути витоків пам'яті

            //ініціалізація джерела даних
            dataSource = new HikariDataSource(config);

        } catch (Exception e) {
            throw new RuntimeException("Database pool init error", e);
        }
    }

    //приватний конструктор
    private DataBaseConnector() {}

    //отримання джерело даних (DataSource) пулу HikariCP
    public static HikariDataSource getDataSource() {
        return dataSource;
    }

    //отримання вільного з'єднання з базою даних із пулу
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }
}