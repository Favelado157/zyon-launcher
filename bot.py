import os
import asyncio
import shutil
from pathlib import Path
import imageio_ffmpeg
import discord
import yt_dlp

from collections import defaultdict, deque
from discord import app_commands
from discord.ext import commands
from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent
load_dotenv(BASE_DIR / ".env")
FFMPEG_EXE = shutil.which("ffmpeg") or imageio_ffmpeg.get_ffmpeg_exe()

TOKEN = os.getenv("DISCORD_TOKEN")

intents = discord.Intents.default()

bot = commands.Bot(
    command_prefix="!",
    intents=intents,
    allowed_mentions=discord.AllowedMentions.none()
)

queues = defaultdict(deque)
current_tracks = {}
volume_levels = defaultdict(lambda: 0.5)

YDL_OPTIONS = {
    "format": "bestaudio/best",
    "noplaylist": True,
    "quiet": True,
    "no_warnings": True,
    "default_search": "ytsearch",
    "source_address": "0.0.0.0"
}

FFMPEG_OPTIONS = {
    "before_options": "-reconnect 1 -reconnect_streamed 1 -reconnect_delay_max 5",
    "options": "-vn"
}


async def extract_music(query):
    def get_info():
        with yt_dlp.YoutubeDL(YDL_OPTIONS) as ydl:
            info = ydl.extract_info(query, download=False)

            if "entries" in info:
                info = next(
                    (entry for entry in info["entries"] if entry),
                    None
                )

            if not info:
                raise ValueError("Música não encontrada.")

            return {
                "title": info.get("title", "Desconhecida"),
                "url": info["url"],
                "webpage_url": info.get("webpage_url", ""),
                "duration": info.get("duration", 0),
                "thumbnail": info.get("thumbnail")
            }

    return await asyncio.to_thread(get_info)


def create_embed(track, title="🎵 Tocando agora"):
    embed = discord.Embed(
        title=title,
        description=f"**[{track['title']}]({track['webpage_url']})**",
        color=discord.Color.blurple()
    )

    if track.get("thumbnail"):
        embed.set_thumbnail(url=track["thumbnail"])

    duration = track.get("duration")

    if duration:
        minutes, seconds = divmod(int(duration), 60)
        embed.add_field(
            name="Duração",
            value=f"{minutes:02d}:{seconds:02d}"
        )

    return embed


async def play_next(guild):
    guild_id = guild.id
    voice = guild.voice_client

    if not voice or not voice.is_connected():
        return

    if not queues[guild_id]:
        current_tracks.pop(guild_id, None)
        return

    track = queues[guild_id].popleft()
    current_tracks[guild_id] = track

    try:
        source = discord.PCMVolumeTransformer(
            discord.FFmpegPCMAudio(
                track["url"],
                executable=FFMPEG_EXE,
                **FFMPEG_OPTIONS
            ),
            volume=volume_levels[guild_id]
        )

        def after_play(error):
            if error:
                print(f"Erro na reprodução: {error}")

            future = asyncio.run_coroutine_threadsafe(
                play_next(guild),
                bot.loop
            )

            try:
                future.result()
            except Exception as exc:
                print(f"Erro ao avançar fila: {exc}")

        voice.play(source, after=after_play)

    except Exception as error:
        print(f"Erro ao iniciar música: {error}")
        await play_next(guild)


@bot.event
async def on_ready():
    print(f"Bot conectado: {bot.user}")

    try:
        synced = await bot.tree.sync()
        print(f"{len(synced)} comandos sincronizados.")
    except Exception as error:
        print(f"Erro ao sincronizar: {error}")


@app_commands.guild_only()
@bot.tree.command(
    name="play",
    description="Toca uma música ou adiciona à fila."
)
@app_commands.describe(
    musica="Nome da música ou link"
)
async def play(interaction: discord.Interaction, musica: str):

    await interaction.response.defer()

    if not interaction.user.voice:
        return await interaction.followup.send(
            "❌ Entre em um canal de voz primeiro."
        )

    try:
        track = await extract_music(musica)

        voice = interaction.guild.voice_client

        if voice is None:
            voice = await interaction.user.voice.channel.connect()

        elif voice.channel != interaction.user.voice.channel:
            return await interaction.followup.send(
                "❌ Você precisa estar no meu canal de voz."
            )

        guild_id = interaction.guild.id

        queues[guild_id].append(track)

        if not voice.is_playing() and not voice.is_paused():
            await play_next(interaction.guild)

            await interaction.followup.send(
                embed=create_embed(track)
            )
        else:
            embed = create_embed(
                track,
                "📀 Música adicionada à fila"
            )

            await interaction.followup.send(embed=embed)

    except Exception as error:
        await interaction.followup.send(
            f"❌ Não consegui tocar essa música: {error}"
        )


