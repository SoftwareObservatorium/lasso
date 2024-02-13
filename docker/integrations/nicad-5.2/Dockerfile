FROM ubuntu:18.04

# we need gcc, make etc.
RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential \
    && rm -rf /var/lib/apt/lists/*

# create app dir
RUN mkdir /app/

# copy over required packages for NiCad
COPY target_app/26624-txl10.7.linux64.tar.gz /app/
COPY target_app/26688-NiCad-5.2.tar.gz /app/

# install freetxl
RUN cd /app/ && tar -xzf 26624-txl10.7.linux64.tar.gz && rm 26624-txl10.7.linux64.tar.gz && cd txl10.7.linux64 && ./InstallTxl
# build nicad
RUN cd /app/ && tar -xzf 26688-NiCad-5.2.tar.gz && rm 26688-NiCad-5.2.tar.gz && cd NiCad-5.2 && make all

# create alias
RUN printf '#!/bin/sh\n cd /app/NiCad-5.2/ && ./nicad5 "$@"' > /usr/bin/nicad && \
    chmod +x /usr/bin/nicad

# working directory (externally mounted)
WORKDIR /src
