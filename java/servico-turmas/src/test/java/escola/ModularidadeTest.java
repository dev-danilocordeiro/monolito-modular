package escola;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularidadeTest {

    @Test
    void verificaFronteiras() {
        ApplicationModules.of(ServicoTurmasApplication.class).verify();
    }
}
