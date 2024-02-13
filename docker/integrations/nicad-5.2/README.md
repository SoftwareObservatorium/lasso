# Build
docker image build -t nicad:5.2 .
# Run
docker run -d --user $(id -u):$(id -u) -v /home/m/Downloads/:/src/ nicad:5.2 nicad functions java /src/systems/ LASSO_type2-report
