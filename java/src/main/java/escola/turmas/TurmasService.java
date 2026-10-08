package escola.turmas;

import escola.turmas.internal.Turma;
import escola.turmas.internal.TurmaRepository;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TurmasService {

    private final TurmaRepository turmas;
    private final ApplicationEventPublisher eventos;

    TurmasService(TurmaRepository turmas, ApplicationEventPublisher eventos) {
        this.turmas = turmas;
        this.eventos = eventos;
    }

    @Transactional
    public void reservarVaga(UUID turmaId) {
        Turma turma = turmas.ocuparVaga(turmaId)
            .orElseThrow(() -> turmas.existe(turmaId) ? new TurmaSemVaga(turmaId) : new TurmaNaoEncontrada(turmaId));
        if (turma.lotada()) {
            eventos.publishEvent(new VagasEsgotadas(turma.id(), turma.anoLetivo()));
        }
    }

    @Transactional
    public void liberarVaga(UUID turmaId) {
        if (!turmas.desocuparVaga(turmaId) && !turmas.existe(turmaId)) {
            throw new TurmaNaoEncontrada(turmaId);
        }
    }
}
