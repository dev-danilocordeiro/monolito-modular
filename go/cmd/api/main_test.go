package main

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/dev-danilocordeiro/monolito-modular/go/internal/turmas"
)

func TestNovoServidorDefineTimeouts(t *testing.T) {
	mux := http.NewServeMux()
	srv := novoServidor(":8080", mux)
	if srv.Addr != ":8080" || srv.Handler != mux {
		t.Fatalf("Addr/Handler errados: %q, %v", srv.Addr, srv.Handler)
	}
	if srv.ReadHeaderTimeout <= 0 || srv.ReadTimeout <= 0 || srv.WriteTimeout <= 0 || srv.IdleTimeout <= 0 {
		t.Errorf("timeouts precisam ser > 0: %+v", srv)
	}
	if srv.MaxHeaderBytes <= 0 {
		t.Errorf("MaxHeaderBytes = %d, quer > 0", srv.MaxHeaderBytes)
	}
}

func TestAbrirTurmaValidaCapacidade(t *testing.T) {
	mux := http.NewServeMux()
	mux.HandleFunc("POST /turmas/{id}", abrirTurmaHandler(turmas.New()))
	casos := map[string]int{"0": 400, "-1": 400, "abc": 400, "": 400, "1": 201, "30": 201, "1000000": 201}
	for capacidade, quer := range casos {
		req := httptest.NewRequest(http.MethodPost, "/turmas/t1?capacidade="+capacidade, nil)
		rec := httptest.NewRecorder()
		mux.ServeHTTP(rec, req)
		if rec.Code != quer {
			t.Errorf("capacidade=%q: status %d, quer %d", capacidade, rec.Code, quer)
		}
	}
}
