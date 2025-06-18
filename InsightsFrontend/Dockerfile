ARG BUILD_ENV=production

FROM node:23-alpine AS build
WORKDIR /app

ARG BUILD_ENV

COPY package*.json ./
RUN npm ci 

COPY . .
RUN npm run build -- --configuration=${BUILD_ENV}

FROM nginx:alpine

COPY --from=build /app/dist/insights-frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
