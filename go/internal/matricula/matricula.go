// Package matricula é a API pública do módulo matricula.
package matricula

import (
	"github.com/dev-danilocordeiro/monolito-modular/go/internal/matricula/internal/store"
	"github.com/dev-danilocordeiro/monolito-modular/go/internal/turmas"
)

// MatriculaConfirmada é o contrato publicado para outros módulos.
type MatriculaConfirmada struct {
	EstudanteID string
	TurmaID     string
	AnoLetivo   int
}

type Servico struct {
	turmas   *turmas.Servico
	store    *store.Store
	ouvintes []func(MatriculaConfirmada)
}

func New(t *turmas.Servico) *Servico {
	return &Servico{turmas: t, store: store.New()}
}

// AoConfirmar registra um ouvinte; a matrícula não sabe quem ele é.
func (s *Servico) AoConfirmar(ouvinte func(MatriculaConfirmada)) {
	s.ouvintes = append(s.ouvintes, ouvinte)
}

func (s *Servico) Matricular(estudanteID, turmaID string, anoLetivo int) error {
	if err := s.turmas.ReservarVaga(turmaID); err != nil {
		return err
	}
	s.store.Salvar(store.Matricula{EstudanteID: estudanteID, TurmaID: turmaID, AnoLetivo: anoLetivo})
	evento := MatriculaConfirmada{EstudanteID: estudanteID, TurmaID: turmaID, AnoLetivo: anoLetivo}
	for _, ouvinte := range s.ouvintes {
		ouvinte(evento)
	}
	return nil
}
