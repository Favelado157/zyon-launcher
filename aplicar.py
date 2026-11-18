"""Aplica ou recria a estrutura Zyon no servidor escolhido."""
import asyncio
import argparse
import getpass
import json
from pathlib import Path
import sys
import discord
from limpeza import inventory, make_plan, show_plan, approval_phrase, start_rebuild, delete_approved

BASE = Path(__file__).resolve().parent
REASON = 'Aplicação do template Zyon solicitada pelo administrador'
CORE_PERMISSIONS = ['manage_channels', 'manage_roles', 'manage_guild', 'view_channel', 'send_messages', 'read_message_history', 'connect', 'speak']


def load_manifest(path=BASE / 'template.json'):
    data = json.loads(path.read_text(encoding='utf-8-sig'))
    if data.get('schema') != 1:
        raise ValueError('Versão de template não reconhecida.')
    for items in [data['roles'], data['categories'], [ch for cat in data['categories'] for ch in cat['channels']]]:
        identifiers = set()
        for item in items:
            if item['id'] in identifiers:
                raise ValueError('Identificador repetido no template: ' + item['id'])
            identifiers.add(item['id'])
    for category in data['categories']:
        if category['access'] not in {'public', 'readonly', 'staff'}:
            raise ValueError('Tipo de acesso inválido.')
        for channel in category['channels']:
            if channel['type'] not in {'text', 'voice'}:
                raise ValueError('Tipo de canal inválido.')
            if len(channel.get('message', '')) > 2000:
                raise ValueError('Mensagem longa demais para o Discord.')
    return data


def role_permissions(role):
    permissions = discord.Permissions.none()
    for name in role['permissions']:
        if name not in discord.Permissions.VALID_FLAGS:
            raise ValueError('Permissão desconhecida: ' + name)
        setattr(permissions, name, True)
    return permissions


def required_permissions(manifest):
    result = discord.Permissions.none()
    for name in CORE_PERMISSIONS:
        setattr(result, name, True)
    for role in manifest['roles']:
        result.value |= role_permissions(role).value
    return result


def state_for(path, guild_id):
    if path.exists():
        state = json.loads(path.read_text(encoding='utf-8'))
        if state.get('guild_id') != guild_id:
            raise ValueError('O arquivo de progresso pertence a outro servidor.')
        return state
    return {'guild_id': guild_id, 'roles': {}, 'categories': {}, 'channels': {}, 'messages': {}}


def save_state(path, state):
    temporary = path.with_suffix('.tmp')
    temporary.write_text(json.dumps(state, ensure_ascii=False, indent=2), encoding='utf-8')
    temporary.replace(path)


def preflight(guild, manifest, state):
    # A named resource is reused only if this installer previously recorded its ID.
    # This avoids repurposing another category or role with the same name.
    missing_roles = 0
    missing_channels = 0
    for role in manifest['roles']:
        if guild.get_role(state['roles'].get(role['id'])):
            continue
        if any(existing.name == role['name'] for existing in guild.roles):
            raise ValueError('Já existe um cargo não gerenciado com o nome ' + role['name'] + '. Renomeie-o antes de aplicar para evitar conflito.')
        missing_roles += 1
    for category in manifest['categories']:
        existing = guild.get_channel(state['categories'].get(category['id']))
        if existing and existing.type != discord.ChannelType.category:
            raise ValueError('O progresso aponta para uma categoria inválida.')
        if not existing:
            if any(c.name == category['name'] for c in guild.categories):
                raise ValueError('Já existe uma categoria não gerenciada com o nome ' + category['name'] + '. Renomeie-a antes de aplicar.')
            missing_channels += 1
        for channel in category['channels']:
            saved = guild.get_channel(state['channels'].get(channel['id']))
            expected = discord.ChannelType.text if channel['type'] == 'text' else discord.ChannelType.voice
            if saved and saved.type != expected:
                raise ValueError('O progresso aponta para um canal de outro tipo.')
            if saved:
                continue
            if existing and any(c.name == channel['name'] for c in guild.channels if getattr(c, 'category_id', getattr(getattr(c, 'category', None), 'id', None)) == existing.id):
                raise ValueError('Canal existente não gerenciado: ' + channel['name'])
            missing_channels += 1
    if len(guild.roles) + missing_roles > 250:
        raise ValueError('O servidor não tem espaço suficiente para os cargos.')
    if len(guild.channels) + missing_channels > 500:
        raise ValueError('O servidor não tem espaço suficiente para os canais.')
    return missing_roles, missing_channels


