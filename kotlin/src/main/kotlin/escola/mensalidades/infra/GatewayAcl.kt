package escola.mensalidades.infra

import escola.mensalidades.SituacaoMensalidade

// Modelo do fornecedor, exatamente como chega no webhook
data class GatewayChargeWebhook(val id: String, val status: String, val paidAmountCents: Long?)

object GatewayAcl {
    fun traduzir(webhook: GatewayChargeWebhook): SituacaoMensalidade =
        when (webhook.status) {
            "PAID", "RECEIVED", "CONFIRMED" -> SituacaoMensalidade.Paga
            "OVERDUE" -> SituacaoMensalidade.Atrasada
            "REFUNDED", "CHARGEBACK" -> SituacaoMensalidade.Estornada
            "PENDING" -> SituacaoMensalidade.EmAberto
            else -> SituacaoMensalidade.Desconhecida(webhook.status)
        }
}
