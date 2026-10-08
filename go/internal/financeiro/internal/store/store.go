// Package store guarda os estudantes que o financeiro acompanha. Só o módulo financeiro enxerga este pacote.
package store

import "sync"

type Store struct {
	mu         sync.Mutex
	estudantes map[string]struct{}
}

func New() *Store {
	return &Store{estudantes: map[string]struct{}{}}
}

func (s *Store) Registrar(estudanteID string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.estudantes[estudanteID] = struct{}{}
}

func (s *Store) Total() int {
	s.mu.Lock()
	defer s.mu.Unlock()
	return len(s.estudantes)
}
