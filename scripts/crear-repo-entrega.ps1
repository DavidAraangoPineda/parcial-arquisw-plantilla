# Crea el repositorio de entrega a partir de esta plantilla (copia limpia, sin historial),
# lo clona al lado de la plantilla y copia las notas locales que no van en git.
# Uso (desde la carpeta de la plantilla):  .\scripts\crear-repo-entrega.ps1 -Nombre parcial-arquisw-final
param(
    [string]$Nombre = "parcial-arquisw-final",
    [ValidateSet("public", "private")][string]$Visibilidad = "public"
)

$plantilla = Split-Path $PSScriptRoot -Parent
$padre = Split-Path $plantilla -Parent
$destino = Join-Path $padre $Nombre

if (Test-Path $destino) { Write-Host "Ya existe $destino"; exit 1 }

# Liberar los puertos si la plantilla está corriendo
Push-Location $plantilla
docker compose down 2>$null | Out-Null
Pop-Location

Push-Location $padre
gh repo create $Nombre "--$Visibilidad" --template DavidAraangoPineda/parcial-arquisw-plantilla --clone
Pop-Location
if (-not (Test-Path $destino)) {
    # GitHub a veces tarda unos segundos en copiar la plantilla antes de poder clonar
    Start-Sleep 5
    git clone "https://github.com/DavidAraangoPineda/$Nombre.git" $destino
}

foreach ($archivo in "CLAUDE.md", "CLAUDE.local.md") {
    if (Test-Path "$plantilla\$archivo") { Copy-Item "$plantilla\$archivo" $destino }
}

Write-Host ""
Write-Host "Listo: $destino"
Write-Host "Siguiente:  cd $destino ; docker compose up -d --build"
