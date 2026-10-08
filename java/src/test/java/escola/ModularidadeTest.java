package escola;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularidadeTest {

    ApplicationModules modulos = ApplicationModules.of(EscolaApplication.class);

    @Test
    void verificaFronteiras() {
        modulos.verify();   // ciclos e acesso a tipos internos reprovam o teste
    }

    @Test
    void geraDocumentacao() {
        new Documenter(modulos).writeDocumentation();   // diagramas C4 em PlantUML
    }
}