def category_overwrites(guild, category, roles, manifest):
    readable = dict(view_channel=True, send_messages=True, read_message_history=True, connect=True, speak=True)
    everyone = dict(readable, mention_everyone=False)
    if category['access'] == 'staff':
        everyone = dict(view_channel=False)
    elif category['access'] == 'readonly':
        everyone.update(send_messages=False, send_messages_in_threads=False, create_public_threads=False, create_private_threads=False, add_reactions=True)
    overwrites = {guild.default_role: discord.PermissionOverwrite(**everyone), guild.me: discord.PermissionOverwrite(**readable)}
    if category['access'] == 'staff':
        for key in manifest['staff_roles']:
            overwrites[roles[key]] = discord.PermissionOverwrite(**readable)
    elif category['access'] == 'readonly':
        for key in manifest['publisher_roles']:
            overwrites[roles[key]] = discord.PermissionOverwrite(**readable)
    return overwrites


async def apply_structure(guild, manifest, state, path):
    roles = {}
    for spec in manifest['roles']:
        role = guild.get_role(state['roles'].get(spec['id']))
        if role is None:
            role = await guild.create_role(name=spec['name'], colour=discord.Colour(int(spec['color'], 16)), permissions=role_permissions(spec), hoist=True, mentionable=False, reason=REASON)
            state['roles'][spec['id']] = role.id
            save_state(path, state)
            print('Cargo criado:', role.name, flush=True)
        roles[spec['id']] = role
    for category in manifest['categories']:
        target = guild.get_channel(state['categories'].get(category['id']))
        overwrites = category_overwrites(guild, category, roles, manifest)
        if target is None:
            target = await guild.create_category(category['name'], overwrites=overwrites, reason=REASON)
            state['categories'][category['id']] = target.id
            save_state(path, state)
            print('Categoria criada:', target.name, flush=True)
        for spec in category['channels']:
            channel = guild.get_channel(state['channels'].get(spec['id']))
            if channel is None:
                if spec['type'] == 'text':
                    channel = await guild.create_text_channel(spec['name'], category=target, overwrites=overwrites, topic=spec.get('topic', ''), reason=REASON)
                else:
                    channel = await guild.create_voice_channel(spec['name'], category=target, overwrites=overwrites, reason=REASON)
                state['channels'][spec['id']] = channel.id
                save_state(path, state)
                print('Canal criado:', channel.name, flush=True)
            previous = state['messages'].get(spec['id'], {})
            if spec.get('message') and previous.get('channel_id') != channel.id:
                # allowed_mentions prevents template text from pinging people or roles.
                message = await channel.send(spec['message'], allowed_mentions=discord.AllowedMentions.none())
                state['messages'][spec['id']] = {'channel_id': channel.id, 'message_id': message.id}
                save_state(path, state)
    return roles


async def native_template(guild, manifest, state, path):
    templates = await guild.templates()
    template = next((t for t in templates if t.code == state.get('template_code')), None)
    if template:
        template = await template.sync()
    else:
        if len(templates) >= 5:
            raise ValueError('O servidor já tem cinco modelos. Gerencie os modelos nas configurações do Discord para liberar um espaço.')
        template = await guild.create_template(name=manifest['name'], description=manifest['description'])
    state['template_code'] = template.code
    state['template_url'] = template.url
    save_state(path, state)
    (path.parent / ('LINK-TEMPLATE-' + str(guild.id) + '.txt')).write_text(template.url + '\n', encoding='utf-8')
    return template.url


