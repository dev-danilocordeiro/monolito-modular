package escola.turmas.http;

import static org.assertj.core.api.Assertions.assertThat;

import escola.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

        assertThat(reservar(turma, UUID.randomUUID())).isEqualTo(204);
        assertThat(ocupadas(turma)).isEqualTo(1);
    }

    @Test
    void turmaLotadaResponde409() {
        var turma = turmaComCapacidade(1);
        reservar(turma, UUID.randomUUID());

        assertThat(reservar(turma, UUID.randomUUID())).isEqualTo(409);
        assertThat(ocupadas(turma)).isEqualTo(1);
    }

    @Test
    void turmaInexistenteResponde404() {
        assertThat(reservar(UUID.randomUUID(), UUID.randomUUID())).isEqualTo(404);
    }

    @Test
    void reenviarAMesmaChaveResponde204SemOcuparOutraVaga() {
        var turma = turmaComCapacidade(5);
        var chave = UUID.randomUUID();

        assertThat(reservar(turma, chave)).isEqualTo(204);
        assertThat(reservar(turma, chave)).isEqualTo(204);
        assertThat(ocupadas(turma)).isEqualTo(1);
    }

    @Test
    void reenviosSimultaneosDaMesmaChaveOcupamUmaVagaSo() throws Exception {
        var turma = turmaComCapacidade(50);
        var chave = UUID.randomUUID();
        var largada = new CountDownLatch(1);
        var respostas = new ArrayList<Future<Integer>>();

        try (var executor = Executors.newFixedThreadPool(10)) {
            for (int i = 0; i < 10; i++) {
                respostas.add(executor.submit(() -> {
                    largada.await();
                    return reservar(turma, chave);
                }));
            }
            largada.countDown();
        }

        for (var resposta : respostas) {
            assertThat(resposta.get()).isEqualTo(204);
        }
        assertThat(ocupadas(turma)).isEqualTo(1);
    }

    @Test
    void chaveReaproveitadaParaOutraTurmaResponde422() {
        var chave = UUID.randomUUID();
        reservar(turmaComCapacidade(5), chave);

        var outra = turmaComCapacidade(5);
        assertThat(reservar(outra, chave)).isEqualTo(422);
        assertThat(ocupadas(outra)).isZero();
    }

    @Test
    void chaveQueNaoEUuidResponde400() {
        var status = http.post().uri("/turmas/{id}/reservas", turmaComCapacidade(1))
            .header("Idempotency-Key", "nao-e-uuid")
            .retrieve().toBodilessEntity().getStatusCode().value();

        assertThat(status).isEqualTo(400);
    }

    @Test
    void liberarDevolveAVagaEDeleteRepetidoNaoDevolveDeNovo() {
        var turma = turmaComCapacidade(5);
        reservar(turma, UUID.randomUUID());
        var chave = UUID.randomUUID();
        reservar(turma, chave);

        assertThat(liberar(turma, chave)).isEqualTo(204);
        assertThat(liberar(turma, chave)).isEqualTo(204);
        assertThat(ocupadas(turma)).isEqualTo(1);
    }

    private int reservar(UUID turma, UUID chave) {
        return http.post().uri("/turmas/{id}/reservas", turma)
            .header("Idempotency-Key", chave.toString())
            .retrieve().toBodilessEntity().getStatusCode().value();
    }

    private int liberar(UUID turma, UUID chave) {
        return http.delete().uri("/turmas/{id}/reservas/{reserva}", turma, chave)
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
