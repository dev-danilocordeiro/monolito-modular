package escola;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** A imagem final do serviço não pode rodar a JVM como root. */
class DockerfileTest {

    private static final Set<String> ROOT = Set.of("root", "0");

    @Test
    void estagioFinalDefineUsuarioNaoRootAntesDoEntrypoint() throws Exception {
        Path dockerfile = Path.of(System.getProperty("user.dir"), "Dockerfile");
        assertTrue(Files.exists(dockerfile), "Dockerfile não encontrado em " + dockerfile);

        List<String> linhas = Files.readAllLines(dockerfile);
        String usuario = null;
        boolean entrypoint = false;
        for (String linha : linhas) {
            String[] partes = linha.trim().split("\\s+", 2);
            String instrucao = partes[0].toUpperCase();
            if (instrucao.equals("FROM")) {
                usuario = null;
                entrypoint = false;
            } else if (instrucao.equals("USER") && partes.length > 1 && !entrypoint) {
                usuario = partes[1].trim();
            } else if (instrucao.equals("ENTRYPOINT")) {
                entrypoint = true;
            }
        }

        assertTrue(entrypoint, "o estágio final precisa de ENTRYPOINT");
        assertNotNull(usuario, "o estágio final precisa de USER antes do ENTRYPOINT");
        String nome = usuario.split(":", 2)[0];
        assertFalse(ROOT.contains(nome), "USER não pode ser root: " + usuario);
    }
}
