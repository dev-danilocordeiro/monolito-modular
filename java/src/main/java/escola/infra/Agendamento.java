package escola.infra;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Sem isso o @Scheduled do ReprocessaEventos nunca dispara.
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class Agendamento {}
