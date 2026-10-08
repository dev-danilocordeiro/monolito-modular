package escola.matricula.internal;

import escola.matricula.Matricula;
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
}
