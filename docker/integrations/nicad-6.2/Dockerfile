FROM ubuntu:20.04

# we need gcc, make etc.
RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential \
    && rm -rf /var/lib/apt/lists/*

# create app dir
RUN mkdir /app/

# copy over required packages for NiCad
COPY target_app/24526-txl10.8.linux64.tar.gz /app/
COPY target_app/24401-NiCad-6.2.tar.gz /app/

# install freetxl
RUN cd /app/ && tar -xzf 24526-txl10.8.linux64.tar.gz && rm 24526-txl10.8.linux64.tar.gz && cd txl10.8.linux64 && ./InstallTxl
# build nicad
RUN cd /app/ && tar -xzf 24401-NiCad-6.2.tar.gz && rm 24401-NiCad-6.2.tar.gz && cd NiCad-6.2 && make all

# create alias
RUN printf '#!/bin/sh\n cd /app/NiCad-6.2/ && ./nicad6 "$@"' > /usr/bin/nicad && \
    chmod +x /usr/bin/nicad

# working directory (externally mounted)
WORKDIR /src
