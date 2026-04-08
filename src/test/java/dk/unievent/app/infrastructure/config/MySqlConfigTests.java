package dk.unievent.app.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MySqlConfigTests {

    @Test
    void gettersAndSettersShouldStoreDatasourceProperties() {
        MySqlConfig config = new MySqlConfig();

        config.setUrl("jdbc:mysql://localhost:3306/unievent");
        config.setUsername("unievent_user");
        config.setPassword("super-secret");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        assertEquals("jdbc:mysql://localhost:3306/unievent", config.getUrl());
        assertEquals("unievent_user", config.getUsername());
        assertEquals("super-secret", config.getPassword());
        assertEquals("com.mysql.cj.jdbc.Driver", config.getDriverClassName());
    }
}
