[CmdletBinding()]
param(
    [string]$Repository,
    [string]$Tag,
    [string]$AssetName,
    [string]$LocalApkPath,
    [string]$Token = $env:GITHUB_TOKEN,
    [switch]$Run
)
function Normalize-GitHubDigest([string]$Digest) {
    if ($Digest -notmatch '^sha256:[0-9a-fA-F]{64}$') { throw 'Invalid GitHub digest.' }
    return $Digest.Substring(7).ToLowerInvariant()
}
function Parse-PikClickChecksum([string]$Text,[string]$ApkName) {
    $line=$Text
    if ($line.EndsWith([char]13+[char]10)) { $line=$line.Substring(0,$line.Length-2) } elseif ($line.EndsWith([char]10)) { $line=$line.Substring(0,$line.Length-1) }
    if ($line.Contains([char]10) -or $line.Contains([char]13) -or $line -notmatch '^([0-9a-fA-F]{64})  (.+)$' -or $Matches[2] -cne $ApkName) { throw 'Invalid PikClick checksum format.' }
    return $Matches[1].ToLowerInvariant()
}
function Select-UniqueUploadedAsset($Assets,[string]$Name) {
    $matches=@($Assets | Where-Object { $_.name -ceq $Name })
    if ($matches.Count -ne 1 -or $matches[0].state -cne 'uploaded') { throw "Asset '$Name' is not exactly one uploaded asset." }
    return $matches[0]
}
function Compare-ArtifactIntegrity {
    param([string]$LocalSha,[string]$DownloadedSha,[string]$GithubDigest,[AllowNull()][string]$SidecarSha)
    if ([string]::IsNullOrWhiteSpace($GithubDigest)) { return [pscustomobject]@{RemoteIntegrity='NOT VERIFIED';LocalIdentity='NOT VERIFIED';Status='NOT VERIFIED';ExitCode=21} }
    try { $github=Normalize-GitHubDigest $GithubDigest } catch { return [pscustomobject]@{RemoteIntegrity='NOT VERIFIED';LocalIdentity='NOT VERIFIED';Status='NOT VERIFIED';ExitCode=21} }
    $remotePass=($DownloadedSha -eq $github -and ($null -eq $SidecarSha -or $SidecarSha -eq $DownloadedSha))
    $remoteStatus=if($remotePass){'PASS'}else{'FAIL'}
    $identityStatus=if($remotePass -and $LocalSha -eq $DownloadedSha){'PASS'}elseif($remotePass){'FAIL'}else{'NOT VERIFIED'}
    $overall=if($remotePass -and $identityStatus -eq 'PASS'){'PASS'}elseif($remotePass){'FAIL'}else{'FAIL'}
    $code=if($overall -eq 'PASS'){0}else{10}
    return [pscustomobject]@{RemoteIntegrity=$remoteStatus;LocalIdentity=$identityStatus;Status=$overall;ExitCode=$code}
}function Invoke-GitHubReleaseIntegrity {
    if ([string]::IsNullOrWhiteSpace($Repository) -or [string]::IsNullOrWhiteSpace($Tag) -or [string]::IsNullOrWhiteSpace($AssetName) -or [string]::IsNullOrWhiteSpace($LocalApkPath)) { Write-Output 'NOT VERIFIED: Repository, Tag, AssetName and LocalApkPath are required.'; return 21 }
    if (-not (Test-Path -LiteralPath $LocalApkPath -PathType Leaf)) { Write-Output 'NOT VERIFIED: local APK does not exist.'; return 21 }
    $headers=@{Accept='application/vnd.github+json';'X-GitHub-Api-Version'='2022-11-28'}; if ($Token) { $headers.Authorization="Bearer $Token" }
    $uri="https://api.github.com/repos/$Repository/releases/tags/$Tag"
    try { $release=Invoke-RestMethod -Uri $uri -Headers $headers -Method Get -ErrorAction Stop } catch { Write-Output 'BLOCKED: GitHub Release API unavailable.'; return 20 }
    try { $apkAsset=Select-UniqueUploadedAsset $release.assets $AssetName } catch { Write-Output ('BLOCKED: ' + $_.Exception.Message); return 20 }
    try { $local=(Get-FileHash -LiteralPath $LocalApkPath -Algorithm SHA256).Hash.ToLowerInvariant(); $github=Normalize-GitHubDigest $apkAsset.digest } catch { Write-Output ('NOT VERIFIED: ' + $_.Exception.Message); return 21 }
    $temp=$null; do { $candidate=[IO.Path]::Combine([IO.Path]::GetTempPath(),[IO.Path]::GetRandomFileName()) } while (Test-Path -LiteralPath $candidate); $temp=New-Item -ItemType Directory -Path $candidate -ErrorAction Stop
    try {
        $download=Join-Path $temp.FullName $AssetName
        Invoke-WebRequest -Uri $apkAsset.browser_download_url -Headers $headers -OutFile $download -UseBasicParsing -ErrorAction Stop
        $downloaded=(Get-FileHash -LiteralPath $download -Algorithm SHA256).Hash.ToLowerInvariant()
        $sidecarAsset=@($release.assets | Where-Object { $_.name -ceq ($AssetName + '.sha256') })
        $sidecar=$null
        if ($sidecarAsset.Count -gt 1) { Write-Output 'BLOCKED: duplicate sidecar assets.'; return 20 }
        if ($sidecarAsset.Count -eq 1) {
            if ($sidecarAsset[0].state -cne 'uploaded') { Write-Output 'BLOCKED: sidecar is not uploaded.'; return 20 }
            $sidecarPath=Join-Path $temp.FullName ($AssetName + '.sha256')
            Invoke-WebRequest -Uri $sidecarAsset[0].browser_download_url -Headers $headers -OutFile $sidecarPath -UseBasicParsing -ErrorAction Stop
            $sidecar=Parse-PikClickChecksum ([IO.File]::ReadAllText($sidecarPath)) $AssetName
        }
        $result=Compare-ArtifactIntegrity $local $downloaded ('sha256:' + $github) $sidecar
        Write-Output "LOCAL_SHA256=$local"; Write-Output "DOWNLOADED_SHA256=$downloaded"; Write-Output "GITHUB_ASSET_DIGEST=sha256:$github"; if ($sidecar) { Write-Output "SHA256_FILE_VALUE=$sidecar" } else { Write-Output 'SHA256_FILE_VALUE=NOT_APPLICABLE' }; Write-Output "GitHub Release internal integrity: $($result.RemoteIntegrity)"; Write-Output "Local-to-Release artifact identity: $($result.LocalIdentity)"; Write-Output 'Publisher provenance: API/Release metadata only; not established by hash comparison'; Write-Output "Overall verifier status: $($result.Status)"
        return $result.ExitCode
    } catch { Write-Output ('BLOCKED: ' + $_.Exception.Message); return 20 } finally { Remove-Item -LiteralPath $temp.FullName -Recurse -Force -ErrorAction SilentlyContinue }
}

if ($Run) { $out=@(Invoke-GitHubReleaseIntegrity); if ($out.Count -gt 1) { $out[0..($out.Count-2)] | Write-Output }; exit ([int]$out[$out.Count-1]) }