#!/usr/bin/env bash
# Passo 6 de ponta a ponta: o monolito sem o módulo turmas, falando com o serviço,
# que tem banco próprio. É o estado final da extração.
cd "$(dirname "$0")/.."
source e2e/comum.sh
trap limpa EXIT

psql_turmas() { docker compose exec -T postgres-turmas psql -U turmas -d turmas -tAc "$1"; }

./gradlew bootJar -q
docker compose up -d --build --wait postgres postgres-turmas kafka > /dev/null
docker compose up -d --build servico-turmas > /dev/null
espera_http "http://localhost:$SERVICO_TURMAS_PORTA/turmas"
sobe_monolito remoto   # o modo não existe mais; a variável é ignorada

confere "o monolito não tem mais o schema turmas"            0   "$(psql_escola "SELECT count(*) FROM pg_namespace WHERE nspname = 'turmas'")"

turma=$(uuidgen)
psql_turmas "INSERT INTO turmas.turmas (id, ano_letivo, capacidade) VALUES ('$turma', 2027, 2)" > /dev/null
ana=$(uuidgen); bia=$(uuidgen)

confere "matrícula pelo serviço"                             201 "$(matricula "$ana" "$turma")"
confere "matrícula duplicada é recusada"                     409 "$(matricula "$ana" "$turma")"
confere "a compensação devolveu a vaga"                      1   "$(psql_turmas "SELECT ocupadas FROM turmas.turmas WHERE id = '$turma'")"
confere "segunda matrícula ocupa a última vaga"              201 "$(matricula "$bia" "$turma")"
confere "turma lotada vira 409 no monolito"                  409 "$(matricula "$(uuidgen)" "$turma")"
confere "turma inexistente vira 404 no monolito"             404 "$(matricula "$(uuidgen)" "$(uuidgen)")"

exit "$falhas"
