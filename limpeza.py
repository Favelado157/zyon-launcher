"""Plano de exclusão por IDs, confirmação explícita e retomada por fases."""
from datetime import datetime, timezone
import json
import discord


class Inventory:
    def __init__(self, guild, roles, channels, me):
        self.guild = guild
        self.roles = roles
        self.channels = channels
        self.categories = [c for c in channels if c.type == discord.ChannelType.category]
        self.me = me

    def __getattr__(self, name):
        return getattr(self.guild, name)

    def get_role(self, identifier):
        return next((r for r in self.roles if r.id == identifier), None)

    def get_channel(self, identifier):
        return next((c for c in self.channels if c.id == identifier), None)


async def inventory(guild):
    roles = await guild.fetch_roles()
    channels = await guild.fetch_channels()
    me = await guild.fetch_member(guild.me.id)
    return Inventory(guild, roles, channels, me)


def approval_phrase(guild_id):
    return 'APAGAR ' + str(guild_id)


def make_plan(guild, manifest):
    if 'COMMUNITY' in guild.features:
        raise ValueError('Desative Comunidade nas configurações do servidor antes de recriar. O Discord protege os canais obrigatórios de servidores Comunidade. Nenhum item foi apagado.')
    own_ids = {r.id for r in guild.me.roles}
    own_roles = [r for r in guild.roles if r.id in own_ids]
    if not own_roles:
        raise ValueError('Não foi possível conferir os cargos do bot.')
    top_position = max(r.position for r in own_roles)
    protected, targets, blocked = [], [], []
    for role in guild.roles:
        if role.id == guild.default_role.id:
            protected.append(dict(id=role.id, name=role.name, reason='@everyone'))
        elif role.managed:
            protected.append(dict(id=role.id, name=role.name, reason='cargo de bot/integração gerenciado pelo Discord'))
        elif role.id in own_ids:
            protected.append(dict(id=role.id, name=role.name, reason='cargo atribuído ao bot aplicador'))
        elif role.position >= top_position:
            blocked.append(role.name)
        else:
            targets.append(dict(id=role.id, name=role.name, position=role.position))
    if blocked:
        raise ValueError('Mova o cargo do bot ACIMA destes cargos antes de continuar: ' + ', '.join(blocked) + '. Nenhum item foi apagado.')
    unavailable = [c.name for c in guild.channels if not c.permissions_for(guild.me).manage_channels]
    if unavailable:
        raise ValueError('Dê ao bot acesso e Gerenciar canais nestes canais/categorias privados: ' + ', '.join(unavailable) + '. Nenhum item foi apagado.')
    wanted_names = {r['name'] for r in manifest['roles']}
    collisions = [r['name'] for r in protected if r['name'] in wanted_names]
    if collisions:
        raise ValueError('Um cargo preservado usa um nome do novo template: ' + ', '.join(collisions) + '. Se for um cargo extra do bot, retire-o do bot antes de tentar novamente; mantenha o cargo próprio da integração.')
    if len(protected) + len(manifest['roles']) > 250:
        raise ValueError('Sem espaço para os novos cargos após a limpeza.')
    if len(manifest['categories']) + sum(len(c['channels']) for c in manifest['categories']) > 500:
        raise ValueError('O template ultrapassa o limite de canais.')
    channels = sorted(guild.channels, key=lambda c: (c.type == discord.ChannelType.category, c.position, c.id))
    return dict(
        guild_id=guild.id, guild_name=guild.name,
        created_at=datetime.now(timezone.utc).isoformat(), phase='deleting',
        channels=[dict(id=c.id, name=c.name, type=str(c.type), category_id=getattr(c, 'category_id', None)) for c in channels],
        roles=sorted(targets, key=lambda r: r['position']),
        protected_roles=protected, done_channels=[], done_roles=[])


def snapshot(guild):
    channels = []
    for c in guild.channels:
        overwrites = []
        for target, overwrite in c.overwrites.items():
            allow, deny = overwrite.pair()
            overwrites.append(dict(id=target.id, type='role' if isinstance(target, discord.Role) else 'member', allow=allow.value, deny=deny.value))
        channels.append(dict(id=c.id, name=c.name, type=str(c.type), position=c.position,
                             category_id=getattr(c, 'category_id', None), topic=getattr(c, 'topic', None), overwrites=overwrites))
    return dict(guild_id=guild.id, guild_name=guild.name, recorded_at=datetime.now(timezone.utc).isoformat(),
                notice='Referência estrutural: não contém mensagens, arquivos nem a atribuição de cargos aos membros. Não restaura o histórico apagado.',
                roles=[dict(id=r.id, name=r.name, position=r.position, permissions=r.permissions.value,
                            color=r.colour.value, managed=r.managed) for r in guild.roles], channels=channels)


