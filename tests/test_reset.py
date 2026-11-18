import asyncio,importlib.util,json,sys,tempfile,unittest
from pathlib import Path
from unittest.mock import AsyncMock,patch
import discord
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT))
import aplicar as app
import limpeza as clean
class Entity:
 def __init__(self,**kw):self.__dict__.update(kw)
class Role(Entity):
 async def delete(self,**kw):self.guild.deleted.append(('role',self.id));self.guild.roles.remove(self)
class Channel(Entity):
 def permissions_for(self,member):return discord.Permissions.all() if self.can_manage else discord.Permissions.none()
 async def delete(self,**kw):
  if self.id==self.guild.fail_id:raise OSError('connection interrupted')
  self.guild.deleted.append(('channel',self.id));self.guild.channels.remove(self)
  if self.type==discord.ChannelType.category:self.guild.categories.remove(self)
 async def send(self,content,allowed_mentions):
  assert not allowed_mentions.everyone
  self.guild.posts.append((self.id,content));return Entity(id=len(self.guild.posts)+90000)
class Guild:
 id=123456789012345678;name='Zyon Test';features=[]
 def __init__(self):
  self.roles=[];self.channels=[];self.categories=[];self.deleted=[];self.posts=[];self.next=1000;self.fail_id=None
  self.default_role=self.role('@everyone',id=self.id,position=0)
  self.bot_role=self.role('Zyon Setup',id=2,position=100,managed=True)
  self.integration=self.role('Other bot integration',id=3,position=200,managed=True)
  self.me=Entity(id=42,roles=[self.default_role,self.bot_role],guild_permissions=discord.Permissions.all())
 def role(self,name,id=None,position=1,managed=False,**kw):
  self.next+=1;r=Role(id=id or self.next,name=name,position=position,managed=managed,permissions=kw.pop('permissions',discord.Permissions.none()),colour=kw.pop('colour',discord.Colour(0)),guild=self,**kw);self.roles.append(r);return r
 def channel(self,name,type=discord.ChannelType.text,category=None,**kw):
  self.next+=1;c=Channel(id=self.next,name=name,type=type,category=category,category_id=category.id if category else None,position=len(self.channels),guild=self,overwrites=kw.pop('overwrites',{}),can_manage=True,**kw);self.channels.append(c)
  if type==discord.ChannelType.category:self.categories.append(c)
  return c
 def get_role(self,id):return next((r for r in self.roles if r.id==id),None)
 def get_channel(self,id):return next((c for c in self.channels if c.id==id),None)
 async def create_role(self,**kw):return self.role(**kw)
 async def create_category(self,name,**kw):return self.channel(name,type=discord.ChannelType.category,**kw)
 async def create_text_channel(self,name,**kw):return self.channel(name,**kw)
 async def create_voice_channel(self,name,**kw):return self.channel(name,type=discord.ChannelType.voice,**kw)
 async def fetch_roles(self):return list(self.roles)
 async def fetch_channels(self):return list(self.channels)
 async def fetch_member(self,id):return self.me
