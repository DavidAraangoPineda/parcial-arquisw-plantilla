# Corre la colección de Postman con Newman y guarda el reporte como evidencia.
# Requiere Node.js. Uso:  .\scripts\probar-api.ps1
param([string]$Base = "http://localhost:8080")

$raiz = Split-Path $PSScriptRoot -Parent
node "$raiz\pruebas\generar-coleccion.js"
New-Item -ItemType Directory -Force "$raiz\docs\evidencias" | Out-Null

npx -y newman@6 run "$raiz\pruebas\parcial.postman_collection.json" `
    --env-var "baseUrl=$Base" `
    --reporters cli,json `
    --reporter-json-export "$raiz\docs\evidencias\newman-resultado.json"
