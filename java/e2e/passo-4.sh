#!/usr/bin/env bash
# Passo 4 de ponta a ponta: monolito em modo remoto chamando o servico-turmas de verdade.
cd "$(dirname "$0")/.."
source e2e/comum.sh
trap limpa EXIT

./gradlew bootJar -q
docker compose up -d --build --wait postgres kafka > /dev/null
docker compose up -d --build servico-turmas > /dev/null
espera_http "http://localhost:$SERVICO_TURMAS_PORTA/turmas"
sobe_monolito remoto

turma=$(uuidgen)
psql_escola "INSERT INTO turmas.turmas (id, ano_letivo, capacidade) VALUES ('$turma', 2027, 2)" > /dev/null
ana=$(uuidgen); bia=$(uuidgen); caio=$(uuidgen)

confere "matrícula pelo serviço remoto"                      201 "$(matricula "$ana" "$turma")"
confere "a vaga foi ocupada no serviço"                      1   "$(psql_escola "SELECT ocupadas FROM turmas.turmas WHERE id = '$turma'")"

# A reserva remota dá certo, o INSERT da matrícula quebra na unicidade e a transação
# local desfaz. Sem compensação, a vaga ficaria presa e ocupadas iria para 2.
confere "matrícula duplicada é recusada"                     409 "$(matricula "$ana" "$turma")"
confere "a compensação devolveu a vaga"                      1   "$(psql_escola "SELECT ocupadas FROM turmas.turmas WHERE id = '$turma'")"
confere "a reserva da tentativa que falhou ficou LIBERADA"   1   "$(psql_escola "SELECT count(*) FROM turmas.reservas WHERE turma_id = '$turma' AND situacao = 'LIBERADA'")"

confere "segunda matrícula ocupa a última vaga"              201 "$(matricula "$bia" "$turma")"
confere "turma lotada vira 409 no monolito"                  409 "$(matricula "$caio" "$turma")"

for _ in $(seq 1 20); do
  [[ "$(psql_escola "SELECT count(*) FROM pedagogico.aprendizes WHERE id IN ('$ana', '$bia')")" == 2 ]] && break; sleep 0.5
done
confere "o pedagógico recebeu os eventos das duas matrículas" 2  "$(psql_escola "SELECT count(*) FROM pedagogico.aprendizes WHERE id IN ('$ana', '$bia')")"

# Rollback: mesmo schema, então voltar para local não exige sincronizar nada.
sobe_monolito local
confere "em modo local a turma continua lotada"              409 "$(matricula "$(uuidgen)" "$turma")"

exit "$falhas"
