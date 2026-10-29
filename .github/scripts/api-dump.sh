#!/usr/bin/env bash
# Prints the Minecraft / Fabric API signatures Cele-Tweaks depends on, straight from the jars
# Loom resolved for one version node. Used to port code and to verify mixin targets, which
# javac does not check (a wrong target only fails at game start).
set -uo pipefail
MC="$1"
echo "::group::jars for $MC"
JARS=$(find "$HOME/.gradle/caches/fabric-loom" "$PWD/.gradle" "$PWD/versions/$MC/.gradle" -name '*.jar' 2>/dev/null | grep -v -- '-sources' | sort -u)
MCJAR=""
for j in $JARS; do
  if unzip -l "$j" 2>/dev/null | grep -q 'net/minecraft/client/Minecraft.class'; then
    case "$j" in *"$MC"*) MCJAR="$j" ;; esac
  fi
done
[ -z "$MCJAR" ] && for j in $JARS; do unzip -l "$j" 2>/dev/null | grep -q 'net/minecraft/client/Minecraft.class' && MCJAR="$j"; done
echo "minecraft jar: $MCJAR"
echo "::endgroup::"
[ -z "$MCJAR" ] && { echo "no minecraft jar found"; exit 0; }

CP="$MCJAR"
for j in $(find "$HOME/.gradle/caches/modules-2" -name '*.jar' 2>/dev/null | grep -iE 'lwjgl|sdl|fastutil|brigadier|datafixer|authlib|jopt|joml|gson|guava|logging|slf4j' | grep -v -- '-sources'); do CP="$CP:$j"; done

sig() { # class [grep-regex]
  local out; out=$(javap -cp "$CP" -p "$1" 2>&1)
  if [ -n "${2:-}" ]; then out=$(printf '%s\n' "$out" | grep -E "class |interface |record |$2"); fi
  printf '%s\n' "$out"
}

echo "::group::[$MC] class names of interest"
unzip -Z1 "$MCJAR" | grep -E '^net/minecraft/(resources/[A-Z]|client/gui/[A-Za-z]*Graphics|client/input/|client/gui/screens/Screen\.class|client/KeyboardHandler|client/Gui\b)' | sed 's/\.class$//' | grep -v '\$[0-9]' | sort
unzip -Z1 "$MCJAR" | grep -iE '^com/mojang/blaze3d/platform/(InputConstants|Window)' | sed 's/\.class$//' | sort
echo "::endgroup::"

echo "::group::[$MC] input libraries on the compile classpath"
find "$HOME/.gradle/caches/modules-2" -name '*.jar' 2>/dev/null | grep -iE 'lwjgl|sdl' | grep -v -- '-sources' | sed 's|.*/||' | sort -u
echo "::endgroup::"

echo "::group::[$MC] mixin targets"
sig net.minecraft.client.KeyboardHandler 'keyPress|charTyped|void [a-z]+\('
sig net.minecraft.client.gui.Gui 'Scoreboard|Sidebar|setOverlayMessage|ActionBar'
sig net.minecraft.world.scores.PlayerTeam 'getColor'
sig net.minecraft.client.gui.components.ChatComponent 'addMessage'
sig net.minecraft.client.Minecraft 'pickBlock|screen|Screen|hasShift|hasControl|hasAlt| execute|levelRenderer|hitResult|options|player|level '
sig net.minecraft.client.renderer.entity.EntityRenderDispatcher 'shouldRender'
sig net.minecraft.world.level.block.state.BlockBehaviour 'getRenderShape'
echo "::endgroup::"

echo "::group::[$MC] APIs used by features"
sig net.minecraft.client.gui.screens.Screen 'public|protected'
sig net.minecraft.client.gui.components.Renderable
sig net.minecraft.world.entity.player.Player 'Message|message|Overlay|overlay'
sig net.minecraft.client.player.LocalPlayer 'Message|message|Overlay|overlay'
sig net.minecraft.client.renderer.LevelRenderer 'public .*(changed|Changed|reload|Reload|dirty|Dirty|needsUpdate)'
sig net.minecraft.world.entity.player.Inventory 'public .*(Slot|slot|Selected|selected|Hotbar|hotbar|Selection)'
sig 'com.mojang.blaze3d.platform.InputConstants' 'public'
sig 'com.mojang.blaze3d.platform.InputConstants$Type' 'public'
sig 'com.mojang.blaze3d.platform.InputConstants$Key' 'public'
sig net.minecraft.client.OptionInstance 'public .* (get|set)\('
sig net.minecraft.client.Options 'sensitivity|keyUp|keyLeft|keyRight'
echo "::endgroup::"

