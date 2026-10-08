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
        var matricula = Matricula.confirmada(pedido);
        // O id da matrícula identifica esta tentativa e vira a chave da reserva:
        // um retry da mesma tentativa nunca reserva duas vagas.
        turmas.reservarVaga(pedido.turmaId(), matricula.id());   // chamada: precisa da resposta
        matriculas.save(matricula);
        eventos.publishEvent(new MatriculaConfirmada(            // evento: ninguém espera
            matricula.estudanteId(), pedido.turmaId(), pedido.anoLetivo(), pedido.nomeSocial()));
        return matricula;
    }
}
