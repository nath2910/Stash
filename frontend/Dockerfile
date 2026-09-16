# Production frontend: Vite is used only while building. The resulting image
# contains no development server, HMR client, watcher, or Node runtime.
FROM node:20-bookworm-slim AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . ./

ARG VITE_API_URL=https://api.mystash.fr
ARG VITE_GOOGLE_OAUTH_URL=https://api.mystash.fr/oauth2/authorization/google
ARG VITE_DISCORD_OAUTH_URL=https://api.mystash.fr/oauth2/authorization/discord
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_GOOGLE_OAUTH_URL=$VITE_GOOGLE_OAUTH_URL
ENV VITE_DISCORD_OAUTH_URL=$VITE_DISCORD_OAUTH_URL
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
