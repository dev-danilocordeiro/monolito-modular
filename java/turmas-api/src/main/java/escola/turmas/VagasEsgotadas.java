package escola.turmas;

import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("escola.turmas.vagas-esgotadas")
public record VagasEsgotadas(UUID turmaId, int anoLetivo) {}
