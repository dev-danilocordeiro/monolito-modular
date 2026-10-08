package escola.turmas.internal;

import escola.turmas.ReservaConflitante;
import escola.turmas.TurmaNaoEncontrada;
import escola.turmas.TurmaSemVaga;
import escola.turmas.TurmasApi;
import escola.turmas.VagasEsgotadas;
import escola.turmas.internal.Reserva.Situacao;
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
    public void reservarVaga(UUID turmaId, UUID reservaId) {
        exigeTurma(turmaId);
        if (!turmas.registrar(new Reserva(reservaId, turmaId, Situacao.RESERVADA))) {
            confereRepeticao(reservaId, turmaId, Situacao.RESERVADA);   // mesma chave: a vaga já foi reservada
            return;
        }
        // Sem vaga, a exceção desfaz a transação e a reserva gravada acima vai junto.
        Turma turma = turmas.ocuparVaga(turmaId).orElseThrow(() -> new TurmaSemVaga(turmaId));
        if (turma.lotada()) {
            eventos.publishEvent(new VagasEsgotadas(turma.id(), turma.anoLetivo()));
        }
    }

    @Override
    @Transactional
    public void liberarVaga(UUID turmaId, UUID reservaId) {
        exigeTurma(turmaId);
        if (turmas.registrar(new Reserva(reservaId, turmaId, Situacao.LIBERADA))) {
            return;   // a reserva nunca chegou: fica registrada como liberada, e se chegar depois é recusada
        }
        if (turmas.marcarLiberada(reservaId)) {
            turmas.desocuparVaga(turmaId);
            return;
        }
        confereRepeticao(reservaId, turmaId, Situacao.LIBERADA);       // mesma chave: a vaga já foi devolvida
    }

    private void exigeTurma(UUID turmaId) {
        if (!turmas.existe(turmaId)) {
            throw new TurmaNaoEncontrada(turmaId);
        }
    }

    private void confereRepeticao(UUID reservaId, UUID turmaId, Situacao esperada) {
        var existente = turmas.reserva(reservaId);
        if (!existente.turmaId().equals(turmaId)) {
            throw new ReservaConflitante(reservaId, "a chave já foi usada para a turma " + existente.turmaId());
        }
        if (existente.situacao() != esperada) {
            throw new ReservaConflitante(reservaId, "a reserva já está " + existente.situacao());
        }
    }
}
