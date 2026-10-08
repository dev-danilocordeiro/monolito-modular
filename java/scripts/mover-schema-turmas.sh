#!/usr/bin/env bash
# Passo 5: move o schema turmas do banco do monolito para o banco do servico-turmas
# com replicação lógica do Postgres, parando as escritas só durante o corte.
#
# Pré-condições: postgres com wal_level=logical, servico-turmas rodando contra o banco
# do monolito (TURMAS_DB_URL) e o monolito em modo remoto (ninguém mais escreve em turmas.*).
set -euo pipefail
cd "$(dirname "$0")/.."

origem()  { docker compose exec -T postgres        psql -v ON_ERROR_STOP=1 -U escola -d escola -tAc "$1"; }
destino() { docker compose exec -T postgres-turmas psql -v ON_ERROR_STOP=1 -U turmas -d turmas -tAc "$1"; }

echo "1/6 copiando a estrutura do schema (sem dados)"
docker compose exec -T postgres pg_dump -U escola -d escola --schema-only --schema=turmas --no-owner --no-privileges \
  | docker compose exec -T postgres-turmas psql -q -v ON_ERROR_STOP=1 -U turmas -d turmas > /dev/null

echo "2/6 publicando o schema na origem e assinando no destino"
origem  "CREATE PUBLICATION turmas_para_servico FOR TABLES IN SCHEMA turmas" > /dev/null
destino "CREATE SUBSCRIPTION turmas_do_monolito
         CONNECTION 'host=postgres port=5432 dbname=escola user=escola password=escola'
         PUBLICATION turmas_para_servico" > /dev/null

echo "3/6 esperando a cópia inicial"
until [[ "$(destino "SELECT count(*) FROM pg_subscription_rel WHERE srsubstate <> 'r'")" == 0 ]]; do sleep 1; done

echo "4/6 parando as escritas (janela curta começa aqui)"
docker compose stop servico-turmas > /dev/null
alvo=$(origem "SELECT pg_current_wal_lsn()")

echo "5/6 esperando a réplica alcançar $alvo"
until [[ "$(origem "SELECT confirmed_flush_lsn >= '$alvo' FROM pg_replication_slots WHERE slot_name = 'turmas_do_monolito'")" == t ]]; do sleep 1; done
for tabela in $(origem "SELECT tablename FROM pg_tables WHERE schemaname = 'turmas' ORDER BY 1"); do
  o=$(origem "SELECT count(*) FROM turmas.$tabela"); d=$(destino "SELECT count(*) FROM turmas.$tabela")
  [[ "$o" == "$d" ]] || { echo "turmas.$tabela diverge: origem=$o destino=$d"; exit 1; }
done
destino "DROP SUBSCRIPTION turmas_do_monolito" > /dev/null
origem  "DROP PUBLICATION turmas_para_servico" > /dev/null
# Replicação lógica não copia sequências. Aqui não há nenhuma: todos os ids são UUID.

echo "6/6 subindo o serviço apontando para o banco próprio"
TURMAS_DB_URL=jdbc:postgresql://postgres-turmas:5432/turmas TURMAS_DB_USUARIO=turmas TURMAS_DB_SENHA=turmas \
  docker compose up -d servico-turmas > /dev/null
echo "pronto. O schema turmas do monolito ficou congelado; confira com pg_stat_user_tables antes de removê-lo."
