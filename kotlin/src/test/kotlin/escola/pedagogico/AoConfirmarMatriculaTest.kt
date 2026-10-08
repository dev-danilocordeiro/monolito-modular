package escola.pedagogico

import escola.matricula.api.MatriculaConfirmada
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AoConfirmarMatriculaTest {

    private class AprendizesEmMemoria : Aprendizes {
        val salvos = mutableListOf<Aprendiz>()
        override fun salvar(aprendiz: Aprendiz) {
            salvos += aprendiz
        }
    }

    @Test
    fun `matricula confirmada vira um aprendiz com o mesmo id e sem historico`() {
        val aprendizes = AprendizesEmMemoria()
        val estudanteId = UUID.randomUUID()

        AoConfirmarMatricula(aprendizes).handle(
            MatriculaConfirmada(estudanteId, UUID.randomUUID(), 2027, "Ana")
        )

        assertEquals(listOf(Aprendiz(AprendizId(estudanteId), "Ana", emptyList(), 0)), aprendizes.salvos)
    }

    @Test
    fun `media ponderada usa o peso de cada avaliacao`() {
        val aprendiz = Aprendiz(
            AprendizId(UUID.randomUUID()),
            "Ana",
            listOf(Avaliacao("Matemática", 6.0, 1.0), Avaliacao("Matemática", 9.0, 2.0)),
            0,
        )

        assertEquals(8.0, aprendiz.mediaPonderada())
    }

    @Test
    fun `aprendiz sem avaliacoes ainda nao tem media`() {
        val aprendiz = Aprendiz(AprendizId(UUID.randomUUID()), "Ana", emptyList(), 0)

        assertNull(aprendiz.mediaPonderada())
    }
}
