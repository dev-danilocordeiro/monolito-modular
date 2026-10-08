package escola.matricula.internal;

import escola.matricula.Matricula;
import escola.matricula.MatriculaService;
import escola.matricula.NovaMatricula;
import escola.turmas.ReservaConflitante;
import escola.turmas.TurmaNaoEncontrada;
import escola.turmas.TurmaSemVaga;
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

    private final MatriculaService matriculas;

    MatriculaController(MatriculaService matriculas) {
        this.matriculas = matriculas;
    }

    @PostMapping("/matriculas")
    @ResponseStatus(HttpStatus.CREATED)
    Matricula matricular(@RequestBody NovaMatricula pedido) {
        return matriculas.matricular(pedido);
    }

    @ExceptionHandler({TurmaSemVaga.class, DuplicateKeyException.class, ReservaConflitante.class})
    ProblemDetail conflito(RuntimeException erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, erro.getMessage());
    }

    @ExceptionHandler(TurmaNaoEncontrada.class)
    ProblemDetail naoEncontrada(TurmaNaoEncontrada erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, erro.getMessage());
    }
}
