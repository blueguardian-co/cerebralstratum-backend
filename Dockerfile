FROM registry.access.redhat.com/ubi9 AS ubi-micro-build

ENV CEREBRALSTRATUM_BACKEND_VERSION 999.0.0-SNAPSHOT
ARG CEREBRALSTRATUM_BACKEND_DIST=https://github.com/blueguardian-co/cerebral-stratum-backend/releases/download/$CEREBRALSTRATUM_BACKEND_VERSION/cerebral-stratum-backend-$CEREBRALSTRATUM_BACKEND_VERSION.tar.gz

RUN dnf install -y tar gzip

ADD $CEREBRALSTRATUM_BACKEND_DIST /tmp/cerebral_stratum_backend/

# The next step makes it uniform for local development and upstream built.
# If it is a local tar archive then it is unpacked, if from remote is just downloaded.
RUN (cd /tmp/cerebral_stratum_backend && \
    tar -xvf /tmp/cerebral_stratum_backend/cerebral-stratum-backend-*.tar.gz && \
    rm /tmp/cerebral_stratum_backend/cerebral-stratum-backend-*.tar.gz) || true

RUN mv /tmp/cerebral_stratum_backend/cerebral-stratum-backend-* /opt/cerebral_stratum_backend && mkdir -p /opt/cerebral_stratum_backend/data
RUN chmod -R g+rwX /opt/cerebral_stratum_backend

ADD ubi-null.sh /tmp/
RUN bash /tmp/ubi-null.sh java-17-openjdk-headless glibc-langpack-en findutils

FROM registry.access.redhat.com/ubi9-micro
ENV LANG en_US.UTF-8

COPY --from=ubi-micro-build /tmp/null/rootfs/ /
COPY --from=ubi-micro-build --chown=1000:0 /opt/cerebral_stratum_backend /opt/cerebral_stratum_backend

RUN echo "cerebral_stratum_backend:x:0:root" >> /etc/group && \
    echo "cerebral_stratum_backend:x:1000:0:cerebral_stratum_backend user:/opt/cerebral_stratum_backend:/sbin/nologin" >> /etc/passwd

USER 1000

EXPOSE 6443

ENTRYPOINT [ "/opt/cerebral_stratum_backend/bin/start.sh" ]