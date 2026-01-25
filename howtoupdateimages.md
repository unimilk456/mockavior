## build project 

`./gradlew clean bootJar`

## rename builded file
`cp build/libs/Mockavior-*.jar  build/libs/mockavior.jar`

## Check Dockerfile in the root

## Build Docker image
`docker build -t mockavior/mockavior:3.0.0 .`

`docker tag mockavior/mockavior:3.0.0 mockavior/mockavior:latest`

## Check image
`docker images | grep mockavior`

## Local test
 Run contaner
`docker run --rm -p 8080:8080 -v $(pwd)/config:/app/config mockavior/mockavior:3.0.0`

send request
`curl -i http://localhost:8080/health`

## Login to Docker
`docker login`

## Re-tag image
`docker tag mockavior/mockavior:2.0.0 unisoft123/mockavior:2.0.0`
`docker tag mockavior/mockavior:2.0.0 unisoft123/mockavior:latest`


## Push docker image to REPO
`docker push unisoft123/mockavior:2.0.0`
`docker push unisoft123/mockavior:latest`