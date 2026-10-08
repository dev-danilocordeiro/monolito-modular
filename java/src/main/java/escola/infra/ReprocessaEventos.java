package escola.infra;

import java.time.Duration;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class ReprocessaEventos {

    private final IncompleteEventPublications pendentes;

    ReprocessaEventos(IncompleteEventPublications pendentes) {
        this.pendentes = pendentes;
    }

    @Scheduled(fixedDelay = 60_000)
    void reprocessar() {
        pendentes.resubmitIncompletePublicationsOlderThan(Duration.ofMinutes(5));
    }
}
