# Publicação e aplicativo Microsoft

O repositório público `Favelado157/zyon-launcher` hospeda o código e os instaladores. O aplicativo Microsoft do Zyon usa o Client ID público `542c755e-734c-47e3-a110-a772b12c6688`. O pedido anterior de acesso às APIs foi recusado. A edição visual 1.3 foi preparada para nova análise, sem garantia de aprovação.

## Aplicativo Microsoft

No [Entra](https://entra.microsoft.com/), use Registros de aplicativo > Zyon Launcher > Autenticação. O aplicativo precisa aceitar contas Microsoft pessoais e ter **Permitir fluxos de cliente público** habilitado. O launcher usa código de dispositivo pelo endpoint consumers, com os escopos XboxLive.signin e offline_access; não distribua um client secret no desktop.

Revise sua candidatura em https://aka.ms/mce-reviewappid com descrição exata dos cinco recursos, links para a fonte e o instalador atuais e informações de contato verdadeiras. Use [docs/API-review.md](docs/API-review.md) como referência técnica, conferindo os campos e as exigências atuais do formulário. Não declare aprovação inexistente nem omita o histórico do projeto. Alterações de capacidades e identidade precisam ser informadas com precisão conforme solicitado na análise.

Não use IDs de outros aplicativos para contornar a recusa. O Client ID e o canal são definidos pelo publicador na compilação, sem campos para troca no launcher. Somente uma mudança legítima do aplicativo publicado deve alterar esses valores. Não compartilhe senhas, códigos de login, cookies ou tokens de jogadores.

## Publicação automática

1. Envie as alterações para a branch main.
2. O workflow compila o cliente Java, executa os testes, gera o instalador Windows e publica uma release com o `.exe` e `SHA256SUMS.txt`.
3. A versão cresce com o número da execução. O Client ID vem do release-config.json; a variável de repositório MICROSOFT_CLIENT_ID só deve refletir o aplicativo legítimo do publicador, nunca um ID emprestado.
4. O launcher consulta novas versões ao abrir e periodicamente, oferecendo a instalação ao jogador. Isso não força uma atualização instantânea em PCs desligados ou sem internet.

Um instalador pode ser publicado antes da autorização das APIs, mas preparar/jogar ficam bloqueados se os serviços não autorizarem o aplicativo ou a propriedade. O CI não concede essa autorização e usa respostas simuladas somente nos testes, sem um modo de simulação no produto.

## Compilação local

Requisitos: Node.js 24, JDK 21 e JDK 8.

```powershell
npm ci
cd client-src
.\gradlew.bat build
cd ..
Copy-Item client-src/build/libs/zyon-1.8.9-1.3.0.jar payload/zyon-1.8.9-1.3.0.jar -Force
npm run prepare:release
npm test
npm run dist:win
node build/prepare-release.cjs --checksums
```

Os testes de arquivos oficiais usam FORGE_FIXTURE e MINECRAFT_FIXTURE com arquivos oficiais válidos; o workflow baixa e verifica essas fixtures. As artes estão em assets e client-src/src/main/resources/assets/zyon/textures. Não distribua executáveis com versões contraditórias ou payload antigo.
