package archtest

import (
	"os/exec"
	"slices"
	"strings"
	"testing"
)

const raiz = "github.com/dev-danilocordeiro/monolito-modular/go/internal/"

// Grafo permitido entre módulos. Qualquer aresta fora daqui reprova.
var permitido = map[string][]string{
	"matricula":  {"turmas"},
	"turmas":     {},
	"financeiro": {"matricula"},
}

func moduloDe(pkg string) (string, bool) {
	resto, ok := strings.CutPrefix(pkg, raiz)
	if !ok {
		return "", false
	}
	nome, _, _ := strings.Cut(resto, "/")
	return nome, true
}

func TestGrafoDeModulos(t *testing.T) {
	cmd := exec.Command("go", "list", "-f", `{{.ImportPath}}{{range .Imports}} {{.}}{{end}}`, "./...")
	cmd.Dir = ".."
	out, err := cmd.Output()
	if err != nil {
		t.Fatalf("go list: %v", err)
	}

	for _, linha := range strings.Split(strings.TrimSpace(string(out)), "\n") {
		campos := strings.Fields(linha)
		origem, ok := moduloDe(campos[0])
		if !ok {
			continue
		}
		for _, imp := range campos[1:] {
			destino, ok := moduloDe(imp)
			if !ok || destino == origem {
				continue
			}
			if !slices.Contains(permitido[origem], destino) {
				t.Errorf("%s importa %s (%s -> %s não está no grafo permitido)", campos[0], imp, origem, destino)
			}
		}
	}

	for modulo := range permitido {
		if _, err := exec.Command("go", "list", "../internal/"+modulo).Output(); err != nil {
			t.Errorf("módulo %q declarado no grafo mas não existe", modulo)
		}
	}
}
