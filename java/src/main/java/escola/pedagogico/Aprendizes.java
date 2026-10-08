package escola.pedagogico;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class Aprendizes {

    private final JdbcClient jdbc;

    Aprendizes(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Idempotente: a entrega é pelo menos uma vez, então o mesmo evento pode chegar de novo,
     * inclusive em paralelo. A chave primária decide, não um SELECT antes do INSERT.
     */
    boolean criarSeNaoExistir(UUID id, String nomeSocial) {
        return jdbc.sql("""
                INSERT INTO pedagogico.aprendizes (id, nome_social) VALUES (:id, :nomeSocial)
                ON CONFLICT (id) DO NOTHING
                """)
            .param("id", id)
            .param("nomeSocial", nomeSocial)
            .update() == 1;
    }

    int contar(UUID id) {
        return jdbc.sql("SELECT count(*) FROM pedagogico.aprendizes WHERE id = :id")
            .param("id", id)
            .query(Integer.class)
            .single();
    }
}
