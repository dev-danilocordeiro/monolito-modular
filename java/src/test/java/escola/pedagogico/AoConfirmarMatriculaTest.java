package escola.pedagogico;

import static org.assertj.core.api.Assertions.assertThat;

import escola.TestcontainersConfiguration;
import escola.matricula.MatriculaConfirmada;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class AoConfirmarMatriculaTest {

    @Autowired Aprendizes aprendizes;
    @Autowired JdbcClient jdbc;

    @Test
    void matriculaConfirmadaCriaOAprendizNoContextoPedagogico(Scenario scenario) {
        var evento = new MatriculaConfirmada(UUID.randomUUID(), UUID.randomUUID(), 2027, "Ana");

        scenario.publish(evento)
            .andWaitForStateChange(() -> aprendizes.contar(evento.estudanteId()), n -> n > 0)
            .andVerify(n -> assertThat(n).isEqualTo(1));
    }

    // A segunda entrega precisa terminar com sucesso: se ela quebrasse na chave
    // primária, a publicação ficaria incompleta e a espera estouraria o tempo.
    @Test
    void reentregaDoMesmoEventoConcluiSemDuplicarOAprendiz(Scenario scenario) {
        var evento = new MatriculaConfirmada(UUID.randomUUID(), UUID.randomUUID(), 2027, "Ana");

        scenario.publish(evento).andWaitForStateChange(() -> publicacoesConcluidas(evento), n -> n == 1);
        scenario.publish(evento)
            .andWaitForStateChange(() -> publicacoesConcluidas(evento), n -> n == 2)
            .andVerify(n -> assertThat(aprendizes.contar(evento.estudanteId())).isEqualTo(1));
    }

    // Falharia com um "SELECT, se não existe INSERT": as entregas passariam pelo
    // SELECT juntas e todas menos uma quebrariam na chave primária. A janela da
    // corrida é estreita, por isso o teste repete várias rodadas.
    @Test
    void entregasSimultaneasDoMesmoEventoNaoFalhamNemDuplicam() throws Exception {
        for (int rodada = 0; rodada < 50; rodada++) {
            var estudante = UUID.randomUUID();
            var largada = new CountDownLatch(1);
            var entregas = new ArrayList<Future<Boolean>>();

            try (var executor = Executors.newFixedThreadPool(16)) {
                for (int i = 0; i < 16; i++) {
                    entregas.add(executor.submit(() -> {
                        largada.await();
                        return aprendizes.criarSeNaoExistir(estudante, "Ana");
                    }));
                }
                largada.countDown();
            }

            long criados = 0;
            for (var entrega : entregas) {
                if (entrega.get()) criados++;   // get() propaga qualquer exceção da entrega
            }
            assertThat(criados).isEqualTo(1);
            assertThat(aprendizes.contar(estudante)).isEqualTo(1);
        }
    }

    private int publicacoesConcluidas(MatriculaConfirmada evento) {
        return jdbc.sql("""
                SELECT count(*) FROM event_publication
                WHERE serialized_event LIKE :estudante AND completion_date IS NOT NULL
                """)
            .param("estudante", "%" + evento.estudanteId() + "%")
            .query(Integer.class)
            .single();
    }
}
