#!/usr/bin/env bash

## Requirements
# gcloud 	    - https://cloud.google.com/sdk/docs/quickstart-linux
# gcloud kubctl - https://cloud.google.com/kubernetes-engine/docs/quickstart
# jq 		    - https://stedolan.github.io/jq/; for MAC: brew install jq

pushd $( dirname "${BASH_SOURCE[0]}" ) >/dev/null 2>&1

if ! [ -x "$(command -v gcloud)" ]; then
  echo 'Error: gcloud is not installed. After install (https://cloud.google.com/sdk/docs/quickstart-linux) open new shell.' >&2
  exit 1
fi

if ! [ -x "$(command -v kubectl)" ]; then
  echo 'Error: kubectl is not installed. Run `gcloud components install kubectl`' >&2
  exit 1
fi

gcloud projects list && \
read -p "Enter Google Cloud Project (leave empty to create new project): " GC_PROJECT_ID

if [[ -z ${GC_PROJECT_ID} ]]; then
  gcloud organizations list && \
  read -p "Google Cloud Organization ID (leave empty to create project without organization): " ORG_ID && \

  gcloud beta billing accounts list && \
  read -p "Google Cloud Billing ACCOUNT_ID: " BILLING_ID
fi

if [[ -z ${APP_NAME} ]]; then
  read -p "Enter a name for the application (will be used in identifiers for the project): " APP_NAME
else
  echo "Application name is set to '${APP_NAME}' by the APP_NAME environment variable."
fi

if [[ -z ${DOCKERFILE} ]]; then
  read -p "Enter path to the Dockerfile to build: " DOCKERFILE
else
  echo "Application name is set to '${DOCKERFILE}' by the DOCKERFILE environment variable."
fi

# pop out of scripts dir to get correct absolute path to Dockerfile from user's perspective
popd >/dev/null 2>&1
DOCKERFILE=$(pwd)/"$DOCKERFILE"
pushd $( dirname "${BASH_SOURCE[0]}" ) >/dev/null 2>&1
echo "Going to build from Dockerfile: ${DOCKERFILE}"

if [[ -z ${EXPOSE_PORT} ]]; then
  read -p "Enter port that is exposed by you application: " EXPOSE_PORT
else
  echo "Application name is set to '${EXPOSE_PORT}' by the EXPOSE_PORT environment variable."
fi

uuid=$(python -c "import uuid; print(str(uuid.uuid4())[:8].lower())")

### set env variables
export ORG_ID=${ORG_ID}
export LOCATION=europe-west1
export ZONE=europe-west3-c

if [[ -z ${GC_PROJECT_ID} ]]; then
  ### create project
  echo "create project ..."
  if [[ -z '$ORG_ID' || ${ORG_ID}=='' ]]; then
    gcloud projects create ${GC_PROJECT_ID} \
      --set-as-default
  else
    gcloud projects create ${GC_PROJECT_ID} \
      --organization ${ORG_ID} \
      --set-as-default
  fi

  gcloud beta billing projects link ${GC_PROJECT_ID} \
    --billing-account ${BILLING_ID} && \
else
  GC_PROJECT_ID=gke-${APP_NAME}-${uuid}
fi

gcloud config set project ${GC_PROJECT_ID} \

echo "... created project with id: ${GC_PROJECT_ID} ."

### activate services
echo "activate services ..." && \
gcloud services enable cloudbilling.googleapis.com && \
gcloud services enable compute.googleapis.com && \
gcloud services enable container.googleapis.com && \
gcloud services enable cloudbuild.googleapis.com

### build application using Google cloudbuild
echo "build application and push to GCR ... (this can take some time, depending on the size of the created image)"

gcloud auth configure-docker && \
docker build -t eu.gcr.io/${GC_PROJECT_ID}/${APP_NAME} -f ${DOCKERFILE} $(dirname "$DOCKERFILE") && \
docker push eu.gcr.io/${GC_PROJECT_ID}/${APP_NAME}:latest

### create GKE cluster
echo "create cluster ..."
# TODO: WARNING auto-upgrade will be enabled by default - maybe disable
gcloud container clusters create "cluster-${APP_NAME}" \
  --project=${GC_PROJECT_ID} \
  --billing-project=${GC_PROJECT_ID} \
  --machine-type=n1-standard-1 \
  --num-nodes=1 \
  --disk-size=50GB \
  --node-locations=${ZONE} \
  --zone=${ZONE}

gcloud container clusters get-credentials "cluster-${APP_NAME}" --zone=${ZONE}

### deploy and expose the application
echo "deploy application ..."

kubectl run ${APP_NAME} --image eu.gcr.io/${GC_PROJECT_ID}/${APP_NAME}:latest --port ${EXPOSE_PORT} && \
kubectl expose deployment ${APP_NAME} --type LoadBalancer --port 80 --target-port ${EXPOSE_PORT}

IP=$(kubectl get service "$APP_NAME" --output jsonpath='{.status.loadBalancer.ingress[0].ip}')
while [[ -z ${IP} ]]; do
  sleep 5
  IP=$(kubectl get service "$APP_NAME" --output jsonpath='{.status.loadBalancer.ingress[0].ip}')
done

echo "Your application is available at: http://${IP}"

