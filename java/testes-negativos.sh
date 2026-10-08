#!/usr/bin/env bash
# Prova que o ModularidadeTest (verify do Spring Modulith) reprova violações de fronteira.
# Cada caso roda numa cópia do projeto, então o código real não é tocado.
set -euo pipefail

cd "$(dirname "$0")"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
falhas=0

# caso <nome> <trecho esperado no relatório> ; os arquivos da violação vêm de violacao_<nome>
caso() {
  local nome=$1 esperado=$2
  local dir="$TMP/$nome"
  mkdir -p "$dir"
  tar --exclude=./build --exclude=./.gradle --exclude=./servico-turmas/build -cf - . | (cd "$dir" && tar xf -)
  "violacao_$nome" "$dir/src/main/java/escola"

  local saida
  if saida=$(cd "$dir" && ./gradlew test --tests escola.ModularidadeTest --offline -q 2>&1); then
    echo "FALHOU  $nome: o verify() deveria reprovar e passou"
    falhas=$((falhas + 1))
  elif ! grep -q "$esperado" "$dir"/build/test-results/test/TEST-escola.ModularidadeTest.xml 2>/dev/null; then
    echo "FALHOU  $nome: reprovou, mas o relatório não contém '$esperado'. Saída:"
    echo "$saida" | tail -30
    falhas=$((falhas + 1))
  else
    echo "ok      $nome"
  fi
}

# matricula usa um tipo de turmas.internal (que precisa ser público para compilar)
violacao_matricula-usa-interno-de-turmas() {
  cat > "$1/turmas/internal/Exposto.java" <<'JAVA'
package escola.turmas.internal;

public class Exposto {}
JAVA
  cat > "$1/matricula/Violacao.java" <<'JAVA'
package escola.matricula;

class Violacao {
    escola.turmas.internal.Exposto atalho;
}
JAVA
}

# matricula depende de um módulo que não está em allowedDependencies (e não forma ciclo)
violacao_matricula-depende-de-modulo-nao-declarado() {
  cat > "$1/infra/Utilitario.java" <<'JAVA'
package escola.infra;

public class Utilitario {}
JAVA
  cat > "$1/matricula/Violacao.java" <<'JAVA'
package escola.matricula;

class Violacao {
    escola.infra.Utilitario atalho;
}
JAVA
}

# turmas passa a depender de matricula: ciclo matricula -> turmas -> matricula
violacao_ciclo-entre-turmas-e-matricula() {
  cat > "$1/turmas/Violacao.java" <<'JAVA'
package escola.turmas;

class Violacao {
    escola.matricula.MatriculaConfirmada atalho;
}
JAVA
}

caso matricula-usa-interno-de-turmas "non-exposed type escola.turmas.internal.Exposto"
caso matricula-depende-de-modulo-nao-declarado "Allowed targets: turmas"
caso ciclo-entre-turmas-e-matricula "Cycle detected"

exit "$falhas"
