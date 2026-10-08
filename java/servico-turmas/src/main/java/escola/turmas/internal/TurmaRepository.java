package escola.turmas.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TurmaRepository {

    private final JdbcClient jdbc;

    TurmaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Ocupa uma vaga numa única instrução, sem corrida entre duas reservas simultâneas. */
    public Optional<Turma> ocuparVaga(UUID turmaId) {
        return jdbc.sql("""
                UPDATE turmas.turmas SET ocupadas = ocupadas + 1
                WHERE id = :id AND ocupadas < capacidade
                RETURNING id, ano_letivo, capacidade, ocupadas
                """)
            .param("id", turmaId)
            .query(Turma.class)
            .optional();
    }

    public boolean desocuparVaga(UUID turmaId) {
        return jdbc.sql("UPDATE turmas.turmas SET ocupadas = ocupadas - 1 WHERE id = :id AND ocupadas > 0")
            .param("id", turmaId)
            .update() == 1;
    }

    public boolean existe(UUID turmaId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM turmas.turmas WHERE id = :id)")
            .param("id", turmaId)
            .query(Boolean.class)
            .single();
    }
}
