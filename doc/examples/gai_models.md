### APIs like OpenAI's Completions Endpoint

* llama.cpp
    * https://github.com/ggerganov/llama.cpp/tree/master/examples/server
* gpt4all
    * https://github.com/nomic-ai/gpt4all
* ollama + open-webui

#### Sample llama.cpp server with DeepSeek-Coder

```bash
wget "https://huggingface.co/TheBloke/deepseek-coder-33B-instruct-GGUF/resolve/main/deepseek-coder-33b-instruct.Q5_K_M.gguf"
cd ~/github/llama.cpp
./server -m /path/to/deepseek-coder-33b-instruct.Q5_K_M.gguf --host myhost -ngl 63 --api-key mysecretkey
```

Note that `-ngl` only works if you have a GPU. `--api-key` makes your instance a bit more secure.

##### GPU support (cuBLAS) - Ubuntu 22.04LTS

```bash
# install Nvidia drivers
wget https://download.nvidia.com/XFree86/Linux-x86_64/550.54.14/NVIDIA-Linux-x86_64-550.54.14.run
apt install build-essential dkms
nvidia-smi
sh NVIDIA-Linux-x86_64-550.54.14.run 
reboot
nvidia-smi

# install cuda
wget https://developer.download.nvidia.com/compute/cuda/repos/ubuntu2204/x86_64/cuda-ubuntu2204.pin
sudo mv cuda-ubuntu2204.pin /etc/apt/preferences.d/cuda-repository-pin-600
wget https://developer.download.nvidia.com/compute/cuda/12.4.0/local_installers/cuda-repo-ubuntu2204-12-4-local_12.4.0-550.54.14-1_amd64.deb
sudo dpkg -i cuda-repo-ubuntu2204-12-4-local_12.4.0-550.54.14-1_amd64.deb
sudo cp /var/cuda-repo-ubuntu2204-12-4-local/cuda-*-keyring.gpg /usr/share/keyrings/
sudo apt-get update
sudo apt-get -y install cuda-toolkit-12-4

# checkout llama.cpp and compile with cuBLAS support
git clone https://github.com/ggerganov/llama.cpp.git
export PATH=/usr/local/cuda-12.4/bin${PATH:+:${PATH}}
cd llama.cpp/
make LLAMA_CUBLAS=1
```

#### ollama + open-webui (supports model switching)

* https://github.com/ollama/ollama uses llama.cpp under the hood
* https://github.com/open-webui/open-webui connects to ollama

API endpoints documentation:
* https://ollama.com/blog/openai-compatibility
* https://github.com/ollama/ollama/blob/main/docs/openai.md

```shell
# ollama with Nvidia GPU docker support (see https://hub.docker.com/r/ollama/ollama)
curl -fsSL https://nvidia.github.io/libnvidia-container/gpgkey     | sudo gpg --dearmor -o /usr/share/keyrings/nvidia-container-toolkit-keyring.gpg
curl -s -L https://nvidia.github.io/libnvidia-container/stable/deb/nvidia-container-toolkit.list     | sed 's#deb https://#deb [signed-by=/usr/share/keyrings/nvidia-container-toolkit-keyring.gpg] https://#g'     | sudo tee /etc/apt/sources.list.d/nvidia-container-toolkit.list
sudo apt-get update
apt-get install -y nvidia-container-toolkit
shutdown -r now

# ollama
docker run -d --gpus=all -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama

# open web gui (here via network host; alternative is to use docker ip) - see https://github.com/open-webui/open-webui
docker run -d --network=host -v open-webui:/app/backend/data -e OLLAMA_BASE_URL=http://127.0.0.1:11434 --name open-webui --restart always ghcr.io/open-webui/open-webui:main

# either pull models inside open-webui or use docker commands (see https://ollama.com/library)
docker exec -it ollama ollama pull deepseek-coder:33b
```

Available service endpoints:
* ollama: http://127.0.0.1:11434 (OpenAI compatible)
* openweb-ui: http://127.0.0.1:8080 (or :3000 if port mappings are used)