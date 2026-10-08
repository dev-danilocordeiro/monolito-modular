#!/usr/bin/env bash
# Passo 5 de ponta a ponta: parte do estado do passo 4 (serviço no banco do monolito),
# move o schema com scripts/mover-schema-turmas.sh e confere que nada se perdeu.
cd "$(dirname "$0")/.."
source e2e/comum.sh
trap limpa EXIT

psql_turmas() { docker compose exec -T postgres-turmas psql -U turmas -d turmas -tAc "$1"; }
leituras_do_schema_antigo() {
  psql_escola "SELECT pg_stat_clear_snapshot()" > /dev/null
  psql_escola "SELECT coalesce(sum(seq_scan + coalesce(idx_scan, 0)), 0) FROM pg_stat_user_tables WHERE schemaname = 'turmas'"
}

./gradlew bootJar -q
docker compose up -d --build --wait postgres postgres-turmas kafka > /dev/null
# Estado do passo 4: o serviço ainda usa o schema turmas do banco do monolito
TURMAS_DB_URL=jdbc:postgresql://postgres:5432/escola TURMAS_DB_USUARIO=escola TURMAS_DB_SENHA=escola \
  docker compose up -d --build servico-turmas > /dev/null
espera_http "http://localhost:$SERVICO_TURMAS_PORTA/turmas"
sobe_monolito remoto

turma=$(uuidgen)
psql_escola "INSERT INTO turmas.turmas (id, ano_letivo, capacidade) VALUES ('$turma', 2027, 3)" > /dev/null
confere "matrícula antes do corte"                      201 "$(matricula "$(uuidgen)" "$turma")"

./scripts/mover-schema-turmas.sh
espera_http "http://localhost:$SERVICO_TURMAS_PORTA/turmas"

confere "turma e reserva chegaram ao banco do serviço"  "1|1" "$(psql_turmas "SELECT (SELECT ocupadas FROM turmas.turmas WHERE id = '$turma') || '|' || (SELECT count(*) FROM turmas.reservas WHERE turma_id = '$turma')")"
confere "histórico do Flyway veio junto"                2   "$(psql_turmas "SELECT count(*) FROM turmas.flyway_schema_history WHERE version IS NOT NULL AND success")"

sleep 2
antes=$(leituras_do_schema_antigo)
confere "matrícula depois do corte"                     201 "$(matricula "$(uuidgen)" "$turma")"
confere "a vaga foi ocupada no banco novo"              2   "$(psql_turmas "SELECT ocupadas FROM turmas.turmas WHERE id = '$turma'")"
sleep 2
confere "ninguém mais lê o schema antigo"               "$antes" "$(leituras_do_schema_antigo)"
confere "a cópia antiga ficou congelada"                1   "$(psql_escola "SELECT ocupadas FROM turmas.turmas WHERE id = '$turma'")"

exit "$falhas"
