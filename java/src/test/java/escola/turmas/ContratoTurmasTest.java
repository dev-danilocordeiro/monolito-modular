package escola.turmas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** O contrato de turmas vem do módulo compartilhado turmas-api, e não de uma cópia no monólito. */
class ContratoTurmasTest {

    @Test
    void contratoVemDoModuloCompartilhado() {
        for (Class<?> tipo : List.of(TurmasApi.class, VagasEsgotadas.class, TurmaSemVaga.class,
                TurmaNaoEncontrada.class, ReservaConflitante.class)) {
            assertThat(tipo.getProtectionDomain().getCodeSource().getLocation().getPath())
                    .as(tipo.getSimpleName())
                    .contains("/turmas-api/");
        }
    }

}
