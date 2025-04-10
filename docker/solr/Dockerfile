FROM swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-solr:9.8.1
# The following command moves saved volume data to the original volume location
RUN mkdir -p /var/solr && cp -a /tmp/my_vol_backup/. /var/solr/ && rm -rf /tmp/my_vol_backup
VOLUME ["/var/solr"]