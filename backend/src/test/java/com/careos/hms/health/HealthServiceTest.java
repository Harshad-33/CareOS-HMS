package com.careos.hms.health;

import com.careos.hms.health.dto.HealthResponse;
import com.careos.hms.health.service.HealthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Test
    void returnsUpWhenDatabaseConnectionIsValid() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        HealthService service = new HealthService(dataSource, "careos-hms-backend", "0.1.0-SNAPSHOT");
        HealthResponse response = service.checkHealth();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.database()).isEqualTo("UP");
        assertThat(response.service()).isEqualTo("careos-hms-backend");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void returnsDownWhenDatabaseThrowsSqlException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed", "08001"));

        HealthService service = new HealthService(dataSource, "careos-hms-backend", "0.1.0-SNAPSHOT");
        HealthResponse response = service.checkHealth();

        assertThat(response.status()).isEqualTo("DOWN");
        assertThat(response.database()).isEqualTo("DOWN");
        assertThat(response.timestamp()).isNotNull();
    }
}
