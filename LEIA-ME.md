# Zyon Visual — Windows 10/11 x64

Esta edição tem somente HUD, keystrokes, mira, zoom próprio e tema. O aplicativo é independente da Mojang/Microsoft. A autorização das APIs está pendente após a recusa anterior; esta versão não significa aprovação. Sem autorização e propriedade verificada, o jogo não abre.

## Instalar e conectar

1. Feche o Minecraft e o Zyon. Baixe o instalador da série **1.3** na página [Releases](https://github.com/Favelado157/zyon-launcher/releases/latest) e execute-o.
2. Em **Minha conta**, escolha **Entrar com Microsoft**. Confirme seu código somente na página oficial aberta pelo navegador, usando uma conta com acesso ao Minecraft Java.
3. Quando os serviços autorizarem o aplicativo e confirmarem a propriedade, use **Verificar e preparar**. A primeira preparação baixa os arquivos oficiais, o Forge, o Java 8 e o cliente visual.
4. Use **Verificar e jogar**. A propriedade será consultada novamente, inclusive quando os arquivos já estiverem instalados. Sem internet ou se o serviço recusar o acesso, a abertura é interrompida.

O Zyon nunca solicita sua senha dentro do aplicativo. Códigos de dispositivo e tokens são privados; não os envie a outras pessoas. **Sair da conta** remove o cache local da conta desse aplicativo.

## Dentro do jogo

- **Shift direito:** painel com categorias, busca, interruptores e ajustes. O botão **?** explica cada recurso ao passar o mouse.
- **Ctrl direito**, ou **Editar HUD:** arraste os keystrokes e solte para salvar. Configure tamanho, cor, opacidade, CPS, mouse e pulo na função Keystrokes.
- **C:** segure para ampliar a imagem. Ative Zoom e ajuste o fator no painel. O zoom não depende do OptiFine.
- **End:** oculta os extras visuais. Reative os que desejar no painel.
- **Esc:** fecha o painel.

HUD mostra informações do próprio jogador. Mira ajusta a aparência da mira; Tema controla cores e fundo. Os perfis ficam em `config/zyon`, na pasta da instância. O menu aceita `/zyon`, `/zyon toggle crosshair`, `/zyon bind crosshair R`, `/zyon profile save principal` e `/zyon profile load principal`.

## Pastas e migração

A instância nova fica em `%APPDATA%\ZyonLauncher\instances\zyon-visual-1.8.9`. A edição anterior e seus mundos continuam na pasta antiga. Em **Configurações**, use **Abrir pasta da instalação anterior**. O launcher não move os mundos automaticamente; copie apenas a pasta de um mundo em `saves`, com o jogo fechado, se quiser transferi-lo. Faça uma cópia de segurança antes de abrir um mundo importante.

Somente campos de perfis visuais compatíveis são recuperados. Mods antigos não são copiados. Se uma pasta contiver mods externos ou um cliente diferente, escolha uma pasta nova; os arquivos existentes são preservados. O cache oficial em `%APPDATA%\ZyonLauncher\minecraft` pode ser compartilhado entre versões do Zyon.

## Problemas e atualizações

- **HTTP 403:** o serviço recusou o acesso. Confira a autorização oficial do aplicativo e a conta; reinstalar ou trocar o Client ID não concede autorização.
- **Sem acesso ao Minecraft Java:** confira a compra ou assinatura na sua conta oficial.
- **Sessão expirada:** entre novamente com Microsoft.
- **Jogo fecha antes do menu:** use **Abrir registros** e consulte `game.log`. Tokens são omitidos desses registros.
- **Atualizações:** verifique a aba Atualizações. O aviso também aparece ao abrir e periodicamente. O instalador preserva os dados em AppData.

Não há promessa de aumento de FPS. Ajuste a memória em Configurações e a distância de visão/limite de FPS nas opções do Minecraft conforme seu PC.

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
