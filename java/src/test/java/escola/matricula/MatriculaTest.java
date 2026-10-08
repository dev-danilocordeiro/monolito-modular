package escola.matricula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import escola.TestcontainersConfiguration;
import escola.turmas.TurmaSemVaga;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.modulith.test.Scenario;

// O padrão (STANDALONE) sobe só o módulo matricula. Como a matrícula chama turmas,
// DIRECT_DEPENDENCIES sobe também as dependências diretas declaradas no package-info.
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES)
@Import(TestcontainersConfiguration.class)
class MatriculaTest {

    static final UUID TURMA_COM_VAGA = UUID.randomUUID();

    @Autowired MatriculaService service;
    @Autowired JdbcClient jdbc;

    @BeforeEach
    void abreTurmaComVaga() {
        jdbc.sql("""
                INSERT INTO turmas.turmas (id, ano_letivo, capacidade) VALUES (:id, 2027, 1000)
                ON CONFLICT (id) DO NOTHING
                """)
            .param("id", TURMA_COM_VAGA)
            .update();
    }

    @Test
    void publicaMatriculaConfirmada(Scenario scenario) {
        var pedido = new NovaMatricula(UUID.randomUUID(), TURMA_COM_VAGA, 2027, "Ana");

        scenario.stimulate(() -> service.matricular(pedido))
            .andWaitForEventOfType(MatriculaConfirmada.class)
            .matching(ev -> ev.estudanteId().equals(pedido.estudanteId()))
            .toArrive();
    }

    @Test
    void matriculaQueFalhaDevolveAVagaPorqueReservaESaveEstaoNaMesmaTransacao() {
        var turma = turmaComCapacidade(2);
        var pedido = new NovaMatricula(UUID.randomUUID(), turma, 2027, "Ana");
        service.matricular(pedido);

        assertThatThrownBy(() -> service.matricular(pedido)).isInstanceOf(DuplicateKeyException.class);

        assertThat(ocupadas(turma)).isEqualTo(1);
    }

    @Test
    void turmaLotadaRecusaAMatriculaSemPublicarEvento(AssertablePublishedEvents eventos) {
        var turma = turmaComCapacidade(1);
        service.matricular(new NovaMatricula(UUID.randomUUID(), turma, 2027, "Ana"));
        var recusado = new NovaMatricula(UUID.randomUUID(), turma, 2027, "Bia");

        assertThatThrownBy(() -> service.matricular(recusado)).isInstanceOf(TurmaSemVaga.class);

        assertThat(eventos.ofType(MatriculaConfirmada.class)
            .matching(ev -> ev.estudanteId().equals(recusado.estudanteId()))).isEmpty();
    }

    private UUID turmaComCapacidade(int capacidade) {
        var id = UUID.randomUUID();
        jdbc.sql("INSERT INTO turmas.turmas (id, ano_letivo, capacidade) VALUES (:id, 2027, :capacidade)")
            .param("id", id)
            .param("capacidade", capacidade)
            .update();
        return id;
    }

    private int ocupadas(UUID turma) {
        return jdbc.sql("SELECT ocupadas FROM turmas.turmas WHERE id = :id").param("id", turma).query(Integer.class).single();
    }
}