def show_plan(plan):
    print('\nSERVIDOR: ' + plan['guild_name'] + ' | ID ' + str(plan['guild_id']))
    print('\nCANAIS E CATEGORIAS QUE SERÃO APAGADOS (inclui suas mensagens e tópicos):')
    done = set(plan['done_channels'])
    for item in plan['channels']:
        print(('  [já apagado] ' if item['id'] in done else '  ') + item['name'] + ' | ' + str(item['id']))
    print('\nCARGOS QUE SERÃO APAGADOS (suas atribuições aos membros serão perdidas):')
    done = set(plan['done_roles'])
    for item in plan['roles']:
        print(('  [já apagado] ' if item['id'] in done else '  ') + item['name'] + ' | ' + str(item['id']))
    print('\nCARGOS PRESERVADOS:')
    for item in plan['protected_roles']:
        print('  ' + item['name'] + ': ' + item['reason'])
    print('\nA exclusão de canais e mensagens é permanente. A referência estrutural não restaura mensagens.')
    print('Membros, emojis, integrações e configurações gerais do servidor permanecem.')


def start_rebuild(guild, previous, plan, path, save):
    # Write the reference before any Discord deletion. A failed disk write aborts.
    timestamp = datetime.now(timezone.utc).strftime('%Y%m%d-%H%M%S-%f')
    reference = path.parent / ('ESTRUTURA-ANTIGA-' + str(guild.id) + '-' + timestamp + '.json')
    reference.write_text(json.dumps(snapshot(guild), ensure_ascii=False, indent=2), encoding='utf-8')
    state = dict(guild_id=guild.id, roles={}, categories={}, channels={}, messages={}, rebuild=plan)
    for key in ['template_code', 'template_url']:
        if key in previous:
            state[key] = previous[key]
    state['rebuild']['structure_reference'] = reference.name
    save(path, state)
    print('Referência estrutural salva:', reference.name)
    return state


async def delete_approved(guild, state, path, save, answer):
    plan = state['rebuild']
    if plan['guild_id'] != guild.id or state['guild_id'] != guild.id:
        raise ValueError('O plano pertence a outro servidor.')
    if answer != approval_phrase(guild.id):
        raise ValueError('Confirmação incorreta. Nenhuma nova exclusão foi realizada.')
    if plan['phase'] != 'deleting':
        raise ValueError('A limpeza já terminou. Retome a criação sem apagar os itens novos.')
    own_ids = {r.id for r in guild.me.roles}
    protected_ids = {r['id'] for r in plan['protected_roles']} | own_ids | {guild.default_role.id}
    top = max(r.position for r in guild.roles if r.id in own_ids)
    for entry in plan['roles']:
        role = guild.get_role(entry['id'])
        if entry['id'] not in plan['done_roles'] and role is not None:
            if role.id in protected_ids or role.managed or role.position >= top:
                raise ValueError('Um cargo do plano está protegido ou acima do bot. Corrija a hierarquia antes de retomar.')
    for entry in plan['channels']:
        channel = guild.get_channel(entry['id'])
        if entry['id'] not in plan['done_channels'] and channel is not None and not channel.permissions_for(guild.me).manage_channels:
            raise ValueError('O bot perdeu acesso a um canal do plano. Corrija as permissões antes de retomar.')
    for kind, collection, completed in [('canal', 'channels', 'done_channels'), ('cargo', 'roles', 'done_roles')]:
        for item in plan[collection]:
            if item['id'] in plan[completed]:
                continue
            obj = guild.get_channel(item['id']) if kind == 'canal' else guild.get_role(item['id'])
            if obj is not None:
                if obj.guild.id != guild.id:
                    raise ValueError('Item pertence a outro servidor; limpeza interrompida.')
                if kind == 'cargo' and (obj.id in protected_ids or obj.managed):
                    raise ValueError('Um cargo passou a ser protegido. Confira os cargos do bot antes de retomar.')
                try:
                    await obj.delete(reason='Recriação Zyon confirmada pelo administrador para este servidor')
                except discord.NotFound:
                    pass  # A prior deletion succeeded before the local progress could be saved.
            plan[completed].append(item['id'])
            save(path, state)
            print('Apagado:', item['name'], flush=True)
    for key in ['roles', 'categories', 'channels', 'messages']:
        state[key] = {}
    plan['phase'] = 'building'
    save(path, state)
