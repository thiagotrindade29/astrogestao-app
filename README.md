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
