$scriptPath = Join-Path $PSScriptRoot '..\Verify-GitHubRelease.ps1'
. $scriptPath
Describe 'GitHub Release artifact integrity contracts' {
 It 'accepts only a strict sha256 digest' {
  (Normalize-GitHubDigest ('sha256:' + ('0123456789abcdef' * 4))) | Should Be ('0123456789abcdef' * 4)
  { Normalize-GitHubDigest ('sha1:' + ('0123456789abcdef' * 4)) } | Should Throw
  { Normalize-GitHubDigest 'sha256:short' } | Should Throw
 }
 It 'parses only the current PikClick sidecar format' {
  $text=('ABCDEF' * 10 + 'ABCD') + '  PikClick-v2.1.1-release.apk' + [char]13 + [char]10
  (Parse-PikClickChecksum $text 'PikClick-v2.1.1-release.apk') | Should Be ('abcdef' * 10 + 'abcd')
  { Parse-PikClickChecksum (('abcdef' * 10 + 'abcd') + ' PikClick-v2.1.1-release.apk') 'PikClick-v2.1.1-release.apk' } | Should Throw
 }
 It 'requires one uploaded exact-name asset' {
  $assets=@([pscustomobject]@{name='app.apk';state='uploaded';digest=('sha256:' + ('a'*64))})
  (Select-UniqueUploadedAsset $assets 'app.apk').name | Should Be 'app.apk'
  { Select-UniqueUploadedAsset @() 'app.apk' } | Should Throw
  { Select-UniqueUploadedAsset @($assets+$assets) 'app.apk' } | Should Throw
 }
 It 'passes only when all applicable comparisons agree' {
  $h=('a' * 64); $result=Compare-ArtifactIntegrity -LocalSha $h -DownloadedSha $h -GithubDigest ('sha256:' + $h) -SidecarSha $h
  $result.Status | Should Be 'PASS'; $result.ExitCode | Should Be 0
  (Compare-ArtifactIntegrity -LocalSha ('a'*64) -DownloadedSha ('b'*64) -GithubDigest ('sha256:' + ('b'*64))).ExitCode | Should Be 10
  (Compare-ArtifactIntegrity -LocalSha 'a' -DownloadedSha 'a' -GithubDigest $null).ExitCode | Should Be 21
 }
 It 'distinguishes remote internal integrity from local artifact identity' {
  $remote=('a' * 64); $local=('b' * 64)
  $result=Compare-ArtifactIntegrity -LocalSha $local -DownloadedSha $remote -GithubDigest ('sha256:' + $remote) -SidecarSha $remote
  $result.RemoteIntegrity | Should Be 'PASS'
  $result.LocalIdentity | Should Be 'FAIL'
  $result.Status | Should Be 'FAIL'
  $result.ExitCode | Should Be 10
 }}