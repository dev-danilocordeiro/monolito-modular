package escola.turmas;

import java.util.UUID;

/** A chave de idempotência foi reutilizada para outra coisa: outra turma, ou uma reserva já liberada. */
public class ReservaConflitante extends RuntimeException {

    public ReservaConflitante(UUID reservaId, String motivo) {
        super("reserva " + reservaId + ": " + motivo);
    }
}
