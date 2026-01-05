param(
    [string]$Root = (Resolve-Path "$PSScriptRoot\.."),
    [int]$Threshold = 200
)

$srcRoot = Join-Path $Root "src\main\java"
if (-not (Test-Path $srcRoot)) {
    throw "Cannot find $srcRoot"
}

# Very lightweight approximation of cyclomatic complexity (v(G)):
# v(G) = 1 + (# of decision points)
# decision points here: if, for, foreach, while, catch, case, &&, ||, ?, "else if"
# Note: This is NOT a full Java parser; it’s meant to be a reproducible quick metric.

$decisionRegex = [regex]"\b(if|for|while|catch|case)\b|&&|\|\||\?"

$files = Get-ChildItem -Path $srcRoot -Recurse -Filter *.java

$results = foreach ($f in $files) {
    $text = Get-Content -LiteralPath $f.FullName -Raw

    # remove block comments and line comments (best-effort)
    $text = [regex]::Replace($text, "/\*.*?\*/", "", [System.Text.RegularExpressions.RegexOptions]::Singleline)
    $text = [regex]::Replace($text, "//.*?$", "", [System.Text.RegularExpressions.RegexOptions]::Multiline)

    $matches = $decisionRegex.Matches($text).Count
    $vg = 1 + $matches

    [pscustomobject]@{
        File = $f.FullName.Substring($Root.Length).TrimStart('\\')
        vG = $vg
        DecisionPoints = $matches
    }
}

$total = ($results | Measure-Object -Property vG -Sum).Sum

$top = $results | Sort-Object vG -Descending | Select-Object -First 20

Write-Host "Total v(G) sum (approx) = $total"
Write-Host "Threshold = $Threshold"
Write-Host ("Result: {0}" -f ($(if ($total -gt $Threshold) { "PASS (> threshold)" } else { "FAIL (<= threshold)" })))
Write-Host ""
Write-Host "Top 20 files by v(G):"
$top | Format-Table -AutoSize

$outDir = Join-Path $Root "target\metrics"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$results | Sort-Object vG -Descending | Export-Csv -NoTypeInformation -Encoding UTF8 -Path (Join-Path $outDir "vg_by_file.csv")
[pscustomobject]@{ Total_vG_Sum = $total; Threshold = $Threshold; Timestamp = (Get-Date).ToString("s") } |
    ConvertTo-Json | Out-File -Encoding UTF8 (Join-Path $outDir "vg_summary.json")

