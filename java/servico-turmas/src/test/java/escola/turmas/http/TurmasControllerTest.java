package escola.turmas.http;

import static org.assertj.core.api.Assertions.assertThat;

import escola.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.client.RestClient;

/** Contrato HTTP da fachada: o que o TurmasRemoto do monolito espera do serviço. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TurmasControllerTest {

    @LocalServerPort int porta;
    @Autowired JdbcClient jdbc;
    RestClient http;

    @BeforeEach
    void cliente() {
        http = RestClient.builder()
            .baseUrl("http://localhost:" + porta)
            .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> {})   // o teste olha o status
            .build();
    }

    @Test
    void reservaOcupaUmaVagaERespondeSemCorpo() {
        var turma = turmaComCapacidade(2);

        assertThat(post("/turmas/{id}/reservas", turma)).isEqualTo(204);
        assertThat(ocupadas(turma)).isEqualTo(1);
    }

    @Test
    void turmaLotadaResponde409() {
        var turma = turmaComCapacidade(1);
        post("/turmas/{id}/reservas", turma);

        assertThat(post("/turmas/{id}/reservas", turma)).isEqualTo(409);
        assertThat(ocupadas(turma)).isEqualTo(1);
    }

    @Test
    void turmaInexistenteResponde404() {
        assertThat(post("/turmas/{id}/reservas", UUID.randomUUID())).isEqualTo(404);
    }

    @Test
    void liberacaoDevolveAVaga() {
        var turma = turmaComCapacidade(1);
        post("/turmas/{id}/reservas", turma);

        assertThat(post("/turmas/{id}/liberacoes", turma)).isEqualTo(204);
        assertThat(ocupadas(turma)).isZero();
    }

    private int post(String caminho, UUID turma) {
        return http.post().uri(caminho, turma)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .retrieve().toBodilessEntity().getStatusCode().value();
    }

    private UUID turmaComCapacidade(int capacidade) {
        var id = UUID.randomUUID();
        jdbc.sql("INSERT INTO turmas.turmas (id, ano_letivo, capacidade) VALUES (:id, 2027, :capacidade)")
            .param("id", id).param("capacidade", capacidade).update();
        return id;
    }

    private int ocupadas(UUID turma) {
        return jdbc.sql("SELECT ocupadas FROM turmas.turmas WHERE id = :id").param("id", turma).query(Integer.class).single();
    }
}
