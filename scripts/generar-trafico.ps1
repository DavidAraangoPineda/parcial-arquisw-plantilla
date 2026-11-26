# Genera tráfico variado contra la API para que Grafana muestre datos (peticiones, latencia,
# errores, órdenes y logs). Uso:  .\scripts\generar-trafico.ps1 -Segundos 120
param(
    [int]$Segundos = 90,
    [string]$Base = "http://localhost:8080"
)

$ErrorActionPreference = "SilentlyContinue"
$fin = (Get-Date).AddSeconds($Segundos)
$n = 0
$json = "application/json"

function Llamar($metodo, $url, $cuerpo) {
    try {
        if ($cuerpo) { Invoke-RestMethod -Method $metodo -Uri $url -ContentType $json -Body ($cuerpo | ConvertTo-Json) | Out-Null }
        else { Invoke-RestMethod -Method $metodo -Uri $url | Out-Null }
    } catch { }
    $script:n++
}

Write-Host "Generando tráfico durante $Segundos s contra $Base ..."
while ((Get-Date) -lt $fin) {
    Llamar GET "$Base/api"
    Llamar GET "$Base/api/v1/productos"
    Llamar GET "$Base/api/v2/productos?page=$(Get-Random -Maximum 2)&size=3"
    Llamar GET "$Base/api/v2/productos?categoria=Tecnología"
    Llamar GET "$Base/api/v1/productos/$(Get-Random -Minimum 1 -Maximum 9)"

    # Flujo de una orden: crear -> pagar -> (enviar | cancelar)
    try {
        $orden = Invoke-RestMethod -Method Post -Uri "$Base/api/v1/ordenes" -ContentType $json `
            -Body (@{ cliente = "Carga"; productoId = (Get-Random -Minimum 1 -Maximum 9); cantidad = 1 } | ConvertTo-Json)
        $n++
        if ($orden._links.pagar) { Llamar POST $orden._links.pagar.href }
        if ((Get-Random -Maximum 2) -eq 0) { Llamar POST "$Base/api/v1/ordenes/$($orden.id)/enviar" }
        else { Llamar POST "$Base/api/v1/ordenes/$($orden.id)/cancelar" }
    } catch { $n++ }

    # Errores intencionales para ver 4xx en el dashboard
    Llamar GET "$Base/api/v2/productos/999999"
    Llamar POST "$Base/api/v1/productos" @{ nombre = ""; precio = -1 }

    Start-Sleep -Milliseconds 300
}
Write-Host "Listo: $n peticiones enviadas."
