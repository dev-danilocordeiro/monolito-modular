package escola;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import escola.matricula.MatriculaService;
import escola.matricula.NovaMatricula;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RegistroDePublicacoesTest {

    @Autowired MatriculaService matriculas;
    @Autowired JdbcClient jdbc;

    @Test
    void aPublicacaoEGravadaNaTransacaoDaMatriculaEConcluidaQuandoOOuvinteTermina() {
        var turma = UUID.randomUUID();
        jdbc.sql("INSERT INTO turmas.turmas (id, ano_letivo, capacidade) VALUES (:id, 2027, 10)").param("id", turma).update();
        var estudante = UUID.randomUUID();

        matriculas.matricular(new NovaMatricula(estudante, turma, 2027, "Ana"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(jdbc.sql("SELECT count(*) FROM pedagogico.aprendizes WHERE id = :id")
                .param("id", estudante).query(Integer.class).single()).isEqualTo(1);
            assertThat(jdbc.sql("""
                    SELECT count(*) FROM event_publication
                    WHERE serialized_event LIKE :estudante AND completion_date IS NOT NULL
                    """)
                .param("estudante", "%" + estudante + "%").query(Integer.class).single()).isEqualTo(1);
        });
    }
}
