package escola.matricula;

import escola.matricula.internal.MatriculaRepository;
import escola.turmas.TurmasApi;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatriculaService {

    private final TurmasApi turmas;
    private final MatriculaRepository matriculas;
    private final ApplicationEventPublisher eventos;

    MatriculaService(TurmasApi turmas, MatriculaRepository matriculas, ApplicationEventPublisher eventos) {
        this.turmas = turmas;
        this.matriculas = matriculas;
        this.eventos = eventos;
    }

    @Transactional
    public Matricula matricular(NovaMatricula pedido) {
        turmas.reservarVaga(pedido.turmaId());                 // chamada: precisa da resposta
        var matricula = matriculas.save(Matricula.confirmada(pedido));
        eventos.publishEvent(new MatriculaConfirmada(          // evento: ninguém espera
            matricula.estudanteId(), pedido.turmaId(), pedido.anoLetivo(), pedido.nomeSocial()));
        return matricula;
    }
}
