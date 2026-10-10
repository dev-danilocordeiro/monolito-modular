# AGENTS.md

Instruções para agentes de código (incluindo a linha de execução do Sentinela) que trabalham
neste repositório. Os comandos abaixo são os mesmos que o CI (`.github/workflows/ci.yml`) roda.

## Estrutura

Monorepo com quatro projetos independentes, um por linguagem. Cada um tem seu próprio build;
rode os comandos de dentro da pasta do projeto que você alterou.

| Pasta | Linguagem | Posts |
| --- | --- | --- |
| `go/` | Go (módulo `github.com/dev-danilocordeiro/monolito-modular/go`) | 3 |
| `java/` | Java 21, Gradle, Spring Modulith | 3, 4 e 5 |
| `kotlin/` | Kotlin, Gradle | 2 |
| `python/` | Python 3.12, Poetry, import-linter | 3 |

## Go (`go/`)

```bash
cd go
go vet ./...
go build ./...
go test ./...
./testes-negativos.sh   # garante que violações de fronteira continuam quebrando o build
```

- Fronteiras entre módulos: pacotes `internal/` de um módulo não são importados por outro; o
  `archtest/` verifica isso.
- Compatível com Go 1.23 (o CI roda 1.23 e stable): não use recursos de versões mais novas.

## Java (`java/`)

```bash
cd java
./gradlew test
./testes-negativos.sh   # verify do Spring Modulith
./e2e/passo-6.sh        # ponta a ponta com Docker (Postgres e Kafka); exige Docker
```

- As tags `passo-0` a `passo-6` precisam continuar verdes: não reescreva histórico nem mova tags.

## Kotlin (`kotlin/`)

```bash
cd kotlin
./gradlew test
```

## Python (`python/`)

```bash
cd python
poetry install
poetry run lint-imports   # contratos do import-linter
poetry run pytest -v
```

## Regras

- Todo bloco de código dos posts existe aqui e roda no CI: mudança de comportamento precisa de
  teste e não pode quebrar o exemplo do post correspondente.
- Não altere `.github/`, `CODEOWNERS` nem as tags dos passos.
