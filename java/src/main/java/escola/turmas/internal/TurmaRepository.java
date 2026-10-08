package escola.turmas.internal;

import escola.turmas.internal.Reserva.Situacao;
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

    /**
     * Grava a reserva se o id ainda não existe. Duas chamadas simultâneas com o mesmo id
     * esperam uma pela outra na chave primária, e só uma devolve true.
     */
    public boolean registrar(Reserva reserva) {
        return jdbc.sql("""
                INSERT INTO turmas.reservas (id, turma_id, situacao) VALUES (:id, :turmaId, :situacao)
                ON CONFLICT (id) DO NOTHING
                """)
            .param("id", reserva.id())
            .param("turmaId", reserva.turmaId())
            .param("situacao", reserva.situacao().name())
            .update() == 1;
    }

    public boolean marcarLiberada(UUID reservaId) {
        return jdbc.sql("UPDATE turmas.reservas SET situacao = 'LIBERADA' WHERE id = :id AND situacao = 'RESERVADA'")
            .param("id", reservaId)
            .update() == 1;
    }

    public Reserva reserva(UUID reservaId) {
        return jdbc.sql("SELECT id, turma_id, situacao FROM turmas.reservas WHERE id = :id")
            .param("id", reservaId)
            .query(Reserva.class)
            .single();
    }
}
