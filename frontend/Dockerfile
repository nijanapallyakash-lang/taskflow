FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
# npm install (not ci) so the Linux-native optional deps of tailwind/rollup resolve
RUN npm install --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
# BACKEND_HOSTPORT is substituted into the template at container start
ENV BACKEND_HOSTPORT=backend:8080
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
