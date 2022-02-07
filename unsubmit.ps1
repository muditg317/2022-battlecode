if ($args.Count -lt 2) {
    Write-Output "You must supply the zip name and package name to extract to!"
    exit
}
$zipName = $args[0]
$sourceZip = "submissions/$zipName.zip"
if (-Not (Test-Path -Path $sourceZip)) {
    Write-Output "$sourceZip does not exist!"
    exit
}
$destinationParent = "src/"
if (-Not (Test-Path -Path $destinationParent)) {
    mkdir -Path $destinationParent
    Write-Output "Create $destinationParent folder"
}
$dstName = $args[1]
$destination = "$destinationParent$dstName"
if (Test-Path -Path "$destination") {
    Write-Output "$destination already exists!"
    exit
}
Write-Output "Unzipping $sourceZip to $destination"
Expand-Archive $sourceZip $destination

$dstSrc = "$destination/firstbot"
$dstSrcParam = "$dstSrc/*"
$finalDst = $destination
Write-Output "Copying $dstSrc to $finalDst"
Copy-Item -Path $dstSrcParam -Destination $finalDst -Recurse

Write-Output "Delete $dstSrc"
Remove-Item $dstSrc -Recurse