package com.example.allufinancas.data

import java.text.Normalizer
import java.util.Locale

/** Answers only the six documented intents from on-device fictional data. */
object LocalAssistant {
    fun answer(
        question: String,
        transactions: List<Transaction>,
        categories: List<Category>,
        budgets: List<Budget>,
        month: String = DemoSeed.MONTH,
    ): AssistantAnswer {
        val intent = normalize(question)
        val realtimeRequest = listOf("hoje", "agora", "tempo real", "atualizado", "sincronizado", "saldo atual")
            .any { it in intent }
        if (realtimeRequest) {
            return AssistantAnswer("Não consigo consultar dados em tempo real. Esta demonstração usa apenas o pacote fictício local.")
        }
        val namedMonth = listOf(
            "janeiro" to "01", "fevereiro" to "02", "marco" to "03", "abril" to "04",
            "maio" to "05", "junho" to "06", "julho" to "07", "agosto" to "08",
            "setembro" to "09", "set" to "09", "outubro" to "10", "novembro" to "11", "dezembro" to "12",
        ).firstOrNull { (name, _) -> name in intent.split(' ') }
        val mentionedYear = Regex("\\b(202[0-9])\\b").find(question)?.groupValues?.get(1)
        val explicitYearMonth = Regex("\\b(202[0-9])[-/](0[1-9]|1[0-2])\\b").find(question)
        val requestedDifferentMonth = namedMonth?.let { (_, number) ->
            !month.endsWith("-$number") || (mentionedYear != null && !month.startsWith("$mentionedYear-"))
        } == true || explicitYearMonth?.let { match ->
                "${match.groupValues[1]}-${match.groupValues[2]}" != month.take(7)
            } == true || (mentionedYear != null && explicitYearMonth == null && namedMonth == null)
        if (requestedDifferentMonth) {
            return AssistantAnswer("Só tenho dados fictícios de setembro de 2026 neste pacote. Não consigo responder sobre outro período.")
        }
        val selected = transactions.filter { it.date.startsWith(month) }
        val expenses = selected.filter { it.amountCents < 0 }
        val category = categories.firstOrNull { normalize(it.name) in intent }
        val source = "Cálculo local com lançamentos fictícios de $month."

        if (listOf("demo", "ficticio", "real", "banco", "fonte", "dados").any { it in intent }) {
            return AssistantAnswer("Todos os valores aqui são exemplos fictícios armazenados neste aparelho. " +
                "Não há conta bancária conectada, sincronização ou aconselhamento financeiro.")
        }

        if ("orcamento" in intent || "limite" in intent || "disponivel" in intent) {
            val budget = budgets.firstOrNull { it.categoryId == category?.id }
                ?: return AssistantAnswer("Para consultar um orçamento, inclua a categoria. " +
                    "Exemplo: quanto resta do orçamento de alimentação? $source")
            val spent = DemoAnalysis.categorySpentCents(selected, budget.categoryId, month)
            val remaining = budget.limitCents - spent
            val sources = expenses.filter { it.categoryId == budget.categoryId }.map { it.id }
            return AssistantAnswer(
                "${category?.name ?: "Categoria"}: limite ${DemoAnalysis.money(budget.limitCents)} menos " +
                    "${DemoAnalysis.money(spent)} em despesas = ${DemoAnalysis.money(remaining)} " +
                    "${if (remaining < 0) "acima do limite" else "disponíveis"}. $source",
                sources,
            )
        }

        if ("maior" in intent && ("gasto" in intent || "despesa" in intent || "compra" in intent)) {
            val largest = expenses.minByOrNull { it.amountCents }
                ?: return AssistantAnswer("Não há despesas fictícias neste período. $source")
            return AssistantAnswer(
                "A maior despesa foi ${largest.merchant}: ${DemoAnalysis.money(-largest.amountCents)} " +
                    "em ${largest.date}. $source",
                listOf(largest.id),
            )
        }

        if (listOf("recorrente", "assinatura", "mensalidade").any { it in intent }) {
            val recurring = expenses.filter { it.recurring }
            if (recurring.isEmpty()) return AssistantAnswer("Não há despesas recorrentes fictícias neste período. $source")
            return AssistantAnswer(
                "${recurring.size} despesas recorrentes somam " +
                    "${DemoAnalysis.money(recurring.sumOf { -it.amountCents })}: " +
                    recurring.joinToString { "${it.merchant} (${DemoAnalysis.money(-it.amountCents)})" } + ". $source",
                recurring.map { it.id },
            )
        }

        if (category != null && listOf("gasto", "gastei", "despesa", "total", "quanto").any { it in intent }) {
            val related = expenses.filter { it.categoryId == category.id }
            return AssistantAnswer(
                "${category.name}: ${DemoAnalysis.money(related.sumOf { -it.amountCents })} em " +
                    "${related.size} lançamentos fictícios. $source",
                related.map { it.id },
            )
        }

        if (listOf("gastei", "gasto", "despesa", "total do mes", "total mensal").any { it in intent }) {
            val total = DemoAnalysis.monthSummary(selected, month).expenseCents
            return AssistantAnswer(
                "As despesas do mês somam ${DemoAnalysis.money(total)} em ${expenses.size} lançamentos. $source",
                expenses.map { it.id },
            )
        }

        return AssistantAnswer("Só consigo calcular total do mês, total por categoria, maior despesa, " +
            "cobranças recorrentes e orçamento disponível usando esta demonstração local. " +
            "Experimente: quanto gastei neste mês?")
    }

    private fun normalize(value: String): String = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex(" +"), " ")
        .trim()
}
