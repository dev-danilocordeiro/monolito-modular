package escola.turmas;

import java.util.UUID;

public interface TurmasApi {
    void reservarVaga(UUID turmaId);
    void liberarVaga(UUID turmaId);
}