GRAPHICS=$(unzip -Z1 "$MCJAR" | grep -E '^net/minecraft/client/gui/[A-Za-z]*Graphics[A-Za-z]*\.class$' | sed 's/\.class$//; s|/|.|g')
for g in $GRAPHICS; do
  echo "::group::[$MC] $g"
  sig "$g" 'public .*(drawString|drawCenteredString|fill\(|pose\(|guiWidth|guiHeight)'
  echo "::endgroup::"
done
for r in $(unzip -Z1 "$MCJAR" | grep -E '^net/minecraft/resources/[A-Z][A-Za-z]*\.class$' | sed 's/\.class$//; s|/|.|g'); do
  echo "::group::[$MC] $r"
  sig "$r" 'public static .*(tryParse|parse|fromNamespaceAndPath|withDefaultNamespace)'
  echo "::endgroup::"
done

echo "::group::[$MC] fabric client command api"
for j in $(find "$HOME/.gradle/caches" -name 'fabric-command-api-v2*.jar' 2>/dev/null | grep -v -- '-sources'); do
  echo "$j"; unzip -Z1 "$j" | grep -E 'api/client/command/v2/[A-Za-z]+\.class$'
  for c in $(unzip -Z1 "$j" | grep -E 'api/client/command/v2/[A-Za-z]+\.class$' | sed 's/\.class$//; s|/|.|g'); do javap -cp "$j:$CP" "$c" 2>&1 | grep -E 'interface|class|literal|argument|EVENT|register'; done
done
echo "::endgroup::"

