# Build the course practice summary report from the supplied school template.
# Chinese content is stored in course-report-content.txt (UTF-8).

$ErrorActionPreference = 'Stop'

$contentFile = Join-Path $PSScriptRoot 'course-report-content.txt'
$lines = Get-Content -LiteralPath $contentFile -Encoding UTF8

$template = ''
$outputName = ''
$cover = @{}
$blocks = @()

foreach ($line in $lines) {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    $index = $line.IndexOf('|')
    if ($index -lt 1) { continue }

    $key = $line.Substring(0, $index)
    $text = $line.Substring($index + 1)

    if ($key -eq 'template') { $template = $text }
    elseif ($key -eq 'output') { $outputName = $text }
    elseif ($key -eq 'project') { }
    elseif ($key -match '^cover([0-9]+)$') { $cover[$Matches[1]] = $text }
    elseif ($key -ne 'end') { $blocks += @{ s = $key; t = $text } }
}

if (-not (Test-Path -LiteralPath $template)) {
    throw "Template not found: $template"
}
if ([string]::IsNullOrWhiteSpace($outputName)) {
    throw 'Output file name is missing.'
}

$output = Join-Path $PSScriptRoot $outputName
Copy-Item -LiteralPath $template -Destination $output -Force

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0

try {
    $doc = $word.Documents.Open($output)

    # Replace cover text without deleting the paragraph marks.
    foreach ($coverIndex in @('7', '8', '9', '10')) {
        if (-not $cover.ContainsKey($coverIndex)) { continue }
        $paragraph = $doc.Paragraphs.Item([int]$coverIndex)
        $textRange = $doc.Range($paragraph.Range.Start, $paragraph.Range.End - 1)
        $textRange.Text = $cover[$coverIndex]
        $textRange.Font.Name = 'Times New Roman'
        $textRange.Font.NameFarEast = 'SimSun'
        $textRange.Font.Size = 16
        $textRange.Font.Bold = 0
    }

    # Keep the cover page break and remove the outline page.
    $breakIndex = 0
    for ($i = 1; $i -le $doc.Paragraphs.Count; $i++) {
        if ($doc.Paragraphs.Item($i).Range.Text.IndexOf([char]12) -ge 0) {
            $breakIndex = $i
            break
        }
    }

    if ($breakIndex -gt 0) {
        $breakParagraph = $doc.Paragraphs.Item($breakIndex).Range
        $breakPosition = $breakParagraph.Text.IndexOf([char]12)
        if ($breakPosition -ge 0 -and $breakPosition -lt ($breakParagraph.Text.Length - 1)) {
            $doc.Range(
                $breakParagraph.Start + $breakPosition + 1,
                $breakParagraph.End - 1
            ).Delete() | Out-Null
        }
        if ($breakIndex -lt $doc.Paragraphs.Count) {
            $removeStart = $doc.Paragraphs.Item($breakIndex + 1).Range.Start
            $doc.Range($removeStart, $doc.Content.End).Delete() | Out-Null
        }
    }

    $selection = $word.Selection
    $selection.EndKey(6) | Out-Null

    foreach ($block in $blocks) {
        $selection = $word.Selection
        $selection.ParagraphFormat.FirstLineIndent = 0
        $selection.ParagraphFormat.SpaceBefore = 0
        $selection.ParagraphFormat.SpaceAfter = 0
        $selection.ParagraphFormat.LineSpacingRule = 0
        $selection.Font.Name = 'Times New Roman'
        $selection.Font.NameFarEast = 'SimSun'
        $selection.Font.Size = 12
        $selection.Font.Bold = 0

        if ($block.s -eq 'title') {
            $selection.ParagraphFormat.Alignment = 1
            $selection.ParagraphFormat.SpaceBefore = 0
            $selection.ParagraphFormat.SpaceAfter = 10
            $selection.Font.NameFarEast = 'SimHei'
            $selection.Font.Size = 16
            $selection.Font.Bold = -1
            $selection.TypeText($block.t)
        }
        elseif ($block.s -eq 'h1') {
            $selection.ParagraphFormat.Alignment = 0
            $selection.ParagraphFormat.SpaceBefore = 8
            $selection.ParagraphFormat.SpaceAfter = 3
            $selection.ParagraphFormat.KeepWithNext = -1
            $selection.Font.NameFarEast = 'SimHei'
            $selection.Font.Size = 14
            $selection.Font.Bold = -1
            $selection.TypeText($block.t)
        }
        elseif ($block.s -eq 'h2') {
            $selection.ParagraphFormat.Alignment = 0
            $selection.ParagraphFormat.SpaceBefore = 4
            $selection.ParagraphFormat.SpaceAfter = 2
            $selection.ParagraphFormat.KeepWithNext = -1
            $selection.Font.NameFarEast = 'SimHei'
            $selection.Font.Size = 12
            $selection.Font.Bold = -1
            $selection.TypeText($block.t)
        }
        elseif ($block.s -eq 'img') {
            $parts = $block.t.Split('|')
            $imagePath = Join-Path $PSScriptRoot $parts[0]
            $caption = if ($parts.Length -gt 1) { $parts[1] } else { '' }
            if (-not (Test-Path -LiteralPath $imagePath)) {
                throw "Image not found: $imagePath"
            }

            $selection.ParagraphFormat.Alignment = 1
            $selection.ParagraphFormat.SpaceBefore = 3
            $selection.ParagraphFormat.SpaceAfter = 1
            $shape = $selection.InlineShapes.AddPicture($imagePath, $false, $true)
            $shape.LockAspectRatio = -1
            $shape.Width = 9.8 * 28.3465

            if ($caption -ne '') {
                $selection.TypeParagraph()
                $selection.ParagraphFormat.Alignment = 1
                $selection.ParagraphFormat.FirstLineIndent = 0
                $selection.ParagraphFormat.SpaceBefore = 0
                $selection.ParagraphFormat.SpaceAfter = 3
                $selection.Font.Name = 'Times New Roman'
                $selection.Font.NameFarEast = 'SimSun'
                $selection.Font.Size = 10.5
                $selection.Font.Bold = 0
                $selection.TypeText($caption)
            }
        }
        else {
            $selection.ParagraphFormat.Alignment = 3
            $selection.ParagraphFormat.FirstLineIndent = 24
            $selection.ParagraphFormat.SpaceBefore = 0
            $selection.ParagraphFormat.SpaceAfter = 0
            $selection.ParagraphFormat.LineSpacingRule = 0
            $selection.Font.Name = 'Times New Roman'
            $selection.Font.NameFarEast = 'SimSun'
            $selection.Font.Size = 12
            $selection.Font.Bold = 0
            $selection.TypeText($block.t)
        }

        $selection.TypeParagraph()
    }

    $doc.Save()
    $pageCount = $doc.ComputeStatistics(2)
    $wordCount = $doc.ComputeStatistics(0)
    $doc.Close(0)
    Write-Output "Generated: $output"
    Write-Output "Pages: $pageCount"
    Write-Output "Words: $wordCount"
}
finally {
    try { $word.Quit() } catch { }
    try { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null } catch { }
}
