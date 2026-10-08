package escola.matricula

import java.time.LocalDate
import java.util.UUID

@JvmInline value class EstudanteId(val valor: UUID)

data class Estudante(
    val id: EstudanteId,
    val nome: String,
    val dataNascimento: LocalDate,
    val responsaveisLegais: List<ResponsavelLegal>,
)

data class ResponsavelLegal(val nome: String, val cpf: String, val parentesco: String)
