package escola.matricula;

import escola.matricula.internal.MatriculaRepository;
import escola.turmas.TurmasApi;
import java.util.UUID;
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
        // O id vem do pedido (estudante, turma e ano letivo): pedidos iguais se enfileiram aqui
        // até o fim da transação, e um retry encontra a matrícula já gravada e a devolve.
        matriculas.travar(matricula.id());
        var existente = matriculas.buscar(matricula.id());
        if (existente.isPresent()) {
            return existente.get();
        }
        // A chave da reserva é desta tentativa: se a transação desfizer, a compensação a libera,
        // e o retry do mesmo pedido reserva com uma chave nova em vez de esbarrar na liberada.
        turmas.reservarVaga(pedido.turmaId(), UUID.randomUUID());   // chamada: precisa da resposta
        matriculas.save(matricula);
        eventos.publishEvent(new MatriculaConfirmada(            // evento: ninguém espera
            matricula.estudanteId(), pedido.turmaId(), pedido.anoLetivo(), pedido.nomeSocial()));
        return matricula;
    }
}
