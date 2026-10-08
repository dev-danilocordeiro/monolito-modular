package escola.pedagogico.internal;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class MigracoesPedagogico {

    @Bean(initMethod = "migrate")
    Flyway flywayPedagogico(DataSource dataSource) {
        return Flyway.configure()
            .dataSource(dataSource)
            .schemas("pedagogico")
            .locations("classpath:db/pedagogico")
            .load();
    }
}
