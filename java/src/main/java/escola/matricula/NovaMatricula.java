package escola.matricula;

import java.util.UUID;

public record NovaMatricula(UUID estudanteId, UUID turmaId, int anoLetivo, String nomeSocial) {}
