package escola.turmas;

import java.util.UUID;

public interface TurmasApi {

    /**
     * Reserva uma vaga. O {@code reservaId} identifica a tentativa e é a chave de idempotência:
     * repetir a chamada com o mesmo id não ocupa outra vaga.
     */
    void reservarVaga(UUID turmaId, UUID reservaId);

    /** Devolve a vaga da reserva. Repetir não devolve duas vezes; liberar antes de reservar cancela a reserva. */
    void liberarVaga(UUID turmaId, UUID reservaId);
}
