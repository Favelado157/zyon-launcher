# Ativar Play online e atualizações para todos

São dois cadastros independentes: o repositório hospeda as versões do instalador; o aplicativo Microsoft permite autenticar os jogadores. O canal já foi definido nas fontes como `Favelado157/zyon-launcher`, e o Client ID público do Zyon já está incluído em `release-config.json`. A aprovação Microsoft/Minecraft e o teste real de login continuam pendentes. É possível publicar atualizações visuais antes de concluir o login próprio.

## 1. Repositório GitHub

1. Entre na sua conta e abra https://github.com/new.
2. Use o nome **zyon-launcher**.
3. Escolha **Public** e marque **Add a README file**.
4. Crie e guarde o endereço `https://github.com/SEU-USUARIO/zyon-launcher`.

O canal dos jogadores deve ser público. Não coloque um token pessoal GitHub no launcher para acessar um repositório privado. Releases públicas não precisam de token dos jogadores.

Se a conexão GitHub desta conversa continuar retornando 403 de conta suspensa, confira no navegador qual conta está ativa. Essa conexão não é corrigida pelo código do Zyon. Você pode configurar o fluxo pelo site com sua conta ativa, sem me enviar credenciais.

## 2. Aplicativo Microsoft

1. Abra https://entra.microsoft.com/ e entre com a conta responsável pelo aplicativo.
2. Em **App registrations / Registros de aplicativo**, crie **Zyon Launcher**.
3. Habilite suporte a **contas Microsoft pessoais**. Se a opção estiver combinada com contas organizacionais, o Zyon continuará usando o endpoint `consumers` para as contas pessoais.
4. Em **Authentication / Autenticação**, habilite **Allow public client flows / Permitir fluxos de cliente público**. O Zyon usa código de dispositivo; não precisa de servidor de redirecionamento.
5. Copie somente **Application (client) ID / ID do aplicativo (cliente)**, um identificador público como `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`.
6. Não crie nem distribua client secret para esse app desktop. Não envie senhas, cookies, códigos de login ou tokens para a conversa.
7. Siga a revisão vigente de App ID para as APIs Minecraft em **https://aka.ms/mce-reviewappid**. Informe o ID, nome do aplicativo e repositório, conforme o formulário solicitar.

O registro no Entra **não garante** acesso ao Minecraft. A Microsoft/Mojang precisa aprovar o app. Até lá, o login pode chegar ao Xbox e receber **403: Invalid app registration** no Minecraft. Nenhuma opção do Zyon concede essa aprovação.

O fluxo solicita `XboxLive.signin` e `offline_access`. Se sua conta não puder criar o registro, resolva o acesso ao portal com a Microsoft; o launcher não cria o cadastro em seu nome.

Referências:

- https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-device-code
- https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-register-app
- https://minecraft.wiki/w/Microsoft_authentication

## 3. Testar no seu PC

Após obter o Client ID aprovado:

1. Zyon → Configurações → Configuração do publicador.
2. Informe o ID público e `SEU-USUARIO/zyon-launcher`.
3. Salve, prepare o jogo e conecte a conta em Minha conta.
4. Teste Play e a abertura de um mundo.

Esses valores afetam **somente esse PC**. Para outras pessoas, publique uma compilação com os valores incluídos.

## 4. Ativar a publicação

1. Repositório → **Settings → Secrets and variables → Actions → Variables**.
2. Quando o cadastro Microsoft estiver aprovado, crie a variável **MICROSOFT_CLIENT_ID** com esse ID público. Ela é opcional para publicar as atualizações visuais; sem ela, o login online próprio continua bloqueado. Não é uma senha.
3. Extraia o pacote de fontes e envie os arquivos para a raiz do repositório: `package.json`, `package-lock.json`, `main.cjs`, `renderer`, `assets`, `payload`, `client-src`, `build`, `test`, **`.github`** e demais arquivos incluídos. Não envie `node_modules`, `dist`, `.gradle`, caches, tokens ou arquivos da instalação pessoal.
4. Confira se `package.json` e `.github` estão na **raiz**, sem uma pasta extra envolvendo o projeto.
5. Envie à branch **main**. O workflow em **Actions** compila o mod, testa, gera o instalador e cria uma **Release** com `.exe` e `SHA256SUMS.txt`.

O workflow insere o repositório atual e a variável Microsoft no `release-config.json` da compilação. Quando a variável está vazia, mantém o Client ID público já incluído nas fontes. A versão aumenta com o número da execução; a branch não é alterada apenas para gerar a versão.

Se tanto a variável Microsoft quanto o ID das fontes estiverem vazios, o workflow publica o instalador com o canal de atualizações configurado e o login próprio ainda pendente. Com o ID incluído, teste o login real após a aprovação: o workflow não concede nem simula a aprovação Microsoft.

## 5. Publicar as próximas mudanças

1. Altere o projeto localmente ou pelo GitHub.
2. Envie a alteração para **main**.
3. Aguarde o workflow terminar e a release aparecer.
4. Clientes com esse canal verificam ao abrir e periodicamente, e oferecem a versão mais nova.

Não é um comando remoto instantâneo. Quem estiver sem internet ou com o launcher fechado verificará depois. A pessoa pode atualizar ou deixar para mais tarde. O instalador é verificado e os mundos/configurações são mantidos.

Quem usa a **1.0** precisa instalar o verificador uma vez. Quem recebeu a **1.1 sem canal** também precisa da primeira compilação configurada ou definir o canal manualmente. Não é possível adicionar o verificador retroativamente a executáveis antigos já instalados.

## Conectar o canal no Zyon já instalado

Em **Configurações → Configuração do publicador**, informe `Favelado157/zyon-launcher` em Repositório e salve. Isso não exige Client ID para verificar atualizações. Antes da primeira release pública, a verificação poderá informar que ainda não há versão disponível. O Play online próprio continua dependendo do cadastro Microsoft.

## Compilação local

Com o projeto extraído e Node.js 24:

```powershell
npm ci
npm run configure:publisher -- --repository SEU-USUARIO/zyon-launcher --client-id SEU-CLIENT-ID-PUBLICO
npm test
npm run dist:win
node build/prepare-release.cjs --checksums
```

Publique o `.exe` e `SHA256SUMS.txt` na mesma release, com uma tag estável de três números como `v1.1.1`. A versão do `package.json` deve corresponder ao nome do instalador. O workflow cuida dessa numeração.

Artes do jogo: `client-src/src/main/resources/assets/zyon/textures`. Artes do launcher: `assets`. Para usar o mesmo fundo nos dois, altere os dois arquivos correspondentes antes de publicar.

Para configurar apenas o canal, antes de ter o cadastro Microsoft, use:

```powershell
npm run configure:publisher -- --repository Favelado157/zyon-launcher
```
