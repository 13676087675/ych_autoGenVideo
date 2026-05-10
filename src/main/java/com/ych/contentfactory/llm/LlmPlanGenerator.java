package com.ych.contentfactory.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ych.contentfactory.config.PipelineConfig;
import com.ych.contentfactory.model.PresentationPlan;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 调用 Chat Completions（默认 DeepSeek：{@code https://api.deepseek.com/chat/completions}），
 * 将用户粘贴的资料整理为结构化演示与逐页旁白；亦可通过环境变量改用其它 OpenAI 兼容网关。
 */
public final class LlmPlanGenerator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PipelineConfig cfg;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public LlmPlanGenerator(PipelineConfig cfg) {
        this.cfg = cfg;
    }

    public PresentationPlan generate(String userRequest) throws Exception {
//        String system = """
//                你是专业的应聘/述职演示设计助手。用户会粘贴「需求说明」和「候选人资料」（可能包含中文），不需要联网检索。
//                你必须只输出一个 JSON 对象（不要 Markdown 代码围栏，不要额外解释），字段如下：
//                {
//                  "title": "PPT 主标题",
//                  "subtitle": "副标题（如：应聘岗位或一句话定位）",
//                  "openingNarration": "封面页的口语化旁白，适合直接朗读，约 25-55 秒",
//                  "bodySlides": [
//                    {
//                      "heading": "本页标题",
//                      "bullets": ["要点1","要点2","最多5条，每条简短"],
//                      "narration": "与本页要点严格对应的口语化旁白，适合配音，可稍长"
//                    }
//                  ],
//                  "closingNarration": "结束页感谢与收束，约 15-40 秒"
//                }
//                约束：
//                - bodySlides 建议 6-14 页；信息不足就少一些，不要编造不存在的工作经历。
//                - bullets 每页 3-5 条；narration 用「我/本人」等第一人称，自然停顿，避免过长的单句。
//                - 语言与资料一致（默认中文）。
//                """;
//
//        Map<String, Object> payload = new LinkedHashMap<>();
//        payload.put("model", cfg.llmModel);
//        payload.put("temperature", 0.6);
//        payload.put("messages", List.of(
//                Map.of("role", "system", "content", system),
//                Map.of("role", "user", "content", userRequest)
//        ));
//        payload.put("stream", false);
//        if (cfg.deepSeekReasoningExtras) {
//            payload.put("thinking", Map.of("type", "enabled"));
//            payload.put("reasoning_effort", "high");
//        }
//
//        String body = MAPPER.writeValueAsString(payload);
//
//        URI uri = URI.create(cfg.llmBaseUrl + "/chat/completions");
//        HttpRequest req = HttpRequest.newBuilder(uri)
//                .timeout(Duration.ofMinutes(3))
//                .header("Authorization", "Bearer " + cfg.llmApiKey)
//                .header("Content-Type", "application/json")
//                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
//                .build();
//
//        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
//        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
//            throw new IllegalStateException("LLM 请求失败 HTTP " + resp.statusCode() + "：" + resp.body());
//        }

//        System.out.println("deepseek最终返回结果："+resp.body());

        String tempStr="{\"id\":\"f0ef5344-fa8f-4c68-a5af-b16e3705b535\",\"object\":\"chat.completion\",\"created\":1778399671,\"model\":\"deepseek-v4-pro\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"{\\n  \\\"title\\\": \\\"杨崇海 · Java开发工程师\\\",\\n  \\\"subtitle\\\": \\\"6年经验 · 擅长高并发后端与效率工具建设\\\",\\n  \\\"openingNarration\\\": \\\"各位面试官好，我是杨崇海，今年28岁，来自广东汕头。我本科毕业于暨南大学软件工程专业，有6年Java开发经验，先后在平安、天猫、SHEIN和嘉银科技等企业负责核心业务开发。今天主要向您介绍我的技术能力和项目经历，希望能有机会加入团队。\\\",\\n  \\\"bodySlides\\\": [\\n    {\\n      \\\"heading\\\": \\\"个人概况\\\",\\n      \\\"bullets\\\": [\\n        \\\"28岁 · 男 · 广东汕头\\\",\\n        \\\"6年Java后端开发经验\\\",\\n        \\\"暨南大学（211）软件工程 本科\\\",\\n        \\\"现居上海，随时到岗\\\"\\n      ],\\n      \\\"narration\\\": \\\"我先简单介绍一下自己。我今年28岁，本科毕业于暨南大学软件工程专业。目前我已经工作了6年，一直从事Java后端开发，现在定居上海，可以随时入职。\\\"\\n    },\\n    {\\n      \\\"heading\\\": \\\"教育背景\\\",\\n      \\\"bullets\\\": [\\n        \\\"2016-2020 暨南大学 软件工程 学士\\\",\\n        \\\"211工程重点建设高校\\\",\\n        \\\"在校期间获优秀学子标兵奖\\\",\\n        \\\"港澳台侨创新创业大赛一等奖\\\",\\n        \\\"挑战杯省赛银奖、校赛金奖\\\"\\n      ],\\n      \\\"narration\\\": \\\"我的教育背景是暨南大学软件工程本科。大学期间我比较注重实践，带领团队参加过很多比赛，拿到了港澳台侨创新创业大赛的一等奖，还有挑战杯省赛银奖和校赛金奖。这些经历锻炼了我的技术落地能力和团队协作能力。\\\"\\n    },\\n    {\\n      \\\"heading\\\": \\\"技术栈概览\\\",\\n      \\\"bullets\\\": [\\n        \\\"核心：SpringBoot/Cloud、MyBatis、MySQL、TiDB\\\",\\n        \\\"中间件：Redis、Kafka、RabbitMQ、Nacos、Zookeeper\\\",\\n        \\\"部署与运维：Docker、Jenkins、Linux、线上排错\\\",\\n        \\\"精通Java（85%），熟练Python（80%）\\\",\\n        \\\"前端：HTML/CSS/JS、Bootstrap、jQuery\\\"\\n      ],\\n      \\\"narration\\\": \\\"技术栈方面，我熟练掌握SpringBoot和SpringCloud微服务体系，常用MyBatis和MySQL、TiDB数据库。中间件上经常使用Redis、Kafka、RabbitMQ和Nacos。在运维方面，我有Docker容器化部署经验，能独立搭建Jenkins自动化构建平台，也具备比较扎实的线上问题排查能力。另外，我也会写Python和前端基础，帮助团队做一些效率工具。\\\"\\n    },\\n    {\\n      \\\"heading\\\": \\\"上海嘉银科技 · 高级Java开发\\\",\\n      \\\"bullets\\\": [\\n        \\\"负责司法诉讼平台还款与账单核心业务\\\",\\n        \\\"重构还款代码，实现线下流程全线上化\\\",\\n        \\\"建设自动对账、自动还款与规则配置引擎\\\",\\n        \\\"优化慢SQL，引入Charles/Arthas提升定位效率\\\",\\n        \\\"开发代码分析工具，结合大模型自动解析告警日志\\\"\\n      ],\\n      \\\"narration\\\": \\\"最近一段经历是在上海嘉银科技，我作为高级Java开发负责司法诉讼平台。我主导了还款业务的独立改造，把原本线下的还款流程全部线上化，包括自动对账、自动还款和灵活的规则配置引擎。同时，我主动优化了系统的慢SQL问题，在团队里推广使用Charles和Arthas，提升了自测和线上问题排查效率。此外，我还写了一个代码分析工具，可以自动抓取生产告警日志，结合大模型API进行分析和结果落表，明显加快了告警响应速度。\\\"\\n    },\\n    {\\n      \\\"heading\\\": \\\"SHEIN · 高级Java开发\\\",\\n      \\\"bullets\\\": [\\n        \\\"多渠广告投放系统：Meta、Google、TikTok等渠道管理\\\",\\n        \\\"设计实现自动投放、预算管理、数据回流与OneLink分流\\\",\\n        \\\"主导DSP账号权限迁移，保障业务平滑过渡\\\",\\n        \\\"率先引入Claude Code，开发Agent/SubAgent提升效率30%\\\",\\n        \\\"编写告警分析调度程序，实现自动化日志分析与可视化\\\"\\n      ],\\n      \\\"narration\\\": \\\"在SHEIN期间，我负责广告投放管理系统的后端开发，对接了Meta、Google、TikTok等多个渠道。我设计并实现了广告自动投放、预算控制、渠道数据回流以及OneLink分流等核心模块。作为项目主导者，我完成了DSP系统的账号权限迁移，确保业务零中断。我还在团队中第一个引入Claude Code大模型，通过开发Agent和SubAgent自动化处理文档整理、慢SQL优化等工作，让人力效率提升了30%。另外，我搭建了一套告警日志自动分析调度程序，结合大模型将分析结果可视化，大幅优化了运维排查流程。\\\"\\n    },\\n    {\\n      \\\"heading\\\": \\\"天猫技术 · Java开发（P6）\\\",\\n      \\\"bullets\\\": [\\n        \\\"天猫超市商详页核心开发，负责详情3.0升级、周期购等\\\",\\n        \\\"支撑加购后推荐、下架推荐等多场景商详接入\\\",\\n        \\\"淘宝买菜N元N件、时令频道、红包挑战等业务开发\\\",\\n        \\\"编写QuickQuery插件快速查询多环境配置\\\",\\n        \\\"用Appium/Selenium编写自动化脚本提升回归效率\\\"\\n      ],\\n      \\\"narration\\\": \\\"在天猫工作期间，我深入参与了天猫超市和淘宝买菜的商详页开发。为超市端我负责了详情3.0升级、周期购玩法和多种推荐场景的接入。在买菜业务中，我开发了N元N件、天降红包等前端玩法，并对标拼多多做了体验改造。为了提高团队效率，我自己写了一个IDEA插件QuickQuery，可以快速比对预发和线上的配置。我还用Appium和Selenium写了自动化脚本，分别覆盖多端展示验证和重复性操作，帮团队省下不少回归时间。\\\"\\n    },\\n    {\\n      \\\"heading\\\": \\\"中国平安 · Java开发\\\",\\n      \\\"bullets\\\": [\\n        \\\"负责金管家用户-代理人模块开发与数据库设计\\\",\\n        \\\"参与数据库脱O工程，完成Oracle到TiDB/MySQL改造\\\",\\n        \\\"智能推送策略算法核心开发，基于ALS实现个性化推荐\\\",\\n        \\\"项目获金管家'火焰杯'创新项目金奖\\\"\\n      ],\\n      \\\"narration\\\": \\\"我第一段工作经历是在中国平安，负责金管家App的用户和代理人模块。我参与了整个数据库脱O工程，把Oracle数据迁移到TiDB和MySQL，并完成相关SQL和业务代码的改造。最有成就感的是，我作为核心开发成员参与了智能推送策略项目，用ALS算法在首页“大家都在看”等模块实现个性化推荐，这个项目最终获得了公司的创新金奖。\\\"\\n    },\\n    {\\n      \\\"heading\\\": \\\"效率工具与团队贡献\\\",\\n      \\\"bullets\\\": [\\n        \\\"在嘉银引入Charles/Arthas，编写代码分析工具\\\",\\n        \\\"在SHEIN引入Claude Code，开发Agent/SubAgent\\\",\\n        \\\"在天猫开发QuickQuery插件与自动化测试脚本\\\",\\n        \\\"在平安编写团队共享的自动化排查脚本\\\",\\n        \\\"持续为团队输出小工具，提升研发效能\\\"\\n      ],\\n      \\\"narration\\\": \\\"我在每一段经历中都习惯主动发现效率瓶颈，并通过工具来解决。在平安，我编写了自动化排查脚本让团队重复工作一键完成；在天猫，我推出了QuickQuery插件和多端自动化验证脚本；到SHEIN，我第一个推广Claude Code大模型，并封装成Agent供团队复用；在嘉银，我又整合大模型做告警自动分析。这些都是我特别喜欢且持续在做的事情——为团队贡献小工具，提升整体研发效率。\\\"\\n    },\\n    {\\n      \\\"heading\\\": \\\"荣誉与竞赛\\\",\\n      \\\"bullets\\\": [\\n        \\\"平安集团技术公益创新大赛复赛第一、总决赛优秀奖（队长+开发）\\\",\\n        \\\"平安精益流程自动化大赛三等奖（队长+开发）\\\",\\n        \\\"金管家“火焰杯”项目金奖（核心开发）\\\",\\n        \\\"大学期间获挑战杯省赛银奖、校赛金奖等多项竞赛奖励\\\"\\n      ],\\n      \\\"narration\\\": \\\"此外，我还在公司内部和大学期间获得过不少荣誉。在平安，我作为队长带团队拿到集团技术公益创新大赛的复赛第一和总决赛优秀奖，还获得过流程自动化大赛三等奖。金管家的智能推送项目也为我们团队赢得了内部最高奖项“火焰杯”。大学时期的挑战杯、港澳台侨创新创业大赛等经历，也让我很早就习惯了以结果为导向的技术落地。\\\"\\n    }\\n  ],\\n  \\\"closingNarration\\\": \\\"以上就是我个人的整体情况。我非常喜欢用技术解决实际问题，也乐于为团队打造趁手的工具。如果您觉得我的经验和风格与团队契合，希望能有机会进一步交流，谢谢！\\\"\\n}\",\"reasoning_content\":\"我们被要求输出一个JSON对象，包含PPT内容。根据提供的简历，生成演示结构。需要title, subtitle, openingNarration, bodySlides (6-14页), closingNarration。\\n\\n简历人物：杨崇海，求职Java开发，6年经验，4段工作经历。需要基于此设计PPT。可能的结构：封面介绍、个人概况、教育背景、技能掌握、工作经历（按时间倒序，每段经历或合并）、项目亮点、成就荣誉、感谢结束。确保每页bullets简短，narration第一人称口语化。\\n\\n需输出JSON。不编造，只基于现有信息。bodySlides建议6-14页。我会制作约8-10页的body。\\n\\n准备架构：\\n第一页：个人简介与求职意向\\n第二页：教育背景\\n第三页：技能专长\\n第四页：上海嘉银科技经验（高级Java）\\n第五页：SHEIN经验\\n第六页：天猫技术经验\\n第七页：中国平安经验\\n第八页：技术产出与工具贡献\\n第九页：荣誉与成就\\n第十页：自我评价与致谢？但结束语在closingNarration。可以第十页作为总结但bodySlides后面有closingNarration。我们可以将bodySlides做到8-9页，然后结尾。\\n\\n注意bullets每条简短，narration对应。例如：\\n\\nslide1: heading \\\"个人概况\\\", bullets: [\\\"28岁，男，广东汕头\\\",\\\"6年Java开发经验\\\",\\\"暨南大学软件工程本科\\\",\\\"随时到岗，base上海\\\"], narration: \\\"大家好，我是杨崇海，今年28岁，来自广东汕头。本科毕业于暨南大学软件工程专业，拥有6年Java开发经验。目前我在上海，可以随时到岗。\\\"\\n\\nslide2: 教育背景。bullets: [\\\"2016-2020 暨南大学 软件工程 本科\\\",\\\"211院校\\\",\\\"在校期间多次获奖\\\",\\\"挑战杯省赛银奖等\\\"], narration: ...\\n\\n然后技能专长。把技能列出来。注意不需要编造。\\n\\nbodySlides数量估计：9页。\\n\\n需要确保整个JSON合法。输出只JSON。\\n\\n开始构造。\"},\"logprobs\":null,\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":1671,\"completion_tokens\":2348,\"total_tokens\":4019,\"prompt_tokens_details\":{\"cached_tokens\":1664},\"completion_tokens_details\":{\"reasoning_tokens\":455},\"prompt_cache_hit_tokens\":1664,\"prompt_cache_miss_tokens\":7},\"system_fingerprint\":\"fp_9954b31ca7_prod0820_fp8_kvcache_20260402\"}";
        JsonNode root = MAPPER.readTree(tempStr);
//        JsonNode root = MAPPER.readTree(resp.body());
        JsonNode message = root.path("choices").path(0).path("message");
        String content = message.path("content").asText("");
        if (content.isBlank()) {
            content = message.path("reasoning_content").asText("");
        }
        if (content.isBlank()) {
            throw new IllegalStateException("LLM 返回内容为空：" + tempStr);
        }

        String json = JsonPayloadExtractor.extractJsonObject(content);
        PresentationPlan plan = MAPPER.readValue(json, PresentationPlan.class);
        plan.normalize();
        if (plan.title.isBlank()) {
            throw new IllegalStateException("LLM 未生成 title");
        }
        if (plan.bodySlides.isEmpty()) {
            throw new IllegalStateException("LLM 未生成 bodySlides");
        }
        return plan;
    }
}
