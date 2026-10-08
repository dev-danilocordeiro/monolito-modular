# monolito-modular

Repositório companheiro da série **Monolito Modular na Prática**, publicada em
[danilocordeiro.dev](https://danilocordeiro.dev). Todo bloco de código dos posts 2 a 5 existe
aqui e roda no CI. O post 1 usa outro projeto, o
[smarthome-hub](https://github.com/dev-danilocordeiro/smarthome-hub).

O domínio é uma plataforma escolar fictícia, com os módulos `matricula`, `turmas`,
`pedagogico`, `mensalidades` e `financeiro`. As regras de negócio são só as necessárias para
os exemplos funcionarem.

| Post | Assunto | Pasta |
| --- | --- | --- |
| [2. DDD estratégico](docs/posts/02-ddd-estrategico-subdominios-bounded-contexts.mdx) | Um modelo por contexto, Published Language, ACL | [`kotlin/`](kotlin) |
| [3. Fronteiras no CI](docs/posts/03-fronteiras-de-modulos-no-ci.mdx) | import-linter, Spring Modulith, `internal/` no Go | [`python/`](python), [`java/`](java) (tag `passo-0`), [`go/`](go) |
| [4. Comunicação entre módulos](docs/posts/04-comunicacao-entre-modulos.mdx) | Eventos, registro de publicações, um schema por módulo | [`java/`](java) (tag `passo-0`) |
| [5. Extraindo um módulo](docs/posts/05-extraindo-modulo-para-servico.mdx) | Extração de `turmas` em seis passos | [`java/`](java) (tags `passo-1` a `passo-6`) |

## Pré-requisitos

Só os da pasta que você for rodar:

- **kotlin/** e **java/**: JDK 21. O Gradle vem pelo wrapper.
- **java/**: Docker com Compose v2. Os testes sobem PostgreSQL e Kafka com Testcontainers.
- **python/**: Python 3.12+ e Poetry 2.2+.
- **go/**: Go 1.23+.

## kotlin/ (post 2)

```bash
cd kotlin
./gradlew test
```

Um pacote por contexto (`escola.matricula`, `escola.pedagogico`, `escola.mensalidades`), cada
um com o seu modelo de "aluno": `Estudante` e `Aprendiz`. O evento `MatriculaConfirmada` é a
Published Language entre os dois, e a `GatewayAcl` traduz os status do gateway de pagamento
para `SituacaoMensalidade`.

## python/ (post 3)

```bash
cd python
poetry install
poetry run lint-imports   # os contratos do .importlinter
poetry run pytest         # inclui o teste que injeta violações
```

`tests/test_contratos.py` copia o pacote para um diretório temporário, injeta cada tipo de
violação (domínio de outro módulo, domínio importando framework, `turmas` importando
`matricula`, camada de baixo importando a de cima) e confere que o contrato certo quebra.

## go/ (post 3)

```bash
cd go
go build ./...
go test ./...              # inclui o archtest com go list
./testes-negativos.sh      # prova que compilador e archtest reprovam violações
```

## java/ (posts 3, 4 e 5)

Spring Boot 4.1.1 e Spring Modulith 2.1.1, com JDK 21, PostgreSQL 18 e Kafka 4.2.

O `main` está no fim da extração do post 5. Para o código dos posts 3 e 4, ou para
acompanhar a extração passo a passo, use as tags:

| Tag | Estado |
| --- | --- |
| `passo-0` | Monolito modular dos posts 3 e 4: `matricula`, `turmas`, `pedagogico`, registro de publicações JDBC |
| `passo-1` | `TurmasApi` vira a porta; a implementação atual vira `TurmasLocal` |
| `passo-2` | Eventos externalizados no Kafka com `@Externalized` |
| `passo-3` | App `servico-turmas` no mesmo repositório, usando o mesmo schema |
| `passo-4` | `TurmasRemoto` atrás de `escola.turmas.modo`, com chave de idempotência e compensação |
| `passo-5` | Schema `turmas` num banco próprio, movido com replicação lógica |
| `passo-6` | Módulo removido do monolito |

```bash
git checkout passo-0       # ou passo-1 ... passo-6; volte com git checkout main
cd java
./gradlew test             # testes de unidade e integração (Testcontainers)
./testes-negativos.sh      # prova que o verify() do Spring Modulith reprova violações
```

Para subir a aplicação contra o compose:

```bash
docker compose up -d postgres kafka
./gradlew bootRun
```

A partir do `passo-3`, o `servico-turmas` também sobe pelo compose
(`docker compose up -d servico-turmas`). Nos passos 4 a 6, `e2e/passo-*.sh` sobe tudo e
exercita a extração de ponta a ponta, inclusive a compensação quando a matrícula falha depois
da reserva remota.

Se alguma porta já estiver ocupada na sua máquina, troque pelo ambiente:
`SERVICO_TURMAS_PORTA` (padrão 8081) e `POSTGRES_TURMAS_PORTA` (padrão 5433).

## CI

O [workflow](.github/workflows/ci.yml) tem um job por linguagem. Além dos testes normais, cada
um roda os **testes negativos**: injeta uma violação de fronteira e confirma que a ferramenta
reprova. O job `passos` faz checkout de cada tag de `passo-0` a `passo-5` e roda os testes e o
ponta a ponta daquele passo, para que nenhum passo do post 5 fique quebrado no histórico.
