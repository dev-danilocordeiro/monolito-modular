package escola.matricula.internal;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class MigracoesMatricula {

    @Bean(initMethod = "migrate")
    Flyway flywayMatricula(DataSource dataSource) {
        return Flyway.configure()
            .dataSource(dataSource)
            .schemas("matricula")
            .locations("classpath:db/matricula")
            .load();
    }
}
