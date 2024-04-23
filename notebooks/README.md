# LASSO project website


## Jupyter Lite

Static data is located in `files/`.

```bash
python3 -m venv jupyterlite-lasso
source jupyterlite-lasso/bin/activate

mkdir lasso
cd lasso/

pip install jupyterlite-core
jupyter lite --version
pip install jupyterlite-core[all] 
pip install jupyterlite-pyodide-kernel
# to open LASSO's generated notebooks
pip install jupyterlab-open-url-parameter


mkdir files
# copy over ...

jupyter lite build --output-dir dist

# or serve
jupyter lite serve  --output-dir dist

# copy to src/main/resources/
```
