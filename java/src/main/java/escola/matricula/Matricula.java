package escola.matricula;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public record Matricula(UUID id, UUID estudanteId, UUID turmaId, int anoLetivo) {

    public static Matricula confirmada(NovaMatricula pedido) {
        return new Matricula(idDo(pedido), pedido.estudanteId(), pedido.turmaId(), pedido.anoLetivo());
    }

    /** Derivado de estudante, turma e ano letivo: o mesmo pedido sempre dá o mesmo id. */
    public static UUID idDo(NovaMatricula pedido) {
        var chave = pedido.estudanteId() + ":" + pedido.turmaId() + ":" + pedido.anoLetivo();
        return UUID.nameUUIDFromBytes(chave.getBytes(StandardCharsets.UTF_8));
    }
}
