FROM node:23-alpine AS build
WORKDIR /app

ARG ENV_CONFIGURATION=production

COPY package*.json ./
RUN npm ci

COPY . .

RUN npm run build -- --configuration ${ENV_CONFIGURATION}

FROM nginx:alpine

COPY --from=build /app/dist/insights-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
