# hmpps-pin-phone-api

[![Ministry of Justice Repository Compliance Badge](https://github-community.service.justice.gov.uk/repository-standards/api/hmpps-pin-phone-api/badge)](https://github-community.service.justice.gov.uk/repository-standards/hmpps-pin-phone-api)
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-pin-phone-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://pin-phone-dev.prison.service.justice.gov.uk/swagger-ui/index.html)

Template github repo used for new Kotlin based projects.

# Instructions

If this is a HMPPS project then the project will be created as part of bootstrapping -
see [hmpps-project-bootstrap](https://github.com/ministryofjustice/hmpps-project-bootstrap). You are able to specify a
template application using the `github_template_repo` attribute to clone without the need to manually do this yourself
within GitHub.

This project is community managed by the mojdt `#kotlin-dev` slack channel.
Please raise any questions or queries there. Contributions welcome!

Our security policy is located [here](https://github.com/ministryofjustice/hmpps-pin-phone-api/security/policy).

Documentation to create new service is located [here](https://tech-docs.hmpps.service.justice.gov.uk/creating-new-services/).


## Running application locally

Ensure dependent services are running:

1. Medusa: https://github.com/ministryofjustice/hmpps-pin-phone-medusa-service-api
2. UI: https://github.com/ministryofjustice/hmpps-pin-phone-ui

### Running the application in Intellij

1. Get client secrets from DEV namespace. Export or add to IntelliJ run configuration:


    CLIENT_ID={id}

    CLIENT_SECRET={secret}

2. Spin up docker wiremock, as it is required for BT.

Note: PrisonAPI and Prisoner Search can be ran against DEV, or wiremock, update application-dev.yml accordingly


    docker compose up wiremock -d


### Building and running the docker image locally


1. Get client secrets from DEV namespace.
Update docker compose 

       CLIENT_ID={id}
    
       CLIENT_SECRET={secret}


The `Dockerfile` relies on the application being built first. Steps to build the docker image:
2. Build the jar files.
```
./gradlew clean assemble
```

3. Build the docker image with required arguments.
```
docker build --build-arg BUILD_NUMBER=$(ls build/libs/hmpps-pin-phone-api-*.jar | sed -E 's/.*api-(.*)\.jar/\1/') \
  -t ghcr.io/ministryofjustice/hmpps-pin-phone-api:local .
```
4. Run the docker image.
```
docker compose up -d
```
## Common Kotlin patterns

Many patterns have evolved for HMPPS Kotlin applications. Using these patterns provides consistency across our suite of
Kotlin microservices and allows you to concentrate on building your business needs rather than reinventing the
technical approach.

Documentation for these patterns can be found in the [HMPPS tech docs](https://tech-docs.hmpps.service.justice.gov.uk/common-kotlin-patterns/).
If this documentation is incorrect or needs improving please report to [#ask-prisons-digital-sre](https://moj.enterprise.slack.com/archives/C06MWP0UKDE)
or [raise a PR](https://github.com/ministryofjustice/hmpps-tech-docs).