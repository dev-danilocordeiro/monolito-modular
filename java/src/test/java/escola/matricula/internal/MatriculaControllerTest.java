package escola.matricula.internal;

import static org.assertj.core.api.Assertions.assertThat;

import escola.turmas.ReservaConflitante;
import escola.turmas.TurmaSemVaga;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(OutputCaptureExtension.class)
class MatriculaControllerTest {

    final MatriculaController controller = new MatriculaController(null, null);

    @Test
    void duplicadaNaoVazaDetalheDoBancoMasRegistraNoLog(CapturedOutput saida) {
        var mensagemDoBanco = "PreparedStatementCallback; SQL [insert into matricula (id) values (?)];"
                + " duplicate key value violates unique constraint \"matricula_pkey\" Key (id)=(abc-123)";

        var corpo = controller.duplicada(new DuplicateKeyException(mensagemDoBanco));

        assertThat(corpo.getStatus()).isEqualTo(409);
        assertThat(corpo.getDetail()).isEqualTo("matrícula já existe");
        assertThat(saida.getOut()).contains(mensagemDoBanco);
    }

    @Test
    void conflitosDeTurmaMantemSuaMensagem() {
        var turma = UUID.randomUUID();
        var semVaga = controller.conflito(new TurmaSemVaga(turma));
        var reserva = controller.conflito(new ReservaConflitante(turma, "outra turma"));

        assertThat(semVaga.getStatus()).isEqualTo(409);
        assertThat(semVaga.getDetail()).isEqualTo("turma " + turma + " sem vaga");
        assertThat(reserva.getDetail()).isEqualTo("reserva " + turma + ": outra turma");
    }
}
