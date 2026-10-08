#!/usr/bin/env bash
# Prova que o compilador e o archtest reprovam violações de fronteira.
# Cada caso roda numa cópia do módulo Go, então o código real não é tocado.
set -euo pipefail

cd "$(dirname "$0")"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
MOD=github.com/dev-danilocordeiro/monolito-modular/go
falhas=0

# caso <nome> <pacote onde injetar> <import> <comando> <trecho esperado na saída>
caso() {
  local nome=$1 pacote=$2 importa=$3 comando=$4 esperado=$5
  local dir="$TMP/$nome"
  cp -r . "$dir"
  printf 'package %s\n\nimport _ "%s"\n' "$(basename "$pacote")" "$MOD/$importa" > "$dir/$pacote/violacao.go"

  local saida
  if saida=$(cd "$dir" && eval "$comando" 2>&1); then
    echo "FALHOU  $nome: '$comando' deveria reprovar e passou"
    falhas=$((falhas + 1))
  elif ! grep -qF -- "$esperado" <<<"$saida"; then
    echo "FALHOU  $nome: reprovou, mas sem '$esperado'. Saída:"
    echo "$saida"
    falhas=$((falhas + 1))
  else
    echo "ok      $nome"
  fi
}

caso matricula-importa-store-de-turmas \
  internal/matricula internal/turmas/internal/store \
  "go build ./..." "use of internal package $MOD/internal/turmas/internal/store not allowed"

caso turmas-importa-financeiro-gera-ciclo \
  internal/turmas internal/financeiro \
  "go build ./..." "import cycle not allowed"

caso financeiro-importa-turmas-fora-do-grafo \
  internal/financeiro internal/turmas \
  "go test ./archtest/" "financeiro -> turmas não está no grafo permitido"

# O build passa nesse último caso: só o archtest enxerga a aresta proibida.
(cd "$TMP/financeiro-importa-turmas-fora-do-grafo" && go build ./...) \
  && echo "ok      financeiro-importa-turmas compila (só o archtest pega)" \
  || { echo "FALHOU  financeiro-importa-turmas deveria compilar"; falhas=$((falhas + 1)); }

exit "$falhas"
