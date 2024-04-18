# Examples for Action `GenerativeAI`

## Prompt ChatGPT to generate a Base64 class that for encoding strings and verify its functional correctness

This requires the presence of an artifact repository for deployment (see [nexus.md](..%2Fnexus.md)).

```groovy
dataSource 'lasso_quickstart'

/** Define a new study */
study(name: 'GenerativeAI-Base64') {

    /** defines profile (compiler etc.) */
    profile('java17Profile') {
        scope('class') { type = 'class' } // measurement scope
        environment('java17') { // execution environment
            image = 'maven:3.6.3-openjdk-17' // (docker) image template
        }
    }
    
    /** This requires an account for OpenAI's ChatGPT */
    action(name: 'gai', type: 'GenerativeAI') {
        apiUrl = "https://api.openai.com/v1/chat/completions"
        apiKey = "XXXXX" // your API key (generate one in your profile)
        
        abstraction('Base64Encode') {           
            // NLP prompt         
            prompt 'write a java method that encodes a string to base64 without padding and returns a byte array'
            
            model = "gpt-3.5-turbo"
            role = "user"
            temperature = 0.7
        }
        
        profile('java17Profile') // for building purposes
    }
    
    // filter action
    action(name: 'filter', type: 'ArenaExecute') { // filter by tests
        specification = 'Base64{encode(java.lang.String)->byte[]}'
        sequences = [
                'testEncode': sheet(base64:'Base64', p2:"user:pass") {
                    row  '',    'create', '?base64'
                    row 'dXNlcjpwYXNz'.getBytes(),  'encode',   'A1',     '?p2'
                },
                'testEncode_padding': sheet(base64:'Base64', p2:"Hello World") {
                    row  '',    'create', '?base64'
                    row 'SGVsbG8gV29ybGQ'.getBytes(),  'encode',   'A1',     '?p2'
                }
        ]
        maxAdaptations = 1 // how many adaptations to try
        features = ["cc"] // enable code coverage

        dependsOn 'gai'
        includeAbstractions '*'
        profile('java17Profile')

        whenAbstractionsReady() {
            def base64 = abstractions['Base64Encode']
            // define oracle based on expected responses in sequences
            def expectedBehaviour = toOracle(srm(abstraction: base64).sequences)
            // returns a filtered SRM
            def matchesSrm = srm(abstraction: base64)
                    .systems // select all systems
                    .equalTo(expectedBehaviour) // functionally equivalent

            // iterate over sub-SRM
            matchesSrm.systems.each { s ->
                log("Matched class ${s.id}, ${s.packageName}.${s.name}")
            }
            // continue pipeline with matched systems only
            base64.systems = matchesSrm.systems
        }
    }
}
```

### APIs like OpenAI's Completions Endpoint

* llama.cpp
  * https://github.com/ggerganov/llama.cpp/tree/master/examples/server
* gpt4all
  * https://github.com/nomic-ai/gpt4all

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