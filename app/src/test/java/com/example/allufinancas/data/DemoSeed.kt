package com.example.allufinancas.data

/** Seed-shaped records exist only as deterministic unit-test fixtures. */
object DemoSeed {
  const val MONTH = "2026-09"
  const val GENERATED_DATE = "2026-09-23"

  val accounts =
      listOf(
          AccountSummary("conta-demo", "Conta de exemplo", "Conta corrente fictícia", 1_489_075),
          AccountSummary("reserva-demo", "Reserva de exemplo", "Reserva fictícia", 562_000),
          AccountSummary("cartao-demo", "Cartão de exemplo", "Fatura fictícia", -128_000),
      )
  val categories = DefaultCategories.all
  val transactions =
      listOf(
          Transaction(
              "t01",
              "conta-demo",
              "Empresa Exemplo",
              "Salário fictício",
              820_000,
              "2026-09-05",
              "receita",
          ),
          Transaction(
              "t02",
              "conta-demo",
              "Mercado Exemplo",
              "Compra da semana",
              -35_642,
              "2026-09-21",
              "alimentacao",
          ),
          Transaction(
              "t03",
              "conta-demo",
              "Café Exemplo",
              "Café",
              -1_890,
              "2026-09-20",
              "alimentacao",
          ),
          Transaction(
              "t04",
              "cartao-demo",
              "Farmácia Exemplo",
              "Compra de exemplo",
              -4_780,
              "2026-09-19",
              "saude",
          ),
          Transaction(
              "t05",
              "cartao-demo",
              "Farmácia Exemplo",
              "Compra de exemplo",
              -4_780,
              "2026-09-19",
              "saude",
          ),
          Transaction(
              "t06",
              "cartao-demo",
              "Vídeo Exemplo",
              "Assinatura mensal",
              -3_990,
              "2026-09-17",
              "assinaturas",
              recurring = true,
          ),
          Transaction(
              "t07",
              "conta-demo",
              "Transporte Exemplo",
              "Corrida",
              -2_890,
              "2026-09-15",
              "",
          ),
          Transaction(
              "t08",
              "cartao-demo",
              "Loja Exemplo",
              "Notebook fictício",
              -12_990,
              "2026-09-12",
              "compras",
              3,
              10,
          ),
          Transaction(
              "t09",
              "conta-demo",
              "Celular Exemplo",
              "Plano mensal",
              -5_490,
              "2026-09-10",
              "assinaturas",
              recurring = true,
          ),
          Transaction(
              "t10",
              "conta-demo",
              "Mercado Exemplo",
              "Compra do mês",
              -28_010,
              "2026-09-08",
              "alimentacao",
          ),
          Transaction(
              "t11",
              "conta-demo",
              "Moradia Exemplo",
              "Aluguel fictício",
              -180_000,
              "2026-09-07",
              "casa",
              recurring = true,
          ),
          Transaction(
              "t12",
              "cartao-demo",
              "Loja Exemplo",
              "Compra de teste",
              -7_250,
              "2026-08-26",
              "compras",
          ),
      )
  val budgets =
      listOf(
          Budget("alimentacao", 50_000),
          Budget("assinaturas", 12_000),
          Budget("transporte", 30_000),
      )
  val goals = listOf(Goal("reserva", "Reserva de exemplo", 300_000, 85_000))
}
