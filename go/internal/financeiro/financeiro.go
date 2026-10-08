// Package financeiro é a API pública do módulo financeiro.
package financeiro

import (
	"github.com/dev-danilocordeiro/monolito-modular/go/internal/financeiro/internal/store"
	"github.com/dev-danilocordeiro/monolito-modular/go/internal/matricula"
)

type Servico struct {
	store *store.Store
}

func New() *Servico {
	return &Servico{store: store.New()}
}

// AoConfirmarMatricula guarda a cópia local do que o financeiro precisa.
func (s *Servico) AoConfirmarMatricula(evento matricula.MatriculaConfirmada) {
	s.store.Registrar(evento.EstudanteID)
}

func (s *Servico) EstudantesAcompanhados() int {
	return s.store.Total()
}
