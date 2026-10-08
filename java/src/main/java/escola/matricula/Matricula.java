package escola.matricula;

import java.util.UUID;

public record Matricula(UUID id, UUID estudanteId, UUID turmaId, int anoLetivo) {

    public static Matricula confirmada(NovaMatricula pedido) {
        return new Matricula(UUID.randomUUID(), pedido.estudanteId(), pedido.turmaId(), pedido.anoLetivo());
    }
}
