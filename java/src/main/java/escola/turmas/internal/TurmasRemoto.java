package escola.turmas.internal;

import escola.turmas.ReservaConflitante;
import escola.turmas.TurmaNaoEncontrada;
import escola.turmas.TurmaSemVaga;
import escola.turmas.TurmasApi;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Service
class TurmasRemoto implements TurmasApi {

    private static final Logger log = LoggerFactory.getLogger(TurmasRemoto.class);

    private final RestClient http;

    // Timeout ou conexão recusada: não dá para saber se o serviço processou, então
    // repete. É seguro porque a repetição leva a mesma chave.
    private final RetryTemplate retry = new RetryTemplate(RetryPolicy.builder()
        .includes(ResourceAccessException.class)
        .maxRetries(2)
        .delay(Duration.ofMillis(200))
        .build());

    TurmasRemoto(RestClient.Builder builder, @Value("${escola.turmas.url}") String url) {
        this.http = builder.baseUrl(url).build();
    }

    @Override
    public void reservarVaga(UUID turmaId, UUID reservaId) {
        // Registrada antes da chamada: se todas as tentativas estourarem o tempo, não dá
        // para saber se o serviço reservou. Liberar com a mesma chave cobre os dois casos.
        liberarSeATransacaoDesfizer(turmaId, reservaId);
        retry.invoke(() -> http.post()
            .uri("/turmas/{id}/reservas", turmaId)
            .header("Idempotency-Key", reservaId.toString())   // estável: vem da tentativa de matrícula
            .retrieve()
            .onStatus(status -> status.isSameCodeAs(HttpStatus.CONFLICT), (req, res) -> { throw new TurmaSemVaga(turmaId); })
            .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (req, res) -> { throw new TurmaNaoEncontrada(turmaId); })
            .onStatus(status -> status.isSameCodeAs(HttpStatus.UNPROCESSABLE_CONTENT),
                (req, res) -> { throw new ReservaConflitante(reservaId, "recusada pelo serviço de turmas"); })
            .toBodilessEntity());
    }

    @Override
    public void liberarVaga(UUID turmaId, UUID reservaId) {
        retry.invoke(() -> http.delete()
            .uri("/turmas/{id}/reservas/{reserva}", turmaId, reservaId)
            .retrieve()
            .toBodilessEntity());
    }

    /**
     * Antes da extração, reserva e matrícula estavam na mesma transação: se a matrícula
     * falhasse, a vaga voltava junto. A chamada remota não participa da transação local,
     * então a devolução vira uma compensação explícita, disparada quando ela desfaz.
     */
    private void liberarSeATransacaoDesfizer(UUID turmaId, UUID reservaId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) {
                    return;
                }
                try {
                    liberarVaga(turmaId, reservaId);
                } catch (RuntimeException erro) {
                    // Sem retry depois daqui: a vaga fica presa até alguém liberar com a mesma chave.
                    log.atError()
                        .addKeyValue("turmaId", turmaId)
                        .addKeyValue("reservaId", reservaId)
                        .setCause(erro)
                        .log("compensação falhou: vaga reservada para uma matrícula que não existe");
                }
            }
        });
    }
}
