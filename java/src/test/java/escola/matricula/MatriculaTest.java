package escola.matricula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import escola.TestcontainersConfiguration;
import escola.turmas.TurmaSemVaga;
import escola.turmas.TurmasApi;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.mockito.ArgumentCaptor;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// Depois da extração, turmas é um serviço: o teste do módulo sobe só a matrícula
// (o modo padrão, STANDALONE) e substitui a porta por um mock.
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class MatriculaTest {

    static final UUID TURMA_COM_VAGA = UUID.randomUUID();

    @Autowired MatriculaService service;
    @Autowired JdbcClient jdbc;
    @MockitoBean TurmasApi turmas;

    @Test
    void publicaMatriculaConfirmada(Scenario scenario) {
        var pedido = new NovaMatricula(UUID.randomUUID(), TURMA_COM_VAGA, 2027, "Ana");

        scenario.stimulate(() -> service.matricular(pedido))
            .andWaitForEventOfType(MatriculaConfirmada.class)
            .matching(ev -> ev.estudanteId().equals(pedido.estudanteId()))
            .toArrive();
    }

    @Test
    void aChaveDaReservaEOIdDaMatricula() {
        var matricula = service.matricular(new NovaMatricula(UUID.randomUUID(), TURMA_COM_VAGA, 2027, "Ana"));

        verify(turmas).reservarVaga(TURMA_COM_VAGA, matricula.id());
    }

    @Test
    void turmaLotadaRecusaAMatriculaSemPublicarEvento(AssertablePublishedEvents eventos) {
        var turmaLotada = UUID.randomUUID();
        doThrow(new TurmaSemVaga(turmaLotada)).when(turmas).reservarVaga(eq(turmaLotada), any());
        var recusado = new NovaMatricula(UUID.randomUUID(), turmaLotada, 2027, "Bia");

        assertThatThrownBy(() -> service.matricular(recusado)).isInstanceOf(TurmaSemVaga.class);

        assertThat(eventos.ofType(MatriculaConfirmada.class)
            .matching(ev -> ev.estudanteId().equals(recusado.estudanteId()))).isEmpty();
    }

    @Test
    void pedidoRepetidoDevolveAMesmaMatriculaSemReservarOutraVaga() {
        var pedido = new NovaMatricula(UUID.randomUUID(), TURMA_COM_VAGA, 2027, "Ana");

        var primeira = service.matricular(pedido);
        var segunda = service.matricular(pedido);

        assertThat(segunda).isEqualTo(primeira);
        verify(turmas, times(1)).reservarVaga(eq(TURMA_COM_VAGA), any());
        assertThat(matriculasDo(pedido)).isEqualTo(1);
    }

    @Test
    void pedidosIguaisSimultaneosViramUmaMatriculaEUmaReserva() throws Exception {
        var turma = UUID.randomUUID();
        var pedido = new NovaMatricula(UUID.randomUUID(), turma, 2027, "Ana");
        // As duas chamadas só passam da reserva juntas: nenhuma viu a matrícula da outra.
        var juntas = new CyclicBarrier(2);
        doAnswer(chamada -> juntas.await(5, TimeUnit.SECONDS)).when(turmas).reservarVaga(eq(turma), any());

        var primeira = CompletableFuture.supplyAsync(() -> service.matricular(pedido));
        var segunda = CompletableFuture.supplyAsync(() -> service.matricular(pedido));

        assertThat(segunda.get(10, TimeUnit.SECONDS)).isEqualTo(primeira.get(10, TimeUnit.SECONDS));
        var chaves = ArgumentCaptor.forClass(UUID.class);
        verify(turmas, atLeastOnce()).reservarVaga(eq(turma), chaves.capture());
        assertThat(chaves.getAllValues()).containsOnly(primeira.get().id());
        assertThat(matriculasDo(pedido)).isEqualTo(1);
    }

    private int matriculasDo(NovaMatricula pedido) {
        return jdbc.sql("SELECT count(*) FROM matricula.matriculas WHERE estudante_id = :estudante")
            .param("estudante", pedido.estudanteId())
            .query(Integer.class)
            .single();
    }
}
