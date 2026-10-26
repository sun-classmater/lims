# Build the LIMS technical summary report from the school template.
# Content, image paths and captions live in tech-report-content.txt (UTF-8),
# so this script stays ASCII-only for Windows PowerShell 5.1.

$ErrorActionPreference = 'Stop'

$contentFile = Join-Path $PSScriptRoot 'tech-report-content.txt'
$lines = Get-Content -LiteralPath $contentFile -Encoding UTF8

$template = ''
$outputName = ''
$blocks = @()

foreach ($line in $lines) {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    $index = $line.IndexOf('|')
    if ($index -lt 1) { continue }
    $key = $line.Substring(0, $index)
    $text = $line.Substring($index + 1)
    if ($key -eq 'template') { $template = $text }
    elseif ($key -eq 'output') { $outputName = $text }
    else { $blocks += @{ s = $key; t = $text } }
}

if (-not (Test-Path -LiteralPath $template)) {
    throw "Template not found: $template"
}

$output = Join-Path $PSScriptRoot $outputName
Copy-Item -LiteralPath $template -Destination $output -Force

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0

try {
    $doc = $word.Documents.Open($output)

    # Remove everything after the cover's page break (the template outline page).
    $breakIndex = 0
    for ($i = 1; $i -le $doc.Paragraphs.Count; $i++) {
        $text = $doc.Paragraphs.Item($i).Range.Text
        if ($text.IndexOf([char]12) -ge 0) {
            $breakIndex = $i
            break
        }
    }
    if ($breakIndex -gt 0) {
        # The template keeps the page break and the outline text in one paragraph:
        # keep the break char (12) and drop the rest of that paragraph.
        $breakParagraph = $doc.Paragraphs.Item($breakIndex).Range
        $textStart = $breakParagraph.Start + 1
        $textEnd = $breakParagraph.End - 1
        if ($textEnd -gt $textStart) {
            $doc.Range($textStart, $textEnd).Delete() | Out-Null
        }
        if ($breakIndex -lt $doc.Paragraphs.Count) {
            $range = $doc.Range($doc.Paragraphs.Item($breakIndex + 1).Range.Start, $doc.Content.End)
            $range.Delete() | Out-Null
        }
    }

    $selection = $word.Selection
    $selection.EndKey(6) | Out-Null
    $selection.TypeParagraph()

    foreach ($block in $blocks) {
        $selection = $word.Selection
        $selection.ParagraphFormat.Alignment = 3
        $selection.ParagraphFormat.FirstLineIndent = 0
        $selection.Font.Name = 'Times New Roman'
        $selection.Font.NameFarEast = 'SimSun'
        $selection.Font.Size = 12
        $selection.Font.Bold = 0
        $selection.ParagraphFormat.LineSpacingRule = 0

        if ($block.s -eq 'h1') {
            $selection.ParagraphFormat.Alignment = 0
            $selection.Font.NameFarEast = 'SimHei'
            $selection.Font.Size = 14
            $selection.Font.Bold = -1
            $selection.ParagraphFormat.SpaceBefore = 10
            $selection.ParagraphFormat.SpaceAfter = 4
            $selection.TypeText($block.t)
        }
        elseif ($block.s -eq 'h2') {
            $selection.ParagraphFormat.Alignment = 0
            $selection.Font.NameFarEast = 'SimSun'
            $selection.Font.Size = 12
            $selection.Font.Bold = -1
            $selection.ParagraphFormat.SpaceBefore = 4
            $selection.ParagraphFormat.SpaceAfter = 2
            $selection.TypeText($block.t)
        }
        elseif ($block.s -eq 'img') {
            $parts = $block.t.Split('|')
            $imagePath = $parts[0]
            $caption = if ($parts.Length -gt 1) { $parts[1] } else { '' }
            $selection.ParagraphFormat.Alignment = 1
            $selection.ParagraphFormat.SpaceBefore = 4
            $selection.ParagraphFormat.SpaceAfter = 2
            $shape = $selection.InlineShapes.AddPicture($imagePath, $false, $true)
            $shape.LockAspectRatio = -1
            $shape.Width = 11.5 * 28.3465
            if ($caption -ne '') {
                $selection.TypeParagraph()
                $selection.ParagraphFormat.Alignment = 1
                $selection.Font.NameFarEast = 'SimSun'
                $selection.Font.Size = 10.5
                $selection.Font.Bold = 0
                $selection.ParagraphFormat.FirstLineIndent = 0
                $selection.TypeText($caption)
            }
        }
        else {
            $selection.ParagraphFormat.FirstLineIndent = 24
            $selection.ParagraphFormat.SpaceBefore = 0
            $selection.ParagraphFormat.SpaceAfter = 0
            $selection.TypeText($block.t)
        }

        $selection.TypeParagraph()
    }

    $doc.Save()
    $doc.Close(0)
    Write-Output "Generated: $output"
}
finally {
    try { $word.Quit() } catch { }
    try { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null } catch { }
}
