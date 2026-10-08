package escola.pedagogico;

import escola.matricula.MatriculaConfirmada;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class AoConfirmarMatricula {

    private final Aprendizes aprendizes;

    AoConfirmarMatricula(Aprendizes aprendizes) {
        this.aprendizes = aprendizes;
    }

    @ApplicationModuleListener
    void on(MatriculaConfirmada evento) {
        aprendizes.criarSeNaoExistir(evento.estudanteId(), evento.nomeSocial());
    }
}
