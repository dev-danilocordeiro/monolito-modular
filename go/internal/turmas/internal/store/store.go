// Package store guarda as turmas. Só o módulo turmas enxerga este pacote.
package store

import "sync"

type Turma struct {
	ID         string
	Capacidade int
	Ocupadas   int
}

type Store struct {
	mu     sync.Mutex
	turmas map[string]*Turma
}

func New() *Store {
	return &Store{turmas: map[string]*Turma{}}
}

func (s *Store) Salvar(t Turma) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.turmas[t.ID] = &t
}

// Atualizar aplica fn na turma sob o lock do store.
func (s *Store) Atualizar(id string, fn func(*Turma) error) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	t, ok := s.turmas[id]
	if !ok {
		return ErrNaoEncontrada
	}
	return fn(t)
}
