package escola.turmas.http;

import escola.turmas.TurmaNaoEncontrada;
import escola.turmas.TurmaSemVaga;
import escola.turmas.TurmasApi;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/turmas")
class TurmasController {

    private final TurmasApi turmas;

    TurmasController(TurmasApi turmas) {
        this.turmas = turmas;
    }

    @PostMapping("/{turmaId}/reservas")
    ResponseEntity<Void> reservar(@PathVariable UUID turmaId,
                                  @RequestHeader("Idempotency-Key") String chave) {
        turmas.reservarVaga(turmaId);   // TODO: deduplicar pela chave
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{turmaId}/liberacoes")
    ResponseEntity<Void> liberar(@PathVariable UUID turmaId,
                                 @RequestHeader("Idempotency-Key") String chave) {
        turmas.liberarVaga(turmaId);    // TODO: deduplicar pela chave
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(TurmaSemVaga.class)
    ProblemDetail semVaga(TurmaSemVaga erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, erro.getMessage());
    }

    @ExceptionHandler(TurmaNaoEncontrada.class)
    ProblemDetail naoEncontrada(TurmaNaoEncontrada erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, erro.getMessage());
    }
}
