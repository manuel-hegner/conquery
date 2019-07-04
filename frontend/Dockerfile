FROM node:10.16-alpine

RUN mkdir /conquery
WORKDIR /conquery

COPY ./package.json ./yarn.lock ./
RUN yarn --no-progress --frozen-lockfile

COPY . .

ENV NODE_ENV production

RUN yarn run build

EXPOSE 8000

CMD [ "node", "app/server.js" ]
