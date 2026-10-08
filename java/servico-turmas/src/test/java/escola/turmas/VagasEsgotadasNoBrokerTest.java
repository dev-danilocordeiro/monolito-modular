package escola.turmas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import escola.TestcontainersConfiguration;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.kafka.KafkaContainer;

/** O serviço publica no mesmo tópico que o monolito publicava no passo 2. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class VagasEsgotadasNoBrokerTest {

    @Autowired TurmasApi turmas;
    @Autowired JdbcClient jdbc;
    @Autowired KafkaContainer kafka;

    @Test
    void ocuparAUltimaVagaPublicaVagasEsgotadasNoTopicoDeSempre() {
        var turma = UUID.randomUUID();
        jdbc.sql("INSERT INTO turmas.turmas (id, ano_letivo, capacidade) VALUES (:id, 2027, 1)").param("id", turma).update();

        turmas.reservarVaga(turma, UUID.randomUUID());

        var recebidas = new ArrayList<String>();
        try (var consumidor = new KafkaConsumer<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "teste-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {
            consumidor.subscribe(List.of("escola.turmas.vagas-esgotadas"));
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                consumidor.poll(Duration.ofMillis(200)).forEach(r -> recebidas.add(r.value()));
                assertThat(recebidas).anyMatch(m -> m.contains(turma.toString()));
            });
        }
    }
}
