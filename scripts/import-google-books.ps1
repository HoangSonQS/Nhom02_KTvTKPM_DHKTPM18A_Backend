param(
    [int]$MaxPerCategory = 12,
    [string]$DbContainer = "postgres_db",
    [string]$DbName = "sebook",
    [string]$DbUser = "",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

$envFile = Join-Path $PSScriptRoot "..\.env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match "^\s*([^#][^=]+)=(.*)$") {
            [Environment]::SetEnvironmentVariable($Matches[1].Trim(), $Matches[2].Trim(), "Process")
        }
    }
}

if ([string]::IsNullOrWhiteSpace($DbUser)) {
    $DbUser = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "postgres" }
}

if ($env:DB_NAME -and $DbName -eq "sebook") {
    $DbName = $env:DB_NAME
}

$categoryQueries = @(
    @{ Name = "V$([char]0x0103)n h$([char]0x1ECD)c"; Query = "subject:fiction" },
    @{ Name = "Kinh t$([char]0x1EBF)"; Query = "subject:business" },
    @{ Name = "K$([char]0x1EF9) n$([char]0x0103)ng s$([char]0x1ED1)ng"; Query = "subject:self-help" },
    @{ Name = "Thi$([char]0x1EBF)u nhi"; Query = "subject:juvenile-fiction" },
    @{ Name = "C$([char]0x00F4)ng ngh$([char]0x1EC7) th$([char]0x00F4)ng tin"; Query = "subject:computers" }
)

$paperbackCoverType = "B$([char]0x00EC)a m$([char]0x1EC1)m"

function Escape-Sql([string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        return "NULL"
    }

    $builder = New-Object System.Text.StringBuilder
    foreach ($char in $Value.ToCharArray()) {
        $code = [int][char]$char
        if ($char -eq "'") {
            [void]$builder.Append("''")
        }
        elseif ($char -eq "\") {
            [void]$builder.Append("\005C")
        }
        elseif ($code -ge 32 -and $code -le 126) {
            [void]$builder.Append($char)
        }
        else {
            [void]$builder.Append("\" + $code.ToString("X4"))
        }
    }

    return "U&'" + $builder.ToString() + "'"
}

function To-NullableInt($Value) {
    if ($null -eq $Value) {
        return "NULL"
    }
    $number = 0
    if ([int]::TryParse([string]$Value, [ref]$number)) {
        return [string]$number
    }
    return "NULL"
}

function To-NullableDecimal($Value) {
    if ($null -eq $Value) {
        return "NULL"
    }
    $number = 0.0
    if ([double]::TryParse([string]$Value, [ref]$number)) {
        return ([Math]::Round($number, 2)).ToString([Globalization.CultureInfo]::InvariantCulture)
    }
    return "NULL"
}

function Normalize-ImageUrl([string]$Url) {
    if ([string]::IsNullOrWhiteSpace($Url)) {
        return $null
    }
    return ($Url -replace "^http://", "https://")
}

function Get-Isbn($Identifiers) {
    if ($null -eq $Identifiers) {
        return $null
    }

    $isbn13 = $Identifiers | Where-Object { $_.type -eq "ISBN_13" } | Select-Object -First 1
    if ($isbn13) {
        return $isbn13.identifier
    }

    $isbn10 = $Identifiers | Where-Object { $_.type -eq "ISBN_10" } | Select-Object -First 1
    if ($isbn10) {
        return $isbn10.identifier
    }

    return $null
}

function Get-PublicationYear([string]$PublishedDate) {
    if ([string]::IsNullOrWhiteSpace($PublishedDate)) {
        return $null
    }
    if ($PublishedDate -match "^\d{4}") {
        return [int]$Matches[0]
    }
    return $null
}

function Get-Price($Item, [int]$PageCount) {
    $retail = $Item.saleInfo.retailPrice
    if ($retail -and $retail.currencyCode -eq "VND") {
        return [decimal]$retail.amount
    }

    $base = if ($PageCount -gt 0) { [Math]::Min([Math]::Max($PageCount * 450, 45000), 260000) } else { Get-Random -Minimum 65000 -Maximum 180000 }
    return [decimal]([Math]::Round($base / 1000) * 1000)
}

function Get-OriginalPrice($Item, [decimal]$Price) {
    $list = $Item.saleInfo.listPrice
    if ($list -and $list.currencyCode -eq "VND") {
        return [decimal]$list.amount
    }
    return [decimal]([Math]::Round(($Price * 1.2) / 1000) * 1000)
}

function Truncate-Text([string]$Value, [int]$MaxLength) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $null
    }
    if ($Value.Length -le $MaxLength) {
        return $Value
    }
    return $Value.Substring(0, $MaxLength)
}

function Invoke-GoogleBooksRequest([string]$Url) {
    $headers = @{
        "User-Agent" = "SEBook-Dev-Importer/1.0"
        "Accept"     = "application/json"
    }

    for ($attempt = 1; $attempt -le 3; $attempt++) {
        try {
            return Invoke-RestMethod -Uri $Url -Method Get -Headers $headers
        }
        catch {
            if ($attempt -eq 3) {
                Write-Warning "Skip request after 3 attempts: $Url. Error: $($_.Exception.Message)"
                return $null
            }
            Start-Sleep -Seconds (2 * $attempt)
        }
    }
}

$sqlStatements = New-Object System.Collections.Generic.List[string]
$sqlStatements.Add("BEGIN;")

