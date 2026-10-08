package escola.turmas;

import static org.assertj.core.api.Assertions.assertThat;

import escola.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.AssertablePublishedEvents;

@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class TurmasServiceTest {

    @Autowired TurmasService turmas;
    @Autowired JdbcClient jdbc;

    @Test
    void reservasSimultaneasNuncaPassamDaCapacidade(AssertablePublishedEvents eventos) throws Exception {
        var turma = UUID.randomUUID();
        jdbc.sql("INSERT INTO turmas.turmas (id, ano_letivo, capacidade) VALUES (:id, 2027, 3)").param("id", turma).update();

        var largada = new CountDownLatch(1);
        var resultados = new ArrayList<Future<Boolean>>();
        try (var executor = Executors.newFixedThreadPool(20)) {
            for (int i = 0; i < 20; i++) {
                Callable<Boolean> reserva = () -> {
                    largada.await();
                    try {
                        turmas.reservarVaga(turma);
                        return true;
                    } catch (TurmaSemVaga semVaga) {
                        return false;
                    }
                };
                resultados.add(executor.submit(reserva));
            }
            largada.countDown();
        }

        long confirmadas = 0;
        for (var resultado : resultados) {
            if (resultado.get()) confirmadas++;
        }
        assertThat(confirmadas).isEqualTo(3);
        assertThat(jdbc.sql("SELECT ocupadas FROM turmas.turmas WHERE id = :id").param("id", turma).query(Integer.class).single())
            .isEqualTo(3);
        assertThat(eventos.ofType(VagasEsgotadas.class).matching(ev -> ev.turmaId().equals(turma))).hasSize(1);
    }
}