echo "::group::[$MC] lookups for open porting questions"
sig net.minecraft.client.gui.GuiGraphicsExtractor 'public .*(text|Text|String|string)\('
sig net.minecraft.client.input.KeyEvent 'public'
sig net.minecraft.world.scores.TeamColor 'public'
sig net.minecraft.client.renderer.LevelRenderer 'public'
sig net.minecraft.client.multiplayer.MultiPlayerGameMode 'CarriedItem|carriedItem|Pick|pick'
sig net.minecraft.client.gui.Gui 'public|Screen|screen|Sidebar|Scoreboard'
sig com.mojang.blaze3d.platform.InputConstants 'KEY_ESCAPE|PRESS|RELEASE|UNKNOWN|isKeyDown'
sig net.minecraft.client.renderer.GameRenderer 'Camera'
sig net.minecraft.client.Minecraft 'Camera|BlockColors'
echo "-- classes whose name mentions sidebar / scoreboard / hud (client side)"
unzip -Z1 "$MCJAR" | grep -iE '^net/minecraft/client/.*(sidebar|scoreboard|/hud/)' | grep -v '\$[0-9]' | sed 's/\.class$//'
echo "-- members that return the current Screen, re-mesh chunks, or draw the sidebar"
CLASSES=$(unzip -Z1 "$MCJAR" | grep -E '^net/minecraft/client/([A-Za-z]+|gui/[A-Za-z]+|gui/(components|screens|hud)/[A-Za-z]+|renderer/[A-Za-z]+|renderer/chunk/[A-Za-z]+)\.class$' | sed 's/\.class$//; s|/|.|g')
javap -cp "$CP" -p $CLASSES 2>/dev/null | awk '
  /^(public |protected |private |abstract |final |sealed |non-sealed )*(class|interface|enum|record) /{c=$0; next}
  /gui\.screens\.Screen [a-zA-Z]+\(\);|gui\.screens\.Screen [a-zA-Z]+;|[Ss]idebar|allChanged|[Dd]irty[A-Za-z]*\(\)|[Rr]ebuild[A-Za-z]*\(|[Rr]eload[A-Za-z]*\(\)/ {print c " ::" $0}'
echo "::endgroup::"

echo "::group::[$MC] range overlay / new feature lookups"
sig net.minecraft.client.gui.screens.Screen 'onFilesDrop'
sig net.minecraft.client.multiplayer.ClientChunkCache 'getChunk'
sig net.minecraft.world.level.chunk.LevelChunk 'getBlockEntities'
sig net.minecraft.world.level.block.entity.BeaconBlockEntity 'levels|Levels|getLevel|int '
sig net.minecraft.world.level.block.LightningRodBlock 'RANGE|static final int'
sig net.minecraft.client.renderer.ShapeRenderer 'public static'
echo "-- gizmos"
unzip -Z1 "$MCJAR" | grep -iE '^net/minecraft/gizmos/[A-Za-z]+\.class$' | sed 's/\.class$//'
sig net.minecraft.gizmos.Gizmos 'public static'
sig net.minecraft.gizmos.GizmoStyle 'public static'
echo "-- render types with lines"
for c in net.minecraft.client.renderer.rendertype.RenderTypes net.minecraft.client.renderer.RenderType net.minecraft.client.renderer.RenderTypes; do sig "$c" 'public static .*(lines|Lines|LINES|debugQuads|debugFilledBox)' ; done
echo "-- fabric rendering / fluid / lifecycle api"
for pat in 'fabric-rendering-v1' 'fabric-rendering-fluids-v1' 'fabric-lifecycle-events-v1'; do
  for j in $(find "$HOME/.gradle/caches" -name "${pat}-*.jar" 2>/dev/null | grep -v -- '-sources' | sort -u); do
    echo "## $j"
    CLS=$(unzip -Z1 "$j" | grep -E '/api/.*\.class$' | grep -v '\$' | sed 's/\.class$//; s|/|.|g')
    echo "$CLS"
    for c in $(printf '%s\n' "$CLS" | grep -iE 'World|Level|Fluid|ClientLifecycle'); do javap -cp "$j:$CP" "$c" 2>&1 | grep -vE '^Compiled from'; done
  done
done
echo "::endgroup::"

echo "::group::[$MC] fluid rendering and gizmo collection internals"
for c in net.minecraft.client.renderer.block.FluidRenderer 'net.minecraft.client.renderer.block.FluidRenderer$Output' net.minecraft.client.renderer.block.FluidModel 'net.minecraft.client.renderer.block.FluidModel$Unbaked' net.minecraft.client.renderer.block.BlockAndTintGetter; do sig "$c"; done
for pat in 'fabric-rendering-fluids-v1'; do
  for j in $(find "$HOME/.gradle/caches" -name "${pat}-*.jar" 2>/dev/null | grep -v -- '-sources' | sort -u); do
    javap -cp "$j:$CP" net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler net.fabricmc.fabric.api.client.render.fluid.v1.FluidRendering net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry 2>&1
  done
done
echo "-- fabric level render / extraction events (static fields and callback interfaces)"
for j in $(find "$HOME/.gradle/caches" -name 'fabric-rendering-v1-*.jar' 2>/dev/null | grep -v -- '-sources' | sort -u); do
  for c in $(unzip -Z1 "$j" | grep -E 'api/client/rendering/v1/level/Level(Render|Extraction)Events(\$[A-Za-z]+)?\.class$' | sed 's/\.class$//; s|/|.|g'); do javap -cp "$j:$CP" "$c" 2>&1; done
done
echo "-- who opens a gizmo collector (method :: call)"
javap -cp "$CP" -c -p net.minecraft.client.Minecraft net.minecraft.client.renderer.LevelRenderer net.minecraft.client.renderer.GameRenderer net.minecraft.client.renderer.debug.DebugRenderer 2>/dev/null | awk '
  /^  [a-z].*\(.*\);$/ || /^  (public|private|protected|static|final| )*[A-Za-z<].*\(.*\)( throws .*)?;$/ {m=$0}
  /Gizmos\.withCollector|GizmoCollector|SimpleGizmoCollector|drainGizmos|Gizmos\.addGizmo/ {print m " :: " $NF}' | sort -u
sig net.minecraft.client.Camera 'public .*(position|Position|getX|getY|getZ)\('
echo "::endgroup::"

echo "::group::[$MC] misc lookups"
sig net.minecraft.resources.ResourceKey 'public'
sig net.minecraft.network.chat.TextColor 'public'
echo "::endgroup::"