@app_commands.guild_only()
@bot.tree.command(name="pause", description="Pausa a música.")
async def pause(interaction: discord.Interaction):

    voice = interaction.guild.voice_client

    if voice and voice.is_playing():
        voice.pause()
        await interaction.response.send_message(
            "⏸️ Música pausada."
        )
    else:
        await interaction.response.send_message(
            "❌ Não existe música tocando.",
            ephemeral=True
        )


@app_commands.guild_only()
@bot.tree.command(name="resume", description="Continua a música.")
async def resume(interaction: discord.Interaction):

    voice = interaction.guild.voice_client

    if voice and voice.is_paused():
        voice.resume()
        await interaction.response.send_message(
            "▶️ Música retomada."
        )
    else:
        await interaction.response.send_message(
            "❌ Não existe música pausada.",
            ephemeral=True
        )


@app_commands.guild_only()
@bot.tree.command(name="skip", description="Pula a música atual.")
async def skip(interaction: discord.Interaction):

    voice = interaction.guild.voice_client

    if voice and (voice.is_playing() or voice.is_paused()):
        voice.stop()

        await interaction.response.send_message(
            "⏭️ Música pulada!"
        )
    else:
        await interaction.response.send_message(
            "❌ Não há música para pular.",
            ephemeral=True
        )


@app_commands.guild_only()
@bot.tree.command(name="stop", description="Para e limpa a fila.")
async def stop(interaction: discord.Interaction):

    guild_id = interaction.guild.id
    voice = interaction.guild.voice_client

    queues[guild_id].clear()
    current_tracks.pop(guild_id, None)

    if voice:
        voice.stop()

    await interaction.response.send_message(
        "⏹️ Reprodução parada e fila limpa."
    )


@app_commands.guild_only()
@bot.tree.command(name="queue", description="Mostra a fila.")
async def queue(interaction: discord.Interaction):

    guild_id = interaction.guild.id
    tracks = list(queues[guild_id])
    current = current_tracks.get(guild_id)

    embed = discord.Embed(
        title="📜 Fila de músicas",
        color=discord.Color.blurple()
    )

    if current:
        embed.add_field(
            name="🎵 Tocando agora",
            value=current["title"],
            inline=False
        )

    if tracks:
        description = "\n".join(
            f"`{i}.` {track['title']}"
            for i, track in enumerate(tracks[:15], start=1)
        )

        embed.add_field(
            name="Próximas músicas",
            value=description,
            inline=False
        )
    else:
        embed.add_field(
            name="Fila",
            value="Nenhuma música na fila."
        )

    await interaction.response.send_message(embed=embed)


@app_commands.guild_only()
@bot.tree.command(name="volume", description="Altera o volume.")
@app_commands.describe(nivel="Volume de 1 até 100")
async def volume(interaction: discord.Interaction, nivel: app_commands.Range[int, 1, 100]):

    guild_id = interaction.guild.id
    voice = interaction.guild.voice_client

    volume_levels[guild_id] = nivel / 100

    if voice and voice.source:
        voice.source.volume = nivel / 100

    await interaction.response.send_message(
        f"🔊 Volume ajustado para **{nivel}%**."
    )


@app_commands.guild_only()
@bot.tree.command(name="leave", description="Desconecta o bot.")
async def leave(interaction: discord.Interaction):

    voice = interaction.guild.voice_client

    if voice:
        queues[interaction.guild.id].clear()
        current_tracks.pop(interaction.guild.id, None)

        await voice.disconnect()

        await interaction.response.send_message(
            "👋 Saí do canal de voz."
        )
    else:
        await interaction.response.send_message(
            "❌ Não estou em um canal de voz.",
            ephemeral=True
        )


def main():
    if not TOKEN:
        raise RuntimeError("Token ausente. Execute INICIAR.bat para configurar.")
    try:
        bot.run(TOKEN)
    except discord.LoginFailure:
        print("Token recusado pelo Discord. Execute CONFIGURAR_TOKEN.bat e cole um token de bot valido.")
        raise SystemExit(1)
    except discord.PrivilegedIntentsRequired:
        print("O Discord recusou os intents. Este bot so precisa dos intents padrao e de voz.")
        raise SystemExit(1)


if __name__ == "__main__":
    main()