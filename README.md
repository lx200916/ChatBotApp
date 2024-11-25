**mllm Chat Bot** is an Android application based on the multimodal LLM inference engine [**mllm**](https://github.com/UbiquitousLearning/mllm). It supports text/image conversations, Android Intent Invocation, and can support the use of CPU and some Hexagon NPUs.  

> [!NOTE]  
> ChatBot is a demo app of [MLLM Engine](https://github.com/UbiquitousLearning/mllm). Any error report or feature request should be opened in [**mllm**](https://github.com/UbiquitousLearning/mllm) Github Repo.

## Supported Functions  

<table>
  <tr>
    <th>Model</th>
    <th>Backend</th>
    <th style="text-align:center;">Chat</th>
    <th style="text-align:center;">Intent Invocation</th>
  </tr>
  <tr>
    <td rowspan="2">PhoneLM 1.5B</td>
    <td>CPU</td>
    <td style="text-align:center;">✔️</td>
    <td style="text-align:center;">✔️</td>
  </tr>
  <tr>
    <td>NPU</td>
    <td style="text-align:center;">✔️</td>
    <td style="text-align:center;">✔️</td>
  </tr>
  <tr>
    <td rowspan="2">Qwen-2.5 1.5B</td>
    <td>CPU</td>
    <td style="text-align:center;">✔️</td>
    <td style="text-align:center;">✔️</td>
  </tr>
  <tr>
    <td>NPU</td>
    <td style="text-align:center;"></td>
    <td style="text-align:center;"></td>
  </tr>
  <tr>
    <td rowspan="2">Qwen-1.5 1.8B</td>
    <td>CPU</td>
    <td style="text-align:center;">✔️</td>
    <td style="text-align:center;"></td>
  </tr>
  <tr>
    <td>NPU</td>
    <td style="text-align:center;">✔️</td>
    <td style="text-align:center;"></td>
  </tr>
  <tr>
    <td>Fuyu 8B</td>
    <td>CPU</td>
    <td style="text-align:center;">✔️</td>
    <td style="text-align:center;"></td>
  </tr>
</table>


## How to Build

### Get the Code

```bash
git clone https://github.com/lx200916/ChatBotApp
```

### Build JNI Lib
Get mllm codes:
```bash
git clone https://github.com/UbiquitousLearning/mllm
cd mllm
```

Build mllm_lib:
```bash
mkdir ../build-arm-app
cd ../build-arm-app

cmake .. \
-DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
-DCMAKE_BUILD_TYPE=Release \
-DANDROID_ABI="arm64-v8a" \
-DANDROID_NATIVE_API_LEVEL=android-28  \
-DNATIVE_LIBRARY_OUTPUT=. -DNATIVE_INCLUDE_OUTPUT=. $1 $2 $3 \
-DARM=ON \
-DAPK=ON \
-DQNN=ON \
-DDEBUG=OFF \
-DTEST=OFF \
-DQUANT=OFF \
-DQNN_VALIDATE_NODE=ON \
-DMLLM_BUILD_XNNPACK_BACKEND=OFF

make mllm_lib -j$(nproc)
```
Copy mllm_lib to ChatBotApp:
```bash
cp ./libmllm_lib.a ChatBotApp/app/src/main/cpp/libs/
```

### Download mllm models
You need to download mllm models from Web.
|  |   |
|-------|--------|
|[gte-small-fp32.mllm](https://huggingface.co/mllmTeam/gte-small-mllm/blob/main/gte-small-fp32.mllm) | |
|[phonelm-1.5b-instruct-q4_0_4_4.mllm](https://huggingface.co/mllmTeam/phonelm-1.5b-mllm/blob/main/phonelm-1.5b-instruct-q4_0_4_4.mllm)  |[phonelm-1.5b-instruct-int8.mllm](https://huggingface.co/mllmTeam/phonelm-1.5b-mllm/blob/main/phonelm-1.5b-instruct-int8.mllm) | 
|[phonelm-1.5b-call-q8_0.mllm](https://huggingface.co/mllmTeam/phonelm-1.5b-mllm/blob/main/phonelm-1.5b-call-q8_0.mllm) |[phonelm-1.5b-call-int8.mllm](https://huggingface.co/mllmTeam/phonelm-1.5b-mllm/blob/main/phonelm-1.5b-call-int8.mllm) |
|[qwen-2.5-1.5b-instruct-q4_0_4_4.mllm](https://huggingface.co/mllmTeam/qwen-2.5-1.5b-mllm/blob/main/qwen-2.5-1.5b-instruct-q4_0_4_4.mllm) ||
|[qwen-2.5-1.5b-call-q4_0_4_4.mllm](https://huggingface.co/mllmTeam/qwen-2.5-1.5b-mllm/blob/main/qwen-2.5-1.5b-call-q4_0_4_4.mllm) ||
|[qwen-1.5-1.8b-chat-q4_0_4_4.mllm](https://huggingface.co/mllmTeam/qwen-1.5-1.8b-chat-mllm/blob/main/qwen-1.5-1.8b-chat-q4_0_4_4.mllm) |[qwen-1.5-1.8b-chat-int8.mllm](https://huggingface.co/mllmTeam/qwen-1.5-1.8b-chat-mllm/blob/main/qwen-1.5-1.8b-chat-int8.mllm) | 
|[fuyu-8b-q4_k.mllm](https://huggingface.co/mllmTeam/fuyu-8b-mllm/blob/main/fuyu-8b-q4_k.mllm) | |

### Move models to Phone
Then you need to move these models and vocab files from [`mllm/vocab`](https://github.com/UbiquitousLearning/mllm/tree/main/vocab) to your Android Phone's File Path `/sdcard/Download/model/`. The following files are required to exist in  `/sdcard/Download/model/`:
```bash
/sdcard/Download/model/
|-- gte-small-fp32.mllm
|-- phonelm-1.5b-instruct-int8.mllm
|-- phonelm-1.5b-instruct-q4_0_4_4.mllm 
|-- phonelm-1.5b-call-int8.mllm
|-- phonelm-1.5b-call-q8_0.mllm
|-- qwen-2.5-1.5b-call-q4_0_4_4.mllm
|-- qwen-2.5-1.5b-instruct-q4_0_4_4.mllm
|-- qwen-1.5-1.8b-chat-int8.mllm
|-- qwen-1.5-1.8b-chat-q4_0_4_4.mllm
|-- fuyu-8b-q4_k.mllm 
|-- gte_vocab.mllm
|-- phonelm_vocab.mllm
|-- phonelm_merges.txt   
|-- qwen_vocab.mllm
|-- qwen_merges.txt
|-- qwen2.5_vocab.mllm               
|-- qwen2.5_merges.txt
|-- fuyu_vocab.mllm
```

### Build
Now you can import the project into Android Studio and build it.

If you do not use Android Studio, you may need to manually set up JDK(17+) and Android SDK(30+) environment, and then build it with gradle.
```bash
./gradlew assembleDebug
```
