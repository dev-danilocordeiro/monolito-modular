package escola.turmas.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import escola.turmas.TurmaSemVaga;
import escola.turmas.TurmasApi;
import java.net.SocketTimeoutException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@RestClientTest(
    components = TurmasRemoto.class,
    properties = {"escola.turmas.modo=remoto", "escola.turmas.url=http://servico-turmas"})
class TurmasRemotoTest {

    @Autowired TurmasApi turmas;
    @Autowired MockRestServiceServer servico;

    UUID turma = UUID.randomUUID();
    UUID reserva = UUID.randomUUID();

    @Test
    void aReservaUsaOIdDaTentativaComoChaveDeIdempotencia() {
        esperaReserva().andRespond(withNoContent());

        turmas.reservarVaga(turma, reserva);

        servico.verify();
    }

    // Com UUID.randomUUID() a cada chamada, o retry chegaria com outra chave e o
    // serviço reservaria uma segunda vaga para a mesma matrícula.
    @Test
    void depoisDeUmTimeoutORetryReenviaAMesmaChave() {
        esperaReserva().andRespond(withException(new SocketTimeoutException("read timed out")));
        esperaReserva().andRespond(withNoContent());

        turmas.reservarVaga(turma, reserva);

        servico.verify();
    }

    @Test
    void turmaLotadaNoServicoViraTurmaSemVaga() {
        esperaReserva().andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> turmas.reservarVaga(turma, reserva)).isInstanceOf(TurmaSemVaga.class);
    }

    @Test
    void quandoAMatriculaDesfazATransacaoAVagaEDevolvidaComAMesmaChave() {
        esperaReserva().andRespond(withNoContent());
        servico.expect(requestTo("http://servico-turmas/turmas/" + turma + "/reservas/" + reserva))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withNoContent());

        transacao().executeWithoutResult(tx -> {
            turmas.reservarVaga(turma, reserva);
            tx.setRollbackOnly();   // a matrícula falhou depois da reserva
        });

        servico.verify();
    }

    @Test
    void quandoAMatriculaConfirmaAVagaFicaReservada() {
        esperaReserva().andRespond(withNoContent());

        transacao().executeWithoutResult(tx -> turmas.reservarVaga(turma, reserva));

        servico.verify();   // nenhum DELETE: uma requisição inesperada reprovaria aqui
    }

    @Test
    void seTodasAsTentativasEstouramOTempoAVagaEDevolvidaMesmoAssim() {
        for (int i = 0; i < 3; i++) {
            esperaReserva().andRespond(withException(new SocketTimeoutException("read timed out")));
        }
        servico.expect(requestTo("http://servico-turmas/turmas/" + turma + "/reservas/" + reserva))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withNoContent());

        assertThatThrownBy(() -> transacao().executeWithoutResult(tx -> turmas.reservarVaga(turma, reserva)))
            .isInstanceOf(RuntimeException.class);

        servico.verify();
    }

    private org.springframework.test.web.client.ResponseActions esperaReserva() {
        return servico.expect(requestTo("http://servico-turmas/turmas/" + turma + "/reservas"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Idempotency-Key", reserva.toString()));
    }

    /** Uma transação sem banco: só o que importa aqui é o commit ou o rollback. */
    private static TransactionTemplate transacao() {
        var semBanco = new AbstractPlatformTransactionManager() {
            @Override protected Object doGetTransaction() { return new Object(); }
            @Override protected void doBegin(Object tx, TransactionDefinition def) {}
            @Override protected void doCommit(DefaultTransactionStatus status) {}
            @Override protected void doRollback(DefaultTransactionStatus status) {}
        };
        return new TransactionTemplate(semBanco);
    }
}
