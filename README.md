<<<<<<< HEAD
# AstroGestão (Android)

Aplicativo nativo Android (Kotlin + Jetpack Compose + Material 3 + Firebase Firestore) portado da branch `develop` de `thiagotrindade29/inventory-control`, pensado para uso pessoal e doméstico no dia a dia.

## Principais Funcionalidades

- **Autenticação com Google (Credential Manager + Firebase Auth)**: cada usuário acessa seus próprios dados isolados na nuvem (`/users/{uid}/...`).
- **Início (Dashboard da Casa)**: visão rápida de produtos cadastrados, unidades totais, valor estimado em estoque, lista de reposição automática e alertas de validade (próximos 30 dias).
- **Modo Mercado (Antes do Caixa)**: some o carrinho em tempo real antes de passar no caixa, leia códigos de barras com a câmera (CameraX + ML Kit), compare o preço atual na prateleira com a compra anterior e finalize a compra atualizando estoque e histórico atomicamente.
- **Importar Cupom / NF-e**: importe itens a partir de **Foto / QR Code** (via Android Photo Picker sem permissões amplas de armazenamento + ML Kit Barcode & OCR), **Link da SEFAZ**, **Arquivo XML** ou **Texto Colado**, com tela de revisão editável antes de enviar ao estoque.
- **Estoque da Casa & Movimentações**: busca rápida, filtros por categoria ou estoque baixo, cadastro/edição de produtos e lançamentos rápidos de entrada (`+ Entrada`) e consumo (`- Saída`).
- **Finanças & Comparativo de Preços**: total gasto no mês, variação média de preços (itens que subiram, mantiveram ou desceram), gastos por categoria e histórico de compras.

## Compilação Local (Android Studio / Gradle)

```bash
gradle :app:assembleDebug
```

## Testes das Regras do Firestore

```bash
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```
=======
# AstroGestão — ERP Doméstico (Android)

Aplicativo Android em Kotlin e Jetpack Compose para controle de estoque doméstico, compras de mercado em tempo real, leitura de código de barras, importação de NFC-e e análise financeira da casa.

## Funcionalidades

- **Acesso & Segurança na Nuvem**: Tela cósmica com login por **E-mail e Senha** (`Entrar`, `Criar Conta` e recuperação de senha) e **Google Sign-In** via Firebase Authentication, abertura sempre na tela de login e desconexão automática após 1 hora de inatividade. Dados sincronizados em tempo real no Cloud Firestore com suporte a espaço individual ou compartilhado em família.
- **Dashboard Integrado (Visão Geral do Estoque)**:
  - KPIs em tempo real: Total de Produtos, Valor Estimado Total (`R$`), Itens em Reposição e Alertas de Vencimento Próximo (30 dias).
  - Lista automática de **Compras / Reposição** para itens com saldo igual ou abaixo do mínimo configurado, com ação rápida `+ Repor`.
  - Painel de **Próximos do Vencimento (30 dias)** com contagem regressiva.
- **Modo Mercado (Frente de Caixa / Assistente em Tempo Real)**:
  - Leitor de código de barras via câmera (`CameraX` + `ML Kit Barcode Scanning`) e entrada manual rápida.
  - **Código Único Canônico (`AST-XXXX`) + Anti-duplicação**: cada produto recebe um código interno único e mantém uma tabela de códigos vinculados (`ProductAliasEntity`) para diferentes mercados. Ao bipar ou importar um código desconhecido, a busca normalizada por nome vincula ao mesmo item existente em vez de duplicar o cadastro.
  - Comparação automática do preço na prateleira contra o último preço pago e opção **Guardar itens no estoque ao finalizar** (ON = atualiza saldo e auditoria; OFF = grava apenas o gasto na análise financeira).
- **Importação de Cupom Fiscal / NFC-e**: Consulta via portal SEFAZ regional (incluindo SC `sat.sef.sc.gov.br`), leitura de URL/QR Code, chave de 44 dígitos ou upload de arquivo `.xml`.
- **Gestão de Produtos (CRUD)**: Busca em tempo real por nome, código único `AST-XXXX` ou código de barras, filtro por categoria colorida e chave **Somente estoque baixo**.
- **Análise Financeira**: Valor patrimonial em casa, total gasto em compras, consumo estimado, histórico de compras do Modo Mercado e gráficos em Compose `Canvas`.
- **Movimentações com Auditoria & Categorias**: Registro de `Entrada`, `Saída` e `Ajuste de Saldo` com saldo anterior/atual, motivo, autor e setores coloridos da casa.

## Segurança & Build no GitHub Actions

- `app/google-services.json` é ignorado no Git (`.gitignore`); use `app/google-services.example.json` apenas como referência estrutural sem credenciais.
- Para compilar o APK no GitHub Actions, adicione o secret `GOOGLE_SERVICES_JSON_BASE64` (conteúdo do `app/google-services.json` em Base64) em **Settings > Secrets and variables > Actions** (e opcionalmente `ANDROID_KEYSTORE_BASE64`).
>>>>>>> origin/main
