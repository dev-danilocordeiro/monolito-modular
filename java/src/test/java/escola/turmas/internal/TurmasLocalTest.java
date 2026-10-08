package escola.turmas.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import escola.TestcontainersConfiguration;
import escola.turmas.ReservaConflitante;
import escola.turmas.TurmaSemVaga;
import escola.turmas.TurmasApi;
import escola.turmas.VagasEsgotadas;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.AssertablePublishedEvents;

@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "escola.turmas.modo=local")
class TurmasLocalTest {

    @Autowired TurmasApi turmas;
    @Autowired JdbcClient jdbc;

    @Test
    void comModoLocalAImplementacaoEOTurmasLocal() {
        assertThat(turmas).isInstanceOf(TurmasLocal.class);
    }

    @Test
    void reservasSimultaneasNuncaPassamDaCapacidade(AssertablePublishedEvents eventos) throws Exception {
        var turma = turmaComCapacidade(3);

        long confirmadas = emParalelo(20, () -> {
            try {
                turmas.reservarVaga(turma, UUID.randomUUID());
                return true;
            } catch (TurmaSemVaga semVaga) {
                return false;
            }
        });

        assertThat(confirmadas).isEqualTo(3);
        assertThat(ocupadas(turma)).isEqualTo(3);
        assertThat(eventos.ofType(VagasEsgotadas.class).matching(ev -> ev.turmaId().equals(turma))).hasSize(1);
    }

    @Test
    void repetirAReservaComAMesmaChaveNaoOcupaOutraVaga() {
        var turma = turmaComCapacidade(5);
        var reserva = UUID.randomUUID();

        turmas.reservarVaga(turma, reserva);
        turmas.reservarVaga(turma, reserva);

        assertThat(ocupadas(turma)).isEqualTo(1);
    }

    // Falharia sem a chave primária decidindo: as chamadas passariam juntas por uma
    // checagem prévia e cada uma ocuparia uma vaga.
    @Test
    void reservasSimultaneasComAMesmaChaveOcupamUmaVagaSo() throws Exception {
        for (int rodada = 0; rodada < 20; rodada++) {
            var turma = turmaComCapacidade(50);
            var reserva = UUID.randomUUID();

            long confirmadas = emParalelo(16, () -> {
                turmas.reservarVaga(turma, reserva);
                return true;
            });

            assertThat(confirmadas).isEqualTo(16);   // todas respondem sucesso...
            assertThat(ocupadas(turma)).isEqualTo(1); // ...mas só uma vaga é ocupada
        }
    }

    @Test
    void liberarDuasVezesDevolveUmaVagaSo() {
        var turma = turmaComCapacidade(5);
        var outra = UUID.randomUUID();
        turmas.reservarVaga(turma, outra);
        var reserva = UUID.randomUUID();
        turmas.reservarVaga(turma, reserva);

        turmas.liberarVaga(turma, reserva);
        turmas.liberarVaga(turma, reserva);

        assertThat(ocupadas(turma)).isEqualTo(1);
    }

    @Test
    void liberarAntesDeReservarFazAReservaAtrasadaSerRecusada() {
        var turma = turmaComCapacidade(5);
        var reserva = UUID.randomUUID();

        turmas.liberarVaga(turma, reserva);   // a compensação chegou antes da reserva

        assertThatThrownBy(() -> turmas.reservarVaga(turma, reserva)).isInstanceOf(ReservaConflitante.class);
        assertThat(ocupadas(turma)).isZero();
    }

    @Test
    void mesmaChaveParaOutraTurmaERecusada() {
        var reserva = UUID.randomUUID();
        turmas.reservarVaga(turmaComCapacidade(5), reserva);

        var outraTurma = turmaComCapacidade(5);
        assertThatThrownBy(() -> turmas.reservarVaga(outraTurma, reserva)).isInstanceOf(ReservaConflitante.class);
        assertThat(ocupadas(outraTurma)).isZero();
    }

    @Test
    void reservaSemVagaNaoDeixaAChaveGravada() {
        var turma = turmaComCapacidade(1);
        turmas.reservarVaga(turma, UUID.randomUUID());
        var recusada = UUID.randomUUID();

        assertThatThrownBy(() -> turmas.reservarVaga(turma, recusada)).isInstanceOf(TurmaSemVaga.class);

        assertThat(jdbc.sql("SELECT count(*) FROM turmas.reservas WHERE id = :id").param("id", recusada)
            .query(Integer.class).single()).isZero();
    }

    private long emParalelo(int chamadas, Callable<Boolean> chamada) throws Exception {
        var largada = new CountDownLatch(1);
        var resultados = new ArrayList<Future<Boolean>>();
        try (var executor = Executors.newFixedThreadPool(chamadas)) {
            for (int i = 0; i < chamadas; i++) {
                resultados.add(executor.submit(() -> {
                    largada.await();
                    return chamada.call();
                }));
            }
            largada.countDown();
        }
        long verdadeiros = 0;
        for (var resultado : resultados) {
            if (resultado.get()) verdadeiros++;
        }
        return verdadeiros;
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
