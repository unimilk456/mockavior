## build project 

`./gradlew clean bootJar`

## rename builded file
`cp build/libs/mockavior-*.jar mockavior.jar`

## Check Dockerfile in the root

## Build Docker image
`docker build -t mockavior/mockavior:1.0.0 .`

`docker tag mockavior/mockavior:1.0.0 mockavior/mockavior:latest`

## Check image
`docker images | grep mockavior`

## Local test
 Run contaner
`docker run --rm \
  -p 8080:8080 \
  -v $(pwd)/config/mockapi.yml:/app/config/mockapi.yml \
  mockavior/mockavior:1.0.0`

send request
`curl -i http://localhost:8080/__mockavior__/contract`

## Login to Docker
`docker login`

## Re-tag image
`docker tag mockavior/mockavior:1.0.0 unisoft123/mockavior:1.0.0`
`docker tag mockavior/mockavior:1.0.0 unisoft123/mockavior:latest`


## Push docker image to REPO
`docker push unisoft123/mockavior:1.0.0`
`docker push unisoft123/mockavior:latest`