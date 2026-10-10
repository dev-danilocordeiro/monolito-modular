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
        var existente = matriculas.buscar(matricula.id());
        if (existente.isPresent()) {
            return existente.get();
        }
        // O id da matrícula vem do pedido (estudante, turma e ano letivo) e vira a chave da
        // reserva: repetir o pedido (retry, reenvio do cliente) não reserva outra vaga.
        turmas.reservarVaga(pedido.turmaId(), matricula.id());   // chamada: precisa da resposta
        if (!matriculas.save(matricula)) {
            // Um pedido igual e simultâneo gravou primeiro, com a mesma reserva: devolve o dele.
            return matriculas.buscar(matricula.id()).orElseThrow();
        }
        eventos.publishEvent(new MatriculaConfirmada(            // evento: ninguém espera
            matricula.estudanteId(), pedido.turmaId(), pedido.anoLetivo(), pedido.nomeSocial()));
        return matricula;
    }
}
