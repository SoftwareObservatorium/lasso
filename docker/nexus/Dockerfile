FROM swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-nexus:3.79.0
# The following command moves saved volume data to the original volume location
RUN mkdir -p /nexus-data && cp -a /tmp/my_vol_backup/. /nexus-data/ && rm -rf /tmp/my_vol_backup
VOLUME ["/nexus-data"]