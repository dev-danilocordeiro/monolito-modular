package escola.matricula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import escola.TestcontainersConfiguration;
import escola.turmas.TurmaSemVaga;
import escola.turmas.TurmasApi;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
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
}
