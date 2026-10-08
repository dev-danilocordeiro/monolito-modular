package escola.pedagogico

import java.util.UUID

// Mesmo id da matrícula, modelo completamente diferente
@JvmInline value class AprendizId(val valor: UUID)

data class Aprendiz(
    val id: AprendizId,
    val nomeSocial: String,
    val avaliacoes: List<Avaliacao>,
    val faltas: Int,
) {
    // Sem avaliações não existe média: null, em vez do NaN de 0.0 / 0.0
    fun mediaPonderada(): Double? =
        if (avaliacoes.isEmpty()) null
        else avaliacoes.sumOf { it.nota * it.peso } / avaliacoes.sumOf { it.peso }
}

data class Avaliacao(val componente: String, val nota: Double, val peso: Double)
