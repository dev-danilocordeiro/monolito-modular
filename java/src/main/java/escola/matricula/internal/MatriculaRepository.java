package escola.matricula.internal;

import escola.matricula.Matricula;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MatriculaRepository {

    private final JdbcClient jdbc;

    MatriculaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Grava a matrícula; devolve false se já existe uma com o mesmo id (o mesmo pedido). */
    public boolean save(Matricula matricula) {
        return jdbc.sql("""
                INSERT INTO matricula.matriculas (id, estudante_id, turma_id, ano_letivo)
                VALUES (:id, :estudanteId, :turmaId, :anoLetivo)
                ON CONFLICT (id) DO NOTHING
                """)
            .paramSource(matricula)
            .update() == 1;
    }

    public Optional<Matricula> buscar(UUID id) {
        return jdbc.sql("SELECT id, estudante_id, turma_id, ano_letivo FROM matricula.matriculas WHERE id = :id")
            .param("id", id)
            .query(Matricula.class)
            .optional();
    }
}
