package escola.matricula.internal;

import escola.matricula.Matricula;
import escola.matricula.MatriculaService;
import escola.matricula.NovaMatricula;
import escola.turmas.ReservaConflitante;
import escola.turmas.TurmaNaoEncontrada;
import escola.turmas.TurmaSemVaga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MatriculaController {

    private static final Logger log = LoggerFactory.getLogger(MatriculaController.class);

    private final MatriculaService matriculas;
    private final MatriculaRepository repositorio;

    MatriculaController(MatriculaService matriculas, MatriculaRepository repositorio) {
        this.matriculas = matriculas;
        this.repositorio = repositorio;
    }

    @PostMapping("/matriculas")
    @ResponseStatus(HttpStatus.CREATED)
    Matricula matricular(@RequestBody NovaMatricula pedido) {
        // Pedido repetido não reserva outra vaga (o serviço é idempotente); a API segue respondendo 409.
        if (repositorio.buscar(Matricula.idDo(pedido)).isPresent()) {
            throw new DuplicateKeyException("estudante já matriculado nesta turma e ano letivo");
        }
        return matriculas.matricular(pedido);
    }

    @ExceptionHandler({TurmaSemVaga.class, ReservaConflitante.class})
    ProblemDetail conflito(RuntimeException erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, erro.getMessage());
    }

    // A mensagem vinda do banco pode trazer SQL, constraint e ids: fica só no log.
    @ExceptionHandler(DuplicateKeyException.class)
    ProblemDetail duplicada(DuplicateKeyException erro) {
        log.warn("matrícula duplicada", erro);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "matrícula já existe");
    }

    @ExceptionHandler(TurmaNaoEncontrada.class)
    ProblemDetail naoEncontrada(TurmaNaoEncontrada erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, erro.getMessage());
    }
}
