package escola.matricula.api

import java.util.UUID

// Contrato publicado: mudanças aqui são versionadas, nunca quebradas
data class MatriculaConfirmada(
    val estudanteId: UUID,
    val turmaId: UUID,
    val anoLetivo: Int,
    val nomeSocial: String,
)
