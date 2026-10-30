FROM quay.io/keycloak/keycloak:26.7.4 AS builder

ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_HTTP_RELATIVE_PATH=/auth
ENV KC_HTTP_MANAGEMENT_RELATIVE_PATH=/

WORKDIR /opt/keycloak
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.7.4

COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY infra/keycloak/marketplace-realm.json /opt/keycloak/data/import/marketplace-realm.json

ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_HTTP_RELATIVE_PATH=/auth
ENV KC_HTTP_MANAGEMENT_RELATIVE_PATH=/
