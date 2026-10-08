package escola.matricula;

import java.util.UUID;

public record MatriculaConfirmada(UUID estudanteId, UUID turmaId, int anoLetivo, String nomeSocial) {}
