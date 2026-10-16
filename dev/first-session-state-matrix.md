# Matriz de estados da primeira sessão

Este documento orienta G10.1. A tela inicial e as ações ficam em português. Uma instalação nova não pede conta, conexão bancária ou permissão de notificações e não recebe dados financeiros fictícios. Categorias padrão existem localmente; lançamentos, limites e respostas do Assistente permanecem no aparelho.

## Caminho até o primeiro lançamento salvo

1. Na primeira abertura, a tela de boas-vindas informa: **“Seus dados ficam neste dispositivo. Você não precisa conectar seu banco.”**
2. Toque em **“Adicionar lançamento”** para abrir diretamente o formulário manual.
3. Informe valor, estabelecimento, tipo (entrada ou saída), categoria e data em **dd/MM/aaaa**; descrição é opcional. Toque em **“Salvar”**.
4. O lançamento salvo aparece no Extrato e atualiza os totais locais. A tela indica como próximo passo opcional **“Adicionar limite”**; nenhum limite ou dado é criado sem ação do usuário.

Não salvar até a confirmação significa não alterar o Extrato nem os totais. Erros só aparecem após interação ou tentativa de salvar e mantêm os valores digitados. A primeira sessão não depende de importar notificações.

## Estados e regras por destino

“Importações pendentes” é uma camada sobre os registros já salvos: rascunhos nunca contam como lançamentos, saldo, gastos, orçamento, aviso ou fonte do Assistente. Em cada linha, **ação primária** é a ação destacada daquela tela; outras ações legítimas podem continuar acessíveis como secundárias.

