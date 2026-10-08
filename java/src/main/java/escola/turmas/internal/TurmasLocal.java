package escola.turmas.internal;

import escola.turmas.TurmaNaoEncontrada;
import escola.turmas.TurmaSemVaga;
import escola.turmas.TurmasApi;
import escola.turmas.VagasEsgotadas;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "escola.turmas.modo", havingValue = "local", matchIfMissing = true)
class TurmasLocal implements TurmasApi {

    private final TurmaRepository turmas;
    private final ApplicationEventPublisher eventos;

    TurmasLocal(TurmaRepository turmas, ApplicationEventPublisher eventos) {
        this.turmas = turmas;
        this.eventos = eventos;
    }

    @Override
    @Transactional
    public void reservarVaga(UUID turmaId) {
        Turma turma = turmas.ocuparVaga(turmaId)
            .orElseThrow(() -> turmas.existe(turmaId) ? new TurmaSemVaga(turmaId) : new TurmaNaoEncontrada(turmaId));
        if (turma.lotada()) {
            eventos.publishEvent(new VagasEsgotadas(turma.id(), turma.anoLetivo()));
        }
    }

    @Override
    @Transactional
    public void liberarVaga(UUID turmaId) {
        if (!turmas.desocuparVaga(turmaId) && !turmas.existe(turmaId)) {
            throw new TurmaNaoEncontrada(turmaId);
        }
    }
}
