# Histórico de Versões — AstroGestão

Todas as versões relevantes do aplicativo são registradas aqui utilizando **Versionamento Semântico (`MAJOR.MINOR.PATCH`)** alinhado ao `versionName` e `versionCode` em `app/build.gradle.kts` e às tags `v*` do GitHub Actions.

## [1.1.0] (Build 2)
### Adicionado
- **Categoria de Consumo Único (`singleConsumption`)**: novo tipo de categoria (incluindo o setor padrão *Consumo Único* e chave configurável em qualquer categoria) para lanches e itens consumidos na hora ou em poucos dias.
- Itens vinculados a uma categoria de consumo único são contabilizados nos gastos e movimentações, mas **não geram alertas, selos de "Estoque baixo" nem aparecem na Lista de Reposição**.
- **Verificação remota de versão**: compara a `versionName` instalada com o documento `appConfig/android` no Firestore e exibe aviso quando houver nova versão disponível (além do botão manual em *Mais*).
- **Notificação simples de estoque baixo**: envia um lembrete discreto no Android quando itens ainda aguardam reposição (respeitando a categoria *Consumo Único*, intervalo de 12 horas para evitar repetições e opção de ativar/desativar em *Mais*).
- **Controle de versão e higiene de repositório**: início do histórico formal (`CHANGELOG.md`), atualização para `v1.1.0` (Build `2`) e `.gitignore` configurado para ignorar imagens de teste em `public/assets/aistudio/` e logs temporários (`*-debug.log`, `*.log.txt`).

### Melhorado
- **Importador de NF-e / Cupom**: fluxo redesenhado para usuários leigos com foco direto na chave de acesso de 44 dígitos, contador de dígitos, mensagens de erro em linguagem simples com ações rápidas e botão para descartar/excluir importações em andamento.

## [1.0.1] (Build 1)
### Base
- Sincronização em nuvem via Firebase Authentication (Google Sign-In) e Cloud Firestore com espaço individual ou familiar.
- Dashboard de estoque, Modo Mercado (Frente de Caixa), leitor de código de barras, análise financeira, auditoria de movimentações e backup/restauração em JSON.
