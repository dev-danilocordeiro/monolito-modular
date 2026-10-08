package escola.turmas.internal;

import java.util.UUID;

public record Reserva(UUID id, UUID turmaId, Situacao situacao) {

    public enum Situacao { RESERVADA, LIBERADA }
}
