package escola.turmas.internal;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Cada módulo é dono do seu schema e do seu histórico de migrations.
// Na extração, a pasta db/turmas vai junto com o schema.
@Configuration(proxyBeanMethods = false)
class MigracoesTurmas {

    @Bean(initMethod = "migrate")
    Flyway flywayTurmas(DataSource dataSource) {
        return Flyway.configure()
            .dataSource(dataSource)
            .schemas("turmas")
            .locations("classpath:db/turmas")
            .load();
    }
}
