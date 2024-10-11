dataSource 'lasso_quickstart'

study(name: 'ICSE2025Demo-DataDriven') {

  /* Execution profile */
  profile('java17Profile') {
    scope('class') { type = 'class' }
    environment('java17') {
      image = 'maven:3.9-eclipse-temurin-17' // docker image (JDK 17)
    }
  }

  /* Generate code using LLMs */
  action(name: 'generate', type: 'GenerativeAI') {
    profile('java17Profile')
    // OpenAI Completitions endpoint (here Ollama Open-WebUI)
    apiUrl = "http://bagdana.informatik.uni-mannheim.de:8080/api/chat/completions"
    apiKey = "sk-f5690a9943504087a40eeecd881c4170" // your API key (generate one in your profile)
    def codeModels = ["deepseek-coder-v2:latest", "codellama:34b", "qwen2.5:32b"]
    codeModels.each{codeModel ->
      (1..5).each { sample -> // sample 5 code generations for each model
        abstraction("Base64Encode") { // identifier for coding problem (container for solutions)
          prompt 'write a java method that encodes a string to base64 without padding and return a string. Wrap the method in a class.'
          model = codeModel
          sampleId = sample
          temperature = 0.8
        }
      }
    }
  }

  /* Observe run-time behavior using sequence sheets */
  action(name: 'observe', type: 'ArenaExecute') {
    dependsOn 'generate'
    includeAbstractions 'Base64Encode'
    profile('java17Profile')
    // LQL interface
    specification = 'Base64{encode(java.lang.String)->java.lang.String}'
    sequences = [
        'testEncode': sheet(base64:'Base64', p2:"user:pass") {
          row '',  'create', '?base64'
          row 'dXNlcjpwYXNz',  'encode',  'A1',   '?p2'
        },
        'testEncode_noPadding': sheet(base64:'Base64', p2:"Hello World") {
          row '',  'create', '?base64'
          row 'SGVsbG8gV29ybGQ','encode',  'A1',   '?p2' }]
  }

}
