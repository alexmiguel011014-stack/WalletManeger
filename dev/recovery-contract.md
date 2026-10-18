# Contrato de backup e recuperação local

Este contrato define a proposta de G11.1 para uma exportação manual e criptografada dos dados locais. Ele não habilita backup automático, nuvem, sincronização, nem restauração nesta etapa.

## Decisões propostas para aprovação do dono

- A pessoa inicia uma exportação manual e informa uma frase secreta. O arquivo gerado só pode ser aberto com essa mesma frase.
- O arquivo tem limite de **16 MiB**. Se exceder o limite, a exportação falha sem criar arquivo parcial.
- O aplicativo não envia, armazena ou recupera a frase secreta. Perder a frase torna o arquivo irrecuperável.
- A recuperação aceita apenas arquivos exportados pelo aplicativo com versão de formato compatível.
- Antes de alterar dados, a recuperação mostra um resumo do arquivo: versão, data da exportação e contagem por tipo de registro.
- A única política disponível é **substituir todos os dados locais** depois de uma confirmação final. Mesclar dados ainda não está disponível.

## Dados abrangidos

A exportação deve conter lançamentos, limites, metas e decisões de conciliação. Também inclui a fila de avisos de importação já reduzida aos campos necessários e seus vínculos com os lançamentos ou decisões; nunca o texto bruto da notificação.

Categorias internas usam seus identificadores padrão e são validadas contra os padrões existentes durante a recuperação. Preferências de tema, reduzir movimento e formato de valores em centavos também são restauradas.

Ficam fora do arquivo e são recriados ou ficam desativados após a recuperação:

- contas, identificadores internos e saldo derivado dos lançamentos;
- permissões de notificação, fonte habilitada e filtro pelos quatro dígitos finais; a captura de notificações volta desativada;
- avisos, que são gerados novamente pelas regras locais, sem manter arquivamentos ou dispensas anteriores;
- estado de materialização, onboarding, versão de esquema, logs e qualquer estado visual temporário;
- frase secreta, chaves, credenciais, backups anteriores, dados de banco remoto e dados de outro aplicativo.

## Regras de segurança e recuperação

- O formato deve ter uma versão visível antes da parte criptografada e validar a integridade do conteúdo após a abertura.
- Arquivo adulterado, corrompido, com frase secreta errada, versão não compatível ou tamanho inválido deve falhar com explicação clara e sem alterar os dados locais.
- Registros duplicados ou inconsistentes tornam o arquivo inválido: a recuperação o rejeita antes de qualquer alteração e nunca remove duplicidades silenciosamente.
- Cancelar a escolha do arquivo, a prévia ou a confirmação final deve deixar os dados locais inalterados.
- A recuperação valida limites, tipos, campos obrigatórios, identificadores de categoria e vínculos entre lançamentos, importações e decisões antes da escrita. Só pode substituir os dados quando a validação completa terminar.
- A escrita da recuperação deve ser atômica: falha, interrupção ou erro deixa a base anterior íntegra; êxito deixa apenas o conjunto restaurado.
- Ao concluir, o app informa que os dados restaurados substituíram os dados anteriores e atualiza as telas a partir da nova base local.

## Aceite pendente

O dono precisa aprovar este contrato, incluindo o limite de 16 MiB e a política de substituição total, antes de implementar o formato, a criptografia, a exportação ou a recuperação.
