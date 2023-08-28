param ([Parameter(Mandatory)] $DockerRegistry)
Write-Host "Using Docker registry: $DockerRegistry"

$result = Select-String -Pattern "^version '(?<version>\d+\.\d+\.\d+)'" -Path "build.gradle"
if($result.Matches.Length -eq 0) {
    Write-Host "Could not detect version, stopping."
    exit 1
}
$version = $result.Matches[0].Groups["version"]
Write-Host "Detected version: $version"

$commit = git log --format="%h" -n 1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Could not get commit hash"
    exit 1
}
Write-Host "Detected commit hash: $commit"

$tagLatest = "$DockerRegistry/skull-king:latest"
$tagVersioned = "$DockerRegistry/skull-king:$version-$commit"
Write-Host "Docker image tags: $tagVersioned $tagLatest"

./gradlew clean
./gradlew bootJar
if ($LASTEXITCODE -ne 0) {
    Write-Host "gradlew task 'bootJar' failed"
    exit 1
}

docker image rm $tagLatest
docker build -t $tagLatest .
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker build failed"
    exit 1
}

docker image tag $tagLatest $tagVersioned
docker push $tagLatest
docker push $tagVersioned
