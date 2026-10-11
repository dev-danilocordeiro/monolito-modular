package escola.turmas;

import java.util.UUID;

public class TurmaSemVaga extends RuntimeException {

    public TurmaSemVaga(UUID turmaId) {
        super("turma " + turmaId + " sem vaga");
    }
}