| Estado | Tela | Ação primária | Explicação curta | Cartões, filtros e atalhos |
| --- | --- | --- | --- | --- |
| Sem lançamentos / sem limite | Home | **Adicionar lançamento** → formulário manual | “Comece por uma entrada ou saída; os totais usam apenas lançamentos salvos neste aparelho.” | Mostrar saldo como vazio, disclosure local e progresso “1. Primeiro lançamento”. Ocultar gráficos, categorias, limites, insights e lançamentos recentes sem dados. |
| Sem lançamentos / sem limite | Extrato | **Adicionar lançamento** | “Seus lançamentos salvos aparecerão aqui.” | Mostrar estado vazio; ocultar busca e filtros de categoria/data. Mostrar atalho de importação somente se a fonte tiver sido ativada em Ajustes ou houver rascunhos pendentes. |
| Sem lançamentos / sem limite | Planos | **Adicionar limite** | “Um limite acompanha os gastos salvos de uma categoria; sem lançamentos, ainda não há comparação.” | Mostrar estado vazio e categorias de despesa disponíveis. Ocultar cartões de limite inexistentes e simulação. |
| Sem lançamentos / sem limite | Avisos | **Adicionar lançamento** | “Avisos dependem de regras locais sobre dados salvos.” | Mostrar explicação vazia; ocultar caixa de revisão e seletores Ativos/Arquivados enquanto não houver caso ou aviso. |
| Sem lançamentos / sem limite | Assistente | **Adicionar lançamento** | “O Assistente responde com base nos lançamentos confirmados neste aparelho.” | Substituir sugestões de perguntas e conversa vazia por estado útil e ação para cadastrar; não oferecer respostas sobre dados ausentes. |
| Com lançamentos / sem limite | Home | **Adicionar limite** | “Seu resumo já usa lançamentos salvos. Crie um limite para acompanhar uma categoria.” | Mostrar saldo, período e lançamentos recentes; mostrar progresso “2. Primeiro limite”. Trocar o cartão de orçamento sem dados por convite conciso; só mostrar análises que os dados sustentem. |
| Com lançamentos / sem limite | Extrato | **Adicionar lançamento** | “Este histórico contém apenas lançamentos salvos.” | Mostrar lista, busca e filtros de categoria/data. Atalho para importações somente se a fonte estiver ativada ou houver rascunhos; filtros sem resultado oferecem “Limpar filtros”. |
| Com lançamentos / sem limite | Planos | **Adicionar limite** | “Escolha uma categoria para comparar o limite com os gastos salvos do mês.” | Mostrar vazio contextual e categorias disponíveis; ocultar simulação até existir ao menos um limite. |
| Com lançamentos / sem limite | Avisos | **Revisar aviso**, se uma regra local já gerou um; caso contrário, **Ver lançamentos** | “Avisos aparecem quando os dados salvos atendem a uma regra local.” | Mostrar apenas avisos/casos existentes. Sem resultados, explicar a ausência e ocultar caixa de revisão e alternância Ativos/Arquivados; mostrar alternância só se houver itens arquivados. |
| Com lançamentos / sem limite | Assistente | **Perguntar sobre seus lançamentos** | “As respostas usam dados locais confirmados e mostram suas fontes.” | Habilitar campo e atalhos apenas para perguntas com resultado significativo; mostrar fontes junto às respostas. Ocultar sugestões sem dados correspondentes. |
| Com lançamentos / com limite | Home | **Adicionar lançamento** | “Resumo e comparações usam lançamentos e limites salvos neste aparelho.” | Mostrar saldo, período, limites, lançamentos recentes e análises sustentadas pelos dados. Exibir alertas/insights apenas quando uma regra local os produzir; não preencher lacunas com zeros fictícios. |
| Com lançamentos / com limite | Extrato | **Adicionar lançamento** | “O histórico pode ser pesquisado e filtrado.” | Mostrar lista, busca, filtros de categoria/data e atalho de importação somente se a fonte estiver ativada ou houver pendências. |
| Com lançamentos / com limite | Planos | **Revisar limites**; oferecer **Adicionar limite** quando houver categoria disponível | “Compare o gasto salvo de cada categoria ao limite definido.” | Mostrar cartões de limites e gastos confirmados; habilitar simulação só com ao menos um limite. Se todas as categorias já tiverem limite, priorizar edição/revisão, sem formulário vazio. |
| Com lançamentos / com limite | Avisos | **Revisar aviso**, se existir; caso contrário, **Ver lançamentos** | “Avisos são produzidos por regras locais, não por conexão externa.” | Mostrar avisos e casos existentes. Sem itens, mostrar estado vazio e ocultar controles de revisão; revelar Arquivados apenas quando houver itens arquivados. |
| Com lançamentos / com limite | Assistente | **Perguntar sobre seus dados** | “As respostas locais consideram lançamentos confirmados e limites salvos.” | Habilitar somente perguntas respondíveis, citar fontes locais e não sugerir perguntas sem dados correspondentes. |
| Importações pendentes | Home | **Revisar importações pendentes (N)** | “Importações não alteram seus totais até você revisar e confirmar cada item.” | Dar destaque apenas à contagem, nunca a valores pendentes no saldo ou nos gráficos. Manter os cartões da Home calculados só com lançamentos confirmados e oferecer atalho à fila. |
| Importações pendentes | Extrato | **Revisar importações pendentes (N)**; **Adicionar lançamento** permanece secundária | “Revise, corrija e confirme cada item antes de incluí-lo no Extrato.” | Abrir a fila separada do histórico. Filtros do histórico aparecem somente se houver lançamentos confirmados; atalho à fila fica visível enquanto houver pendências. |
| Importações pendentes | Planos | **Adicionar limite** se não houver limite; senão **Revisar limites** | “Rascunhos aguardando confirmação não entram nos gastos nem nos limites.” | Aplicar as regras de cartões e simulação do estado de limites salvos. Não exibir valores pendentes nem duplicar a fila nesta tela. |
| Importações pendentes | Avisos | **Ver lançamentos** | “Importações aguardando confirmação ficam no Extrato; ainda não são avisos nem lançamentos.” | Alertas e revisões consideram apenas dados confirmados. Se não houver aviso/caso, mostrar estado vazio e ocultar controles de revisão; sem atalho para confirmar nesta tela. |
| Importações pendentes | Assistente | **Perguntar sobre dados salvos**, se existirem; sem lançamentos, **Revisar importações pendentes (N)** | “O Assistente não usa rascunhos; confirme-os no Extrato para incluí-los nos cálculos.” | Habilitar apenas perguntas respondíveis com dados confirmados. Não mostrar valores pendentes, não responder a partir deles e não duplicar a fila nesta tela. |

## Consentimento e estados sem dados

- Captura de notificações é opcional e começa somente após a pessoa escolher uma fonte compatível e conceder acesso em Ajustes. Não pedir permissão na instalação/primeira abertura, não pedir credenciais bancárias e não conectar serviços remotos.
- Uma notificação reconhecida vira rascunho local. Mostrar origem, tipo, data, valor e descrição mínima para revisão; permitir correção e categoria e exigir confirmação explícita por item. Uma despesa importada exige categoria confirmada. Ignorar não cria lançamento.
- Não guardar nem registrar texto bruto da notificação, códigos, identificadores completos de conta/cartão ou dados de fonte não selecionada. A importação continua local e offline.
- Distinguir “ainda não há dados cadastrados” de “total calculado igual a R$ 0,00”. Estados vazios explicam o requisito e dão uma próxima ação útil; não sugerem alerta, comparação ou resposta que os dados não sustentem.
