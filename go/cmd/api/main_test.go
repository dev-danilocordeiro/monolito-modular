package main

import (
	"net/http"
	"testing"
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
