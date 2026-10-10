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

	mux.HandleFunc("POST /turmas/{id}", func(w http.ResponseWriter, r *http.Request) {
		capacidade, err := strconv.Atoi(r.URL.Query().Get("capacidade"))
		if err != nil {
			http.Error(w, "capacidade inválida", http.StatusBadRequest)
			return
		}
		t.AbrirTurma(r.PathValue("id"), capacidade)
		w.WriteHeader(http.StatusCreated)
	})

	mux.HandleFunc("POST /turmas/{id}/matriculas/{estudante}", func(w http.ResponseWriter, r *http.Request) {
		err := m.Matricular(r.PathValue("estudante"), r.PathValue("id"), 2027)
		switch {
		case errors.Is(err, turmas.ErrSemVaga):
			http.Error(w, err.Error(), http.StatusConflict)
		case err != nil:
			http.Error(w, err.Error(), http.StatusNotFound)
		default:
			w.WriteHeader(http.StatusCreated)
		}
	})

	slog.Info("escutando", "endereco", ":8080")
	if err := novoServidor(":8080", mux).ListenAndServe(); err != nil {
		slog.Error("servidor parou", "erro", err)
		os.Exit(1)
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
