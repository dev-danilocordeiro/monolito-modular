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
import escola.turmas.TurmasApi;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RegistroDePublicacoesTest {

    @Autowired MatriculaService matriculas;
    @Autowired JdbcClient jdbc;
    @MockitoBean TurmasApi turmas;   // turmas é outro serviço

    @Test
    void cadaOuvinteGanhaUmaPublicacaoQueEConcluidaQuandoEleTermina() {
        var turma = UUID.randomUUID();
        var estudante = UUID.randomUUID();

        matriculas.matricular(new NovaMatricula(estudante, turma, 2027, "Ana"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(jdbc.sql("SELECT count(*) FROM pedagogico.aprendizes WHERE id = :id")
                .param("id", estudante).query(Integer.class).single()).isEqualTo(1);
            // Uma linha por ouvinte: o pedagógico e o externalizador do Kafka.
            assertThat(jdbc.sql("""
                    SELECT listener_id FROM event_publication
                    WHERE serialized_event LIKE :estudante AND completion_date IS NOT NULL
                    """)
                .param("estudante", "%" + estudante + "%").query(String.class).list())
                .hasSize(2)
                .anyMatch(ouvinte -> ouvinte.contains("AoConfirmarMatricula"));
        });
    }
}
