package escola.turmas.internal;

import java.util.UUID;

public record Turma(UUID id, int anoLetivo, int capacidade, int ocupadas) {

    public boolean lotada() {
        return ocupadas >= capacidade;
    }
}
