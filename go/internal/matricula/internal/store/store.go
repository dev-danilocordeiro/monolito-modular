// Package store guarda as matrículas. Só o módulo matricula enxerga este pacote.
package store

import "sync"

type Matricula struct {
	EstudanteID string
	TurmaID     string
	AnoLetivo   int
}

type Store struct {
	mu         sync.Mutex
	matriculas []Matricula
}

func New() *Store {
	return &Store{}
}

func (s *Store) Salvar(m Matricula) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.matriculas = append(s.matriculas, m)
}
