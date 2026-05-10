编排：LLM 结构化 → PPTX → PNG → 逐页 TTS → 合并音频 → FFmpeg 成片。
根据输入的文本自动进行LLM结构化拆解，然后生成PPT图片，PPT图片对应的旁白通过TTS生成对应的音频，最终合并成一个完整的视频
<img width="353" height="898" alt="image" src="https://github.com/user-attachments/assets/b0484364-ea6f-4f0d-8ac9-6a6ee0039efa" />

需要提前准备好的东西：
本机先装好这些，我的环境是mac
JDK 17+（你本机已有 Java 就可以）
Maven：brew install maven（或从 Maven 官网 安装）
FFmpeg（含 ffprobe）：brew install ffmpeg
LibreOffice（用来把 PPT 导出成 PNG）：brew install --cask libreoffice （这里mac可能会遇到问题，代码已修复好）
如果下载不了，可以选择安装包下载的方式
官网安装包
打开：https://www.libreoffice.org/download/download/
下载 macOS 版，拖进「应用程序」，再用上面同一条 --version 命令验证。

# macOS
brew install poppler
# Ubuntu/Debian
sudo apt update && sudo apt install -y poppler-utils
# CentOS/RHEL
sudo yum install -y poppler-utils

安装好后在终端确认：
java -version
mvn -version
ffmpeg -version
ffprobe -version
/Applications/LibreOffice.app/Contents/MacOS/soffice --version

配置密钥（每次开终端或写进 ~/.zshrc）
export DEEPSEEK_API_KEY="你的DeepSeek密钥"
export MINIMAX_API_KEY="你的MiniMax密钥"



开发期间我遇到的问题，可能对你有帮助：

1.LibreOffice 在 macOS/Linux 无头模式下的已知限制：--convert-to png 默认只导出第一页，无法自动拆分为多页图片。
✅ 最稳定工业级方案：改为 PPTX → PDF → PNG序列。PDF 导出原生支持多页，再用 pdftoppm（轻量级标准工具）拆图，100% 兼容且速度快。
方案	多页支持	macOS 兼容	速度	维护成本
lo --convert-to png	❌ 仅第1页	差	中	高（需魔改宏）
lo → pdf → pdftoppm	✅ 原生支持	✅ 完美	极快	低（2步标准命令）

2.报错MiniMax 逐页配音… java.lang.IllegalStateException: MiniMax 错误：insufficient balance at com.ych.contentfactory.tts.MinimaxTtsClient.synthesizeOne(MinimaxTtsClient.java:85) at com.ych.contentfactory.tts.MinimaxTtsClient.synthesizeOrdered(MinimaxTtsClient.java:49) at com.ych.contentfactory.pipeline.ContentPipeline.run(ContentPipeline.java:83) at com.ych.contentfactory.ContentFactoryCliCreateCommand.call(ContentFactoryCli.java:58)atcom.ych.contentfactory.ContentFactoryCli CreateCommand.call(ContentFactoryCli.java:38) at picocli.CommandLine.executeUserObject(CommandLine.java:2045) at picocli.CommandLine.access1500(CommandLine.java:148)atpicocli.CommandLine RunLast.executeUserObjectOfLastSubcommandWithSameParent(CommandLine.java:2465) at picocli.CommandLineRunLast.handle(CommandLine.java:2457)atpicocli.CommandLine RunLast.handle(CommandLine.java:2419) at picocli.CommandLineAbstractParseResultHandler.execute(CommandLine.java:2277)atpicocli.CommandLine RunLast.execute(CommandLine.java:2421) at picocli.CommandLine.execute(CommandLine.java:2174) at com.ych.contentfactory.ContentFactoryCli.main(ContentFactoryCli.java:28) at com.ych.contentfactory.Application.main(Application.java:15)
MiniMax 账户余额/免费额度已耗尽，非代码逻辑问题。充钱就能解决......
🔍 错误定位
MiniMax 接口返回了标准计费拦截：
{"base_resp":{"status_code":-1,"status_msg":"insufficient balance"}}
✅ 解决步骤
登录控制台：打开 MiniMax 开放平台 → 控制台 → 账户中心/用量统计。
检查额度：查看 TTS 服务的免费字符额度是否已用完，或账户余额是否为 0。
充值或换 Key：
充值后通常 1~5 分钟 生效。
或在环境变量/配置文件中替换为有余额的 MINIMAX_API_KEY。
验证配置：确保 PipelineConfig 实际读取的 minimaxApiKey 与充值账户一致（可临时 System.out.println 打印前 4 位确认）。

3.报错java.lang.IllegalStateException: LLM 请求失败 HTTP 402：{"error":{"message":"Insufficient Balance","type":"unknown_error","param":null,"code":"invalid_request_error"}} at com.ych.contentfactory.llm.LlmPlanGenerator.generate(LlmPlanGenerator.java:83) at com.ych.contentfactory.pipeline.ContentPipeline.run(ContentPipeline.java:41) at com.ych.contentfactory.ContentFactoryCliCreateCommand.call(ContentFactoryCli.java:58)atcom.ych.contentfactory.ContentFactoryCli CreateCommand.call(ContentFactoryCli.java:38) at picocli.CommandLine.executeUserObject(CommandLine.java:2045) at picocli.CommandLine.access1500(CommandLine.java:148)atpicocli.CommandLine RunLast.executeUserObjectOfLastSubcommandWithSameParent(CommandLine.java:2465) at picocli.CommandLine$RunLast.handle(CommandLine.java:2457)
这个错误 不是代码逻辑问题，而是 LLM 服务商返回的标准计费异常： HTTP 402 + "Insufficient Balance" 表示当前配置的 API Key 所在账户 余额已耗尽/额度不足。
🔍 解决步骤
检查账户余额
登录你当前 cfg.llmBaseUrl 对应的 LLM 平台控制台（默认是 DeepSeek：https://platform.deepseek.com）
查看 API 充值额度、免费额度是否已用完，或套餐是否过期。
充值或更换 Key
充值后等待 1~3 分钟生效。
或在环境变量/配置文件中更新为有余额的 LLM_API_KEY 和对应的 LLM_BASE_URL。
验证配置是否生效
确保程序读取的是最新的配置（如通过 PipelineConfig 打印日志确认 llmBaseUrl 和 llmApiKey 实际值）。
