package escola.matricula;

import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("escola.matriculas.confirmadas")
public record MatriculaConfirmada(UUID estudanteId, UUID turmaId, int anoLetivo, String nomeSocial) {}