class SetupClient(discord.Client):
    def __init__(self, guild_id, manifest, rebuild=False):
        super().__init__(intents=discord.Intents(guilds=True), allowed_mentions=discord.AllowedMentions.none())
        self.rebuild = rebuild
        self.target_id = guild_id
        self.manifest = manifest
        self.started = False
        self.result = 1

    async def on_ready(self):
        if self.started:
            return
        self.started = True
        try:
            guild = self.get_guild(self.target_id)
            if guild is None:
                url = discord.utils.oauth_url(self.application_id or self.user.id, permissions=required_permissions(self.manifest), scopes=['bot'], guild=discord.Object(id=self.target_id), disable_guild_select=True)
                print('\nConvide seu bot ao servidor por este link e depois abra APLICAR-TEMPLATE.bat novamente:\n' + url, flush=True)
                return
            if guild.me is None:
                raise ValueError('Não foi possível localizar o bot no servidor.')
            required = required_permissions(self.manifest)
            if not guild.me.guild_permissions.is_superset(required):
                url = discord.utils.oauth_url(self.application_id or self.user.id, permissions=required, scopes=['bot'], guild=discord.Object(id=guild.id), disable_guild_select=True)
                raise ValueError('O bot precisa das permissões de criação e das permissões usadas pelos cargos de moderação. Autorize pelo link e execute novamente:\n' + url)
            path = BASE / ('progresso-' + str(guild.id) + '.json')
            state = state_for(path, guild.id)
            current = await inventory(guild)
            marker = state.get('rebuild', {})
            if self.rebuild and marker.get('phase') != 'building':
                if marker.get('phase') == 'deleting':
                    plan = marker
                    print('Retomando somente as exclusões do plano já aprovado. Itens criados depois não entram na lista.')
                else:
                    plan = make_plan(current, self.manifest)
                show_plan(plan)
                print('Depois serão criados 6 categorias, 27 canais e 10 cargos do novo Zyon.')
                phrase = approval_phrase(guild.id)
                answer = await asyncio.to_thread(input, '\nPara confirmar esta exclusão permanente, digite exatamente ' + phrase + ': ')
                if answer.strip() != phrase:
                    print('Cancelado. Nenhuma nova exclusão foi feita.');self.result = 0;return
                if marker.get('phase') != 'deleting':
                    state = start_rebuild(current, state, plan, path, save_state)
                await delete_approved(current, state, path, save_state, answer.strip())
                current = await inventory(guild)
                preflight(current, self.manifest, state)
            else:
                if marker.get('phase') == 'deleting':
                    raise ValueError('A limpeza está incompleta. Use RECRIAR-SERVIDOR.bat para retomar antes de criar itens novos.')
                count_roles, count_channels = preflight(current, self.manifest, state)
                print('\nSERVIDOR ESCOLHIDO: ' + guild.name + ' (ID ' + str(guild.id) + ')')
                print(str(count_roles) + ' cargos e ' + str(count_channels) + ' categorias/canais novos.')
                if marker.get('phase') == 'building':
                    print('A limpeza já terminou. Esta execução só retoma a criação, sem apagar os itens novos.')
                else:
                    print('Modo sem apagar: os canais e cargos existentes serão preservados.')
                print('Os cargos Administrador e Moderador têm permissões de moderação. Atribua-os apenas à equipe.')
                print('O link nativo copia nomes e permissões de TODOS os canais/cargos do servidor.')
                answer = await asyncio.to_thread(input, 'Confira ESTRUTURA.md e digite CRIAR para continuar: ')
                if answer.strip().upper() != 'CRIAR':
                    print('Cancelado.');self.result = 0;return
                save_state(path, state)
            await apply_structure(current, self.manifest, state, path)
            if state.get('rebuild', {}).get('phase') == 'building':
                state['rebuild']['phase'] = 'complete'
                save_state(path, state)
            print('\nEstrutura criada! Atribua os cargos manualmente e envie os PNGs da pasta emojis nas configurações do servidor.', flush=True)
            try:
                url = await native_template(guild, self.manifest, state, path)
                print('\nLINK NATIVO DO TEMPLATE:\n' + url)
                print('O link foi salvo em LINK-TEMPLATE-' + str(guild.id) + '.txt')
                print('O link nativo copia a estrutura e cargos; mensagens, membros, bots e emojis não são copiados.')
            except (discord.HTTPException, ValueError) as error:
                print('A estrutura está pronta, mas o link nativo não foi gerado:', str(error))
                print('Use APLICAR-SEM-APAGAR.bat para tentar gerar o link sem uma nova limpeza, ou Configurações do servidor > Modelo de servidor.')
            self.result = 0
        except (discord.HTTPException, ValueError, OSError) as error:
            print('\nNão foi possível concluir:', str(error), flush=True)
            print('O progresso foi mantido. Corrija o problema e execute novamente para retomar.')
        finally:
            await self.close()


def main():
    parser = argparse.ArgumentParser(description='Template Zyon v2 para Discord')
    parser.add_argument('--recriar', action='store_true', help='Exclui canais/cargos antigos após mostrar o plano e confirmar o ID do servidor')
    args = parser.parse_args()
    manifest = load_manifest()
    print('TEMPLATE ZYON — aplicação de estrutura no seu servidor Discord.')
    print('Leia LEIA-ME.txt e ESTRUTURA.md antes de começar.')
    server_id = input('Cole o ID do servidor (Modo desenvolvedor > Copiar ID): ').strip()
    if not server_id.isdigit() or not 16 <= len(server_id) <= 20:
        raise ValueError('ID do servidor inválido. Cole somente os números.')
    print('Use um token de BOT criado no Discord Developer Portal, nunca o token da sua conta pessoal.')
    token = getpass.getpass('Token do bot (não será mostrado nem salvo): ').strip()
    if not token:
        raise ValueError('Token vazio.')
    client = SetupClient(int(server_id), manifest, rebuild=args.recriar)
    try:
        client.run(token)
    except discord.LoginFailure:
        print('Token recusado. Obtenha um token válido na página Bot do seu aplicativo.')
        return 1
    return client.result


if __name__ == '__main__':
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        print('\nCancelado. Os itens já criados e seu progresso foram mantidos.')
        raise SystemExit(0)
    except (ValueError, OSError) as error:
        print('Erro:', str(error))
        raise SystemExit(1)
