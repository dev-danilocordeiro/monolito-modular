# Funções usadas pelos testes de ponta a ponta. Rode a partir da pasta java/.
set -euo pipefail

export SERVICO_TURMAS_PORTA="${SERVICO_TURMAS_PORTA:-18081}"
MONOLITO_PORTA="${MONOLITO_PORTA:-18080}"
MONOLITO_PID=""
falhas=0

psql_escola() { docker compose exec -T postgres psql -U escola -d escola -tAc "$1"; }

espera_http() {   # espera_http <url> : qualquer resposta HTTP serve
  for _ in $(seq 1 90); do
    curl -s -o /dev/null "$1" && return 0
    sleep 1
  done
  echo "timeout esperando $1"; return 1
}

sobe_monolito() {   # sobe_monolito <modo>
  para_monolito
  SERVER_PORT=$MONOLITO_PORTA ESCOLA_TURMAS_MODO=$1 ESCOLA_TURMAS_URL="http://localhost:$SERVICO_TURMAS_PORTA" \
    java -jar build/libs/escola.jar > "build/monolito-$1.log" 2>&1 &
  MONOLITO_PID=$!
  espera_http "http://localhost:$MONOLITO_PORTA/matriculas"
}

para_monolito() {
  if [[ -n "$MONOLITO_PID" ]]; then kill "$MONOLITO_PID" 2>/dev/null || true; wait "$MONOLITO_PID" 2>/dev/null || true; MONOLITO_PID=""; fi
}

matricula() {   # matricula <estudante> <turma> : imprime o status HTTP
  curl -s -o /dev/null -w '%{http_code}' -X POST "http://localhost:$MONOLITO_PORTA/matriculas" \
    -H 'Content-Type: application/json' \
    -d "{\"estudanteId\":\"$1\",\"turmaId\":\"$2\",\"anoLetivo\":2027,\"nomeSocial\":\"Teste\"}"
}

confere() {   # confere <descrição> <esperado> <obtido>
  if [[ "$2" == "$3" ]]; then echo "ok      $1"; else echo "FALHOU  $1: esperado '$2', obtido '$3'"; falhas=$((falhas + 1)); fi
}

limpa() {
  para_monolito
  if [[ $falhas -gt 0 ]]; then docker compose logs --tail 80 servico-turmas || true; tail -80 build/monolito-*.log || true; fi
  docker compose down -v --remove-orphans > /dev/null 2>&1 || true
}