foreach ($category in $categoryQueries) {
    $encodedQuery = [uri]::EscapeDataString($category.Query)
    $url = "https://www.googleapis.com/books/v1/volumes?q=$encodedQuery&maxResults=$MaxPerCategory&printType=books"
    Write-Host "Fetching $($category.Name): $url"

    $response = Invoke-GoogleBooksRequest $url
    if ($null -eq $response.items) {
        continue
    }

    $categoryNameSql = Escape-Sql $category.Name
    $sqlStatements.Add("INSERT INTO cat_category (name, is_active, created_at) VALUES ($categoryNameSql, TRUE, NOW()) ON CONFLICT (name) DO UPDATE SET is_active = TRUE, updated_at = NOW();")

    foreach ($item in $response.items) {
        $info = $item.volumeInfo
        $isbn = Get-Isbn $info.industryIdentifiers
        if ([string]::IsNullOrWhiteSpace($isbn)) {
            continue
        }

        $title = Truncate-Text $info.title 255
        $author = Truncate-Text (($info.authors | Select-Object -First 1) -as [string]) 100
        if ([string]::IsNullOrWhiteSpace($title) -or [string]::IsNullOrWhiteSpace($author)) {
            continue
        }

        $pageCount = if ($info.pageCount) { [int]$info.pageCount } else { 0 }
        $price = Get-Price $item $pageCount
        $originalPrice = Get-OriginalPrice $item $price
        $quantity = Get-Random -Minimum 12 -Maximum 80
        $publicationYear = Get-PublicationYear $info.publishedDate
        $rating = if ($info.averageRating) { [Math]::Min([decimal]$info.averageRating, [decimal]5) } else { [decimal]0 }
        $ratingCount = if ($info.ratingsCount) { [int]$info.ratingsCount } else { 0 }
        $imageUrl = Normalize-ImageUrl $info.imageLinks.thumbnail
        $keywords = @($category.Name)
        if ($info.categories) {
            $keywords += @($info.categories)
        }
        $keywordsJson = ($keywords | Select-Object -Unique | ConvertTo-Json -Compress)

        $sql = @"
WITH selected_category AS (
    SELECT id FROM cat_category WHERE name = $(Escape-Sql $category.Name)
),
upsert_book AS (
    INSERT INTO cat_book (
        title, author, description, price, deprecated_quantity, image_url, image_public_id, is_active,
        publisher, isbn, publication_year, language, keywords, page_count, cover_type,
        original_price, average_rating, rating_count, created_at, updated_at
    )
    VALUES (
        $(Escape-Sql $title),
        $(Escape-Sql $author),
        $(Escape-Sql $info.description),
        $(To-NullableDecimal $price),
        $quantity,
        $(Escape-Sql $imageUrl),
        NULL,
        TRUE,
        $(Escape-Sql (Truncate-Text $info.publisher 255)),
        $(Escape-Sql (Truncate-Text $isbn 20)),
        $(To-NullableInt $publicationYear),
        $(Escape-Sql (Truncate-Text $info.language 50)),
        $(Escape-Sql $keywordsJson)::jsonb,
        $(To-NullableInt $pageCount),
        $(Escape-Sql $paperbackCoverType),
        $(To-NullableDecimal $originalPrice),
        $(To-NullableDecimal $rating),
        $ratingCount,
        NOW(),
        NOW()
    )
    ON CONFLICT ON CONSTRAINT uk_books_isbn DO UPDATE SET
        title = EXCLUDED.title,
        author = EXCLUDED.author,
        description = COALESCE(EXCLUDED.description, cat_book.description),
        price = EXCLUDED.price,
        deprecated_quantity = EXCLUDED.deprecated_quantity,
        image_url = COALESCE(EXCLUDED.image_url, cat_book.image_url),
        publisher = COALESCE(EXCLUDED.publisher, cat_book.publisher),
        publication_year = COALESCE(EXCLUDED.publication_year, cat_book.publication_year),
        language = COALESCE(EXCLUDED.language, cat_book.language),
        keywords = EXCLUDED.keywords,
        page_count = COALESCE(EXCLUDED.page_count, cat_book.page_count),
        cover_type = COALESCE(EXCLUDED.cover_type, cat_book.cover_type),
        original_price = EXCLUDED.original_price,
        average_rating = EXCLUDED.average_rating,
        rating_count = EXCLUDED.rating_count,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id
),
book_ref AS (
    SELECT id FROM upsert_book
    UNION
    SELECT id FROM cat_book WHERE isbn = $(Escape-Sql (Truncate-Text $isbn 20))
)
INSERT INTO cat_book_category (book_id, category_id)
SELECT book_ref.id, selected_category.id FROM book_ref CROSS JOIN selected_category
ON CONFLICT DO NOTHING;

INSERT INTO inv_stock (book_id, quantity, version, created_at, updated_at)
SELECT id, $quantity, 0, NOW(), NOW()
FROM cat_book
WHERE isbn = $(Escape-Sql (Truncate-Text $isbn 20))
ON CONFLICT (book_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
"@
        $sqlStatements.Add($sql)
    }
}

$sqlStatements.Add("COMMIT;")
$sqlContent = $sqlStatements -join "`n"
$outputPath = Join-Path $PSScriptRoot "google-books-import.generated.sql"
Set-Content -Path $outputPath -Value $sqlContent -Encoding UTF8

Write-Host "Generated SQL: $outputPath"

if ($DryRun) {
    Write-Host "DryRun enabled. SQL was generated but not executed."
    exit 0
}

Get-Content -Path $outputPath -Raw -Encoding UTF8 | docker exec -i $DbContainer psql -U $DbUser -d $DbName -v ON_ERROR_STOP=1
if ($LASTEXITCODE -ne 0) {
    throw "psql import failed with exit code $LASTEXITCODE"
}
Write-Host "Import completed."
