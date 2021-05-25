#Set-PSBreakpoint .\prepare.ps1 -Line 56
#$DebugPreference = "Continue"
$DebugPreference = "SilentlyContinue"
# case small NER-de-train_small
[bool]$isSmall = $false
# which index type (type1 = 2) or (type2 = 3)
[int]$indexType = 2 
[int]$currentLength = 0
[int]$blockSize = 1
[int]$currentIndex = 0


# meaning B-Pers, I-Pers, togehter 
[bool]$ismMultiWord = $false
[string]$fileNameAddtion = ""
[string]$predecessorWord = ""
[string]$predecessorType = ""
[string]$currentType = ""
[string]$tempWord = ""
[string]$category = "LOC"
if ($isSmall){ $fileNameAddtion = "_small" }

$trainDataFile = "D:\opennlp\Data\ner\NER-de-test" + $fileNameAddtion +".tsv"


[string]$sentenceBeging = "#"
# empty line 
[string]$sentenceStop = ""

#result
[System.Text.StringBuilder]$sentence = ""
[System.Text.StringBuilder]$result = "" 


function BuildTag
{
    param ([string]$word, [string]$type)
    [string]$result = ""
    if ($type -eq "O")
    {
        return ""
    }
    # remove B- oder I- from type
    $result = "<START:"+ $type.Substring(2).ToLower() +"> " + $word.Trim() + " <END>"
    Write-Debug ("taged --> : " + $result )
    return $result
}
function IsMultiWord
{
    param ([string]$predecessorType,[string]$currentType)

    if ($predecessorType[0] -eq 'B' -and $currentType[0] -eq 'I')
    {
        return $true
    }
    if ($predecessorType[0] -eq 'I' -and $currentType[0] -eq 'I')
    {
        return $true
    }

    return $false

}
function RemovePatterns
{
    param ([string]$sentence)
    [string]$temp = $sentence
    [int]$index = 0
    Write-Debug ("replace patterns from --> " + $temp )
    $temp = $temp.Replace(" . """, ".")
    $temp = $temp.Replace(" ! """, "!")

    if ($temp.Contains(""""))
    {
        $parts = $temp.Split("""")
        $temp = ""
        foreach ($part in $parts)
        {
            
            $part = $part.Trim()
            if ($index % 2 -eq 0)
            {
                $temp += $part + " """
            }
            else
            {
                $temp += $part + """ "            
            }
            $index++
         }
         
         
    }
    #$temp = $temp.Replace(" , ", " , ")
    $temp = $temp.Replace(" . """, ".")
    $temp = $temp.Replace(" ! """, "!")
    $temp = $temp.Replace(" . ", ".")
    $temp = $temp.Replace(" ! ", "!")
    $temp = $temp.Replace(" : ", ": ")
    $temp = $temp.Replace(" ( ", " (")
    $temp = $temp.Replace(" ) ", ") ")
    $temp = $temp.Replace("- ", "-")
    Write-Debug ("reuslt of replace pattern --> " + $temp )

    #$temp.Replace(" "" ", """") # 

    return $temp
}

function TransformAndBuildTag
{
    param([string]$type, [string]$word)
    [string]$result = ""

    switch ($type)
    {
        B-PER     { $result = "person"; break}
        I-PER     { $result = "person"; break}
        I-PERpart { $result = "person"; break}
        B-PERpart { $result = "person"; break}
        I-PERderiv{ $result = "person"; break}
        B-PERderiv{ $result = "person"; break}
           
        B-ORG     { $result = "organisation"; break}
        I-ORG     { $result = "organisation"; break}
        B-ORGpart { $result = "organisation"; break}
        I-ORGpart { $result = "organisation"; break}
        B-ORGderiv{ $result = "organisation"; break}

        B-LOC     { $result = "location"; break}
        B-LOCderiv{ $result = "location"; break}
        I-LOCderiv{ $result = "location"; break}
        B-LOCpart { $result = "location"; break}
        I-LOC     { $result = "location"; break}
        I-LOCpart { $result = "location"; break}

        B-OTHderiv{ $result = "other"; break}
        B-OTH     { $result = "other"; break}
        I-OTH     { $result = "other"; break}
        B-OTHpart { $result = "other"; break}
        I-OTHpart { $result = "other"; break}
        I-OTHderiv{ $result = "other"; break}

        O         { return ""}
        Default   { return ""}
    }
    $result = "<START:"+ $result +"> " + $word.Trim() + " <END>"
    Write-Debug ("taged to: " + $result)
    return $result

}

function TransformAndBuildTagByCategory
{
    param([string]$type, [string]$word, [string]$category = "ALL" )
    [string]$result = ""
    
    if ($type.Length -le 6)
    {
        [string]$tempType = $type.Substring(2,3).ToUpper()
    }
    else
    {
        [string]$tempType = ""
    }
    


    if ($category -eq "PER" -and $tempType -eq "PER")
    {
        $result = "person"
    }
    if ($category -eq "ORG" -and $tempType -eq "ORG")
    {
        $result = "organisation"
    }
    if ($category -eq "OTH" -and $tempType -eq "OTH")
    {
        $result = "other"
    }
    if ($category -eq "LOC" -and $tempType -eq "LOC")
    {
       $result = "location"
    }
    if ($category -eq "ALL" )
    {
        switch ($tempType)
        {
            PER { $result = "person";       break}
            OTH { $result = "other";        break}
            ORG { $result = "organisation"; break}
            LOC { $result = "location";     break}
        }
    }
    if (-not[string]::IsNullOrEmpty($result))
    {
        $result = "<START:"+ $result +"> " + $word.Trim() + " <END>"
        Write-Debug ("taged to: " + $result)
        return $result
    }
    else
    {
        return $word.Trim()
    }
}



$fileContents = Get-Content -Path $trainDataFile -Encoding UTF8
foreach  ($fileContent in $fileContents)
{
    if ($fileContent.StartsWith("#")) 
    { 
        continue 
    }
    if ([String]::IsNullOrEmpty($fileContent))
    {
        #$result.Append((RemovePatterns -sentence  $sentence.ToString()))
        $result = $result.Append($sentence.ToString() + " ")
        $result = $result.AppendLine()
        #$result.AppendLine()
        if ($currentIndex % $blockSize -eq 0)
        {
            $result = $result.AppendLine()
        }
        $sentence.Clear()
        continue
    }
    # split by tab
    $parts = $fileContent.Split("`t")
    Write-Debug ("Line:" + $parts[0] + "   Word: " + $parts[1] + "   Type1: " + $parts[2] + "   Type2: " + $parts[3])
    $currentType = $parts[$indexType]

    if ($currentType -eq "O")
    {
        if (-not [string]::IsNullOrEmpty($predecessorWord))
        {
            #$tempWord =  TransformAndBuildTag -word $predecessorWord -type $predecessorType
            $tempWord = TransformAndBuildTagByCategory -type $predecessorType -word $predecessorWord -category $category
            $sentence = $sentence.Append($tempWord)
            $sentence = $sentence.Append( " " )
            Write-Debug ("append --> : " + $tempWord )
            $predecessorWord = [String]::Empty
            $tempWord = [String]::Empty
        }
        Write-Debug ("append --> : " + $parts[1])
        $sentence = $sentence.Append( $parts[1] )
        $sentence = $sentence.Append( " " )
        

 # -or $parts[1] -eq "." -or $parts[1] -eq "," -or $parts[1] -eq "!" -or $parts[1] -eq ":")
    }
    else
    {
        if (IsMultiWord -predecessorType $predecessorType -currentType $currentType)
        {
            
            $predecessorWord += $parts[1] + " "
        }
        else 
        { 
            $predecessorWord = $parts[1] + " "
        }
        Write-Debug ("multiword --> : " + $predecessorWord )
        $predecessorType = $currentType

    }
    $predecessorType = $currentType
    $currentIndex++
 
}

Write-Debug ("write to file")
$Utf8NoBomEncoding = New-Object System.Text.UTF8Encoding $False
[System.IO.File]::WriteAllLines(("D:\opennlp\Data\ner\NER-de-test_"+ $category+ ".txt"), $result.ToString(), $Utf8NoBomEncoding)
 