class ResetTests(unittest.IsolatedAsyncioTestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.path=Path(self.temp.name)/'state.json';self.g=Guild();self.m=app.load_manifest();self.old=self.g.role('Old Admin',position=50);self.category=self.g.channel('Old category',type=discord.ChannelType.category);self.text=self.g.channel('old-chat',category=self.category)
 def begin(self):
  plan=clean.make_plan(self.g,self.m)
  return clean.start_rebuild(self.g,app.state_for(self.path,self.g.id),plan,self.path,app.save_state)
 async def test_approved_cleanup_then_builds_new_template(self):
  state=self.begin();await clean.delete_approved(self.g,state,self.path,app.save_state,clean.approval_phrase(self.g.id))
  self.assertEqual(state['rebuild']['phase'],'building');self.assertEqual(self.g.roles,[self.g.default_role,self.g.bot_role,self.g.integration]);self.assertFalse(self.g.channels)
  self.assertEqual(self.g.deleted[-1],('role',self.old.id));self.assertEqual(self.g.deleted[-2],('channel',self.category.id))
  view=await clean.inventory(self.g);self.assertEqual(app.preflight(view,self.m,state),(10,33));await app.apply_structure(view,self.m,state,self.path)
  self.assertEqual(len(self.g.roles),13);self.assertEqual(len(self.g.channels),33)
  staff=self.g.get_channel(state['categories']['staff']);self.assertFalse(staff.overwrites[self.g.default_role].view_channel)
  self.assertTrue(next(self.path.parent.glob('ESTRUTURA-ANTIGA-*.json')).exists())
 async def test_wrong_phrase_cannot_delete(self):
  state=self.begin()
  with self.assertRaises(ValueError):await clean.delete_approved(self.g,state,self.path,app.save_state,'APAGAR')
  self.assertFalse(self.g.deleted)
 async def test_protected_roles_and_hierarchy(self):
  plan=clean.make_plan(self.g,self.m);self.assertEqual({r['id'] for r in plan['roles']},{self.old.id});self.assertEqual({r['id'] for r in plan['protected_roles']},{self.g.id,2,3})
  self.old.position=150
  with self.assertRaises(ValueError):clean.make_plan(self.g,self.m)
  self.assertFalse(self.g.deleted)
 async def test_community_or_inaccessible_channel_fails_before_deletion(self):
  self.g.features=['COMMUNITY']
  with self.assertRaises(ValueError):clean.make_plan(self.g,self.m)
  self.g.features=[];self.text.can_manage=False
  with self.assertRaises(ValueError):clean.make_plan(self.g,self.m)
  self.assertFalse(self.g.deleted)
 async def test_interruption_keeps_fixed_ids_and_preserves_new_resources(self):
  state=self.begin();self.g.fail_id=self.category.id
  with self.assertRaises(OSError):await clean.delete_approved(self.g,state,self.path,app.save_state,clean.approval_phrase(self.g.id))
  state=app.state_for(self.path,self.g.id);self.assertEqual(state['rebuild']['phase'],'deleting')
  new_channel=self.g.channel('created-after-plan');new_role=self.g.role('created-after-plan')
  self.g.fail_id=None;await clean.delete_approved(await clean.inventory(self.g),state,self.path,app.save_state,clean.approval_phrase(self.g.id))
  self.assertIn(new_channel,self.g.channels);self.assertIn(new_role,self.g.roles)
 async def test_missing_already_deleted_id_is_safe_on_resume(self):
  state=self.begin();self.g.channels.remove(self.text)
  await clean.delete_approved(await clean.inventory(self.g),state,self.path,app.save_state,clean.approval_phrase(self.g.id));self.assertEqual(state['rebuild']['phase'],'building')
 async def test_different_server_or_build_phase_cannot_purge(self):
  state=self.begin();state['rebuild']['guild_id']=999
  with self.assertRaises(ValueError):await clean.delete_approved(self.g,state,self.path,app.save_state,clean.approval_phrase(self.g.id))
  state['rebuild']['guild_id']=self.g.id;state['rebuild']['phase']='building'
  with self.assertRaises(ValueError):await clean.delete_approved(self.g,state,self.path,app.save_state,clean.approval_phrase(self.g.id))
  self.assertFalse(self.g.deleted)
 async def test_new_role_protection_stops_before_any_more_deletions(self):
  state=self.begin();self.g.me.roles.append(self.old)
  with self.assertRaises(ValueError):await clean.delete_approved(self.g,state,self.path,app.save_state,clean.approval_phrase(self.g.id))
  self.assertFalse(self.g.deleted)
 async def test_failed_snapshot_write_prevents_cleanup(self):
  plan=clean.make_plan(self.g,self.m)
  with patch.object(Path,'write_text',side_effect=OSError('disk full')):
   with self.assertRaises(OSError):clean.start_rebuild(self.g,{},plan,self.path,app.save_state)
  self.assertFalse(self.g.deleted)
 async def test_ready_build_resume_never_calls_cleanup(self):
  state=app.state_for(self.path,self.g.id);state['rebuild']={'phase':'building'};app.save_state(self.path,state)
  p=self.path.parent/('progresso-'+str(self.g.id)+'.json');app.save_state(p,state)
  client=app.SetupClient(self.g.id,self.m,rebuild=True);client.get_guild=lambda id:self.g;client.close=AsyncMock()
  with patch.object(app,'BASE',self.path.parent),patch.object(app,'inventory',AsyncMock(return_value=self.g)),patch.object(app,'apply_structure',AsyncMock()) as create,patch.object(app,'delete_approved',AsyncMock()) as delete,patch.object(app,'native_template',AsyncMock(return_value='https://discord.new/example')),patch('builtins.input',return_value='CRIAR'):
   await client.on_ready();create.assert_awaited_once();delete.assert_not_called()
  self.assertEqual(client.result,0);self.assertFalse(self.g.deleted)
 async def test_cancelling_new_plan_changes_nothing(self):
  client=app.SetupClient(self.g.id,self.m,rebuild=True);client.get_guild=lambda id:self.g;client.close=AsyncMock()
  with patch.object(app,'BASE',self.path.parent),patch.object(app,'inventory',AsyncMock(return_value=self.g)),patch('builtins.input',return_value='cancelar'):
   await client.on_ready()
  self.assertEqual(client.result,0);self.assertFalse(self.g.deleted);self.assertFalse(list(self.path.parent.glob('ESTRUTURA-ANTIGA-*.json')))
class InstanceLockTests(unittest.TestCase):
 def test_another_instance_cannot_run_in_the_same_folder(self):
  import iniciar
  with tempfile.TemporaryDirectory() as d,patch.object(iniciar,'BASE',Path(d)):
   with iniciar.instance_lock():
    with self.assertRaises(RuntimeError):
     with iniciar.instance_lock():pass
   with iniciar.instance_lock():pass
if __name__=='__main__':unittest.main()

