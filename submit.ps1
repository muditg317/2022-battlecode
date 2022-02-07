if ($args.Count -lt 1) {
    Write-Output "You must supply the package name to zip!"
    exit
}
$pkgname = $args[0]
$sourceFolder = "src/$pkgname"
if (-Not (Test-Path -Path $sourceFolder)) {
    Write-Output "$sourceFolder does not exist!"
    exit
}
$destinationParent = "submissions/"
if (-Not (Test-Path -Path $destinationParent)) {
    mkdir -Path $destinationParent
    Write-Output "Create $destinationParent folder"
}
$destinationNum = 0
$dstName = $pkgname
if ($args.Count -gt 1) {
    $dstName = $args[1]
}
while (Test-Path -Path "$destinationParent$dstName$destinationNum.zip") {
    $destinationNum = $destinationNum + 1
}
$destination = "$destinationParent$dstName$destinationNum.zip"
Write-Output "Zipping $sourceFolder to $destination"
Compress-Archive $sourceFolder $destination