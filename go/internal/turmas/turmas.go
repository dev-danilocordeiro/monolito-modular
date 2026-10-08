// Package turmas é a API pública do módulo turmas.
package turmas

import (
	"errors"

	"github.com/dev-danilocordeiro/monolito-modular/go/internal/turmas/internal/store"
)

var ErrSemVaga = errors.New("turma sem vaga")

type Servico struct {
	store *store.Store
}

func New() *Servico {
	return &Servico{store: store.New()}
}

func (s *Servico) AbrirTurma(id string, capacidade int) {
	s.store.Salvar(store.Turma{ID: id, Capacidade: capacidade})
}

func (s *Servico) ReservarVaga(turmaID string) error {
	return s.store.Atualizar(turmaID, func(t *store.Turma) error {
		if t.Ocupadas >= t.Capacidade {
			return ErrSemVaga
		}
		t.Ocupadas++
		return nil
	})
}
