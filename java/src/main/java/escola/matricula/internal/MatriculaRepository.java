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

    public Matricula save(Matricula matricula) {
        jdbc.sql("""
                INSERT INTO matricula.matriculas (id, estudante_id, turma_id, ano_letivo)
                VALUES (:id, :estudanteId, :turmaId, :anoLetivo)
                """)
            .paramSource(matricula)
            .update();
        return matricula;
    }

    /** Trava o id até o fim da transação atual: outra transação com o mesmo id espera aqui. */
    public void travar(UUID id) {
        jdbc.sql("SELECT pg_advisory_xact_lock(:chave)")
            .param("chave", id.getMostSignificantBits() ^ id.getLeastSignificantBits())
            .query()
            .listOfRows();
    }

    public Optional<Matricula> buscar(UUID id) {
        return jdbc.sql("SELECT id, estudante_id, turma_id, ano_letivo FROM matricula.matriculas WHERE id = :id")
            .param("id", id)
            .query(Matricula.class)
            .optional();
    }
}
