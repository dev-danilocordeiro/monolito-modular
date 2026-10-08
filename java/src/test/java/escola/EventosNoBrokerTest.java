package escola;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import escola.matricula.MatriculaService;
import escola.matricula.NovaMatricula;
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

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EventosNoBrokerTest {

    @Autowired MatriculaService matriculas;
    @Autowired JdbcClient jdbc;
    @Autowired KafkaContainer kafka;

    @Test
    void eventosExternalizadosChegamNoKafkaEOsOuvintesInternosContinuamRecebendo() {
        var turma = UUID.randomUUID();
        jdbc.sql("INSERT INTO turmas.turmas (id, ano_letivo, capacidade) VALUES (:id, 2027, 1)").param("id", turma).update();
        var estudante = UUID.randomUUID();

        matriculas.matricular(new NovaMatricula(estudante, turma, 2027, "Ana"));   // ocupa a única vaga

        var recebidas = new ArrayList<String>();
        try (var consumidor = consumidor()) {
            consumidor.subscribe(List.of("escola.turmas.vagas-esgotadas", "escola.matriculas.confirmadas"));
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                consumidor.poll(Duration.ofMillis(200)).forEach(r -> recebidas.add(r.topic() + " " + r.value()));
                assertThat(recebidas)
                    .anyMatch(m -> m.startsWith("escola.turmas.vagas-esgotadas ") && m.contains(turma.toString()))
                    .anyMatch(m -> m.startsWith("escola.matriculas.confirmadas ") && m.contains(estudante.toString()));
            });
        }

        await().atMost(Duration.ofSeconds(10)).until(() -> jdbc.sql("SELECT count(*) FROM pedagogico.aprendizes WHERE id = :id")
            .param("id", estudante).query(Integer.class).single() == 1);
    }

    private KafkaConsumer<String, String> consumidor() {
        return new KafkaConsumer<>(Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
            ConsumerConfig.GROUP_ID_CONFIG, "teste-" + UUID.randomUUID(),
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
    }
}
