package main

import (
	"errors"
	"log/slog"
	"net/http"
	"os"
	"strconv"
	"time"

	"github.com/dev-danilocordeiro/monolito-modular/go/internal/financeiro"
	"github.com/dev-danilocordeiro/monolito-modular/go/internal/matricula"
	"github.com/dev-danilocordeiro/monolito-modular/go/internal/turmas"
)

func main() {
	t := turmas.New()
	m := matricula.New(t)
	f := financeiro.New()
	m.AoConfirmar(f.AoConfirmarMatricula)

	mux := http.NewServeMux()

	mux.HandleFunc("POST /turmas/{id}", abrirTurmaHandler(t))

	mux.HandleFunc("POST /turmas/{id}/matriculas/{estudante}", matricularHandler(m.Matricular, anoLetivo()))

	slog.Info("escutando", "endereco", ":8080")
	if err := novoServidor(":8080", mux).ListenAndServe(); err != nil {
		slog.Error("servidor parou", "erro", err)
		os.Exit(1)
	}
}

// anoLetivo vem de ANO_LETIVO (padrão 2027); erros desconhecidos da matrícula viram 500 genérico.
func anoLetivo() int {
	if ano, err := strconv.Atoi(os.Getenv("ANO_LETIVO")); err == nil && ano > 0 {
		return ano
	}
	return 2027
}

func matricularHandler(matricular func(estudanteID, turmaID string, ano int) error, ano int) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		err := matricular(r.PathValue("estudante"), r.PathValue("id"), ano)
		switch {
		case err == nil:
			w.WriteHeader(http.StatusCreated)
		case errors.Is(err, turmas.ErrSemVaga):
			http.Error(w, err.Error(), http.StatusConflict)
		case errors.Is(err, turmas.ErrNaoEncontrada):
			http.Error(w, err.Error(), http.StatusNotFound)
		default:
			slog.Error("falha ao matricular", "erro", err)
			http.Error(w, "erro interno", http.StatusInternalServerError)
		}
	}
}

// abrirTurmaHandler valida a capacidade na borda: precisa ser um inteiro > 0.
func abrirTurmaHandler(t *turmas.Servico) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		capacidade, err := strconv.Atoi(r.URL.Query().Get("capacidade"))
		if err != nil || capacidade <= 0 {
			http.Error(w, "capacidade inválida", http.StatusBadRequest)
			return
		}
		t.AbrirTurma(r.PathValue("id"), capacidade)
		w.WriteHeader(http.StatusCreated)
	}
}

// novoServidor monta o http.Server da API com timeouts e limite de cabeçalho,
// para que clientes lentos (Slowloris) não segurem conexões indefinidamente.
func novoServidor(addr string, handler http.Handler) *http.Server {
	return &http.Server{
		Addr:              addr,
		Handler:           handler,
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       10 * time.Second,
		WriteTimeout:      10 * time.Second,
		IdleTimeout:       60 * time.Second,
		MaxHeaderBytes:    1 << 20,
	}
}
