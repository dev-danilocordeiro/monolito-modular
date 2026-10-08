package escola.turmas;

import java.util.UUID;

public class TurmaNaoEncontrada extends RuntimeException {

    public TurmaNaoEncontrada(UUID turmaId) {
        super("turma " + turmaId + " não encontrada");
    }
}
