package escola.mensalidades.infra

import escola.mensalidades.SituacaoMensalidade
import kotlin.test.Test
import kotlin.test.assertEquals

class GatewayAclTest {
    @Test
    fun `status desconhecido nao quebra a traducao`() {
        val situacao = GatewayAcl.traduzir(GatewayChargeWebhook("ch_1", "AWAITING_RISK_ANALYSIS", null))
        assertEquals(SituacaoMensalidade.Desconhecida("AWAITING_RISK_ANALYSIS"), situacao)
    }

    @Test
    fun `todo status conhecido do gateway vira uma situacao do contexto de mensalidades`() {
        val esperado = mapOf(
            "PAID" to SituacaoMensalidade.Paga,
            "RECEIVED" to SituacaoMensalidade.Paga,
            "CONFIRMED" to SituacaoMensalidade.Paga,
            "OVERDUE" to SituacaoMensalidade.Atrasada,
            "REFUNDED" to SituacaoMensalidade.Estornada,
            "CHARGEBACK" to SituacaoMensalidade.Estornada,
            "PENDING" to SituacaoMensalidade.EmAberto,
        )

        esperado.forEach { (status, situacao) ->
            assertEquals(situacao, GatewayAcl.traduzir(GatewayChargeWebhook("ch_1", status, null)), "status $status")
        }
    }

    @Test
    fun `status com caixa diferente nao e reconhecido e fica registrado como desconhecido`() {
        val situacao = GatewayAcl.traduzir(GatewayChargeWebhook("ch_1", "paid", 1000))
        assertEquals(SituacaoMensalidade.Desconhecida("paid"), situacao)
    }
}
