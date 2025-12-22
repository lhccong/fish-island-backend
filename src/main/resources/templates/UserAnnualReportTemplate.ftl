<#-- 
  用户年度报告 FreeMarker 模板
  说明：所有变量均由 AnnualReportTemplateService 预先计算好，模板仅负责渲染展示层 HTML。
  参考图片样式：卡片式布局，浅蓝到白色渐变背景，关键数字蓝色粗体突出显示
-->
<div style="font-family:'PingFang SC','Microsoft YaHei',sans-serif;background:linear-gradient(135deg,#e0f2fe 0%,#f0f9ff 50%,#ffffff 100%);min-height:100vh;padding:40px 20px;position:relative;overflow:hidden;">
    <#-- 背景装饰圆形 -->
    <div style="position:absolute;top:-100px;right:-100px;width:300px;height:300px;background:radial-gradient(circle,rgba(34,197,94,0.15),transparent);border-radius:50%;filter:blur(60px);"></div>
    <div style="position:absolute;bottom:-150px;left:-150px;width:400px;height:400px;background:radial-gradient(circle,rgba(59,130,246,0.12),transparent);border-radius:50%;filter:blur(80px);"></div>
    <div style="position:absolute;top:50%;left:10%;width:200px;height:200px;background:radial-gradient(circle,rgba(34,197,94,0.1),transparent);border-radius:50%;filter:blur(50px);"></div>

    <div style="max-width:900px;margin:0 auto;position:relative;z-index:1;">
        <#-- 顶部区域：用户信息和平台标识 -->
        <div style="display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:30px;">
            <#-- 左侧：用户头像和昵称 -->
            <div style="display:flex;align-items:center;gap:12px;">
                <#if avatar?? && avatar != "">
                    <img src="${avatar}" alt="avatar"
                         style="width:70px;height:70px;border-radius:50%;object-fit:cover;border:3px solid rgba(59,130,246,0.2);box-shadow:0 2px 8px rgba(0,0,0,0.1);"/>
                <#else>
                    <div style="width:70px;height:70px;border-radius:50%;background:linear-gradient(135deg,#3b82f6,#2563eb);display:flex;align-items:center;justify-content:center;color:#fff;font-size:28px;font-weight:700;border:3px solid rgba(59,130,246,0.2);box-shadow:0 2px 8px rgba(0,0,0,0.1);">${(displayName!"用户")?substring(0,1)}</div>
                </#if>
                <div style="font-size:28px;font-weight:700;color:#1e40af;">${displayName!"用户"}</div>
            </div>

            <#-- 右侧：平台标识 -->
            <div style="display:flex;align-items:center;gap:8px;">
                <img src="https://oss.cqbo.com/moyu/moyu.png" alt="摸鱼岛Logo"
                     style="width:45px;height:45px;object-fit:contain;" onerror="this.style.display='none';"/>
                <div style="font-size:20px;font-weight:600;color:#1e40af;">摸鱼岛</div>
            </div>
        </div>

        <#-- 年度标题横幅：低饱和主题色背景，弱存在感承托“年度报告” -->
        <div style="background:linear-gradient(135deg,rgba(37,99,235,0.06),rgba(45,212,191,0.08));border-radius:12px 999px 999px 12px;padding:14px 24px;margin-bottom:24px;box-shadow:0 2px 8px rgba(15,23,42,0.04);max-width:420px;">
            <div style="font-size:22px;font-weight:700;color:#0f172a;display:flex;align-items:baseline;gap:6px;">
                <#if year??>
                    ${year?c}
                <#else>
                    ${.now?string("yyyy")}
                </#if>
                <span style="color:#2563eb;">年度摸鱼总结</span>
            </div>
        </div>

        <#-- 主标题：年度关键词（大号蓝色粗体，用作年度报告的核心标签） -->
        <div style="font-size:48px;font-weight:800;color:#2563eb;text-align:center;margin-bottom:40px;line-height:1.2;">
            ${annualKeyword!"摸鱼新手"}
        </div>

        <#-- 卡片区域：两列布局 -->
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:30px;">
            <#-- 卡片一：内容发布统计（左上） -->
            <div style="background:#ffffff;border-radius:16px;padding:28px;box-shadow:0 4px 16px rgba(0,0,0,0.08);position:relative;overflow:hidden;">
                <#assign postCountValue = postCount!0>
                <#assign totalWordsValue = totalWords!0>
                <div style="font-size:16px;color:#64748b;margin-bottom:12px;line-height:1.6;">
                    这一年,你共发布了 <span
                            style="color:#2563eb;font-weight:700;font-size:20px;">${postCountValue}</span>
                    篇内容<#if totalWordsValue gt 0><#if totalWordsValue gte 10000>,累计 <span
                        style="color:#2563eb;font-weight:700;font-size:20px;">${(totalWordsValue/10000)?string("0.0")}万</span> 个字<#else>,累计
                    <span style="color:#2563eb;font-weight:700;font-size:20px;">${totalWordsValue}</span> 个字</#if></#if>
                    。
                </div>
                <div style="font-size:14px;color:#94a3b8;margin-top:12px;line-height:1.6;">
                    程序员会敲的可不只是代码,每一行积累,都会在未来"运行"出绚丽的结果。
                </div>
                <#-- 装饰图标 -->
                <div style="position:absolute;bottom:10px;right:10px;width:60px;height:60px;opacity:0.1;">
                    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"
                         style="width:100%;height:100%;">
                        <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" stroke="#2563eb" stroke-width="2"/>
                    </svg>
                </div>
            </div>

            <#-- 卡片二：互动统计（右上） -->
            <div style="background:#ffffff;border-radius:16px;padding:28px;box-shadow:0 4px 16px rgba(0,0,0,0.08);position:relative;overflow:hidden;">
                <#assign postThumbsValue = postThumbs!0>
                <#assign postFavoursValue = postFavours!0>
                <#assign emoticonFavoursValue = emoticonFavours!0>
                <div style="font-size:16px;color:#64748b;margin-bottom:12px;line-height:1.6;">
                    <#if postThumbsValue gte 1000>
                        这些内容累计收获了 <span
                            style="color:#2563eb;font-weight:700;font-size:20px;">${(postThumbsValue/1000)?string("0.0")}千</span> 次好评互动。
                    <#elseif postThumbsValue gt 0>
                        这些内容累计收获了 <span
                            style="color:#2563eb;font-weight:700;font-size:20px;">${postThumbsValue}</span> 次好评互动。
                    <#else>
                        这些内容正在等待更多互动。
                    </#if>
                </div>
                <#if postFavoursValue gt 0>
                    <div style="font-size:16px;color:#64748b;margin-top:12px;line-height:1.6;">
                        你也留下了 <span
                                style="color:#2563eb;font-weight:700;font-size:20px;">${postFavoursValue}</span>
                        条点赞收藏和评论,这对摸鱼岛的伙伴们都很重要。
                    </div>
                </#if>
                <#if emoticonFavoursValue gt 0>
                    <div style="font-size:16px;color:#64748b;margin-top:12px;line-height:1.6;">
                        另外,你还收藏了 <span
                                style="color:#2563eb;font-weight:700;font-size:20px;">${emoticonFavoursValue}</span>
                        个表情包,快乐加倍。
                    </div>
                </#if>
                <#-- 点赞图标装饰 -->
                <div style="position:absolute;bottom:10px;right:10px;width:50px;height:50px;opacity:0.15;">
                    <svg viewBox="0 0 24 24" fill="#22c55e" xmlns="http://www.w3.org/2000/svg"
                         style="width:100%;height:100%;">
                        <path d="M14 9V5a3 3 0 00-3-3l-4 9v11h11.28a2 2 0 002-1.7l1.38-9a2 2 0 00-2-2.3zM7 22H4a2 2 0 01-2-2v-7a2 2 0 012-2h3"/>
                    </svg>
                </div>
            </div>

            <#-- 卡片三：浏览与学习统计（左中） -->
            <div style="background:#ffffff;border-radius:16px;padding:28px;box-shadow:0 4px 16px rgba(0,0,0,0.08);position:relative;overflow:hidden;">
                <div style="font-size:16px;color:#64748b;line-height:1.6;">
                    今年,你浏览了 <span style="color:#2563eb;font-weight:700;font-size:20px;">${postViews!0}</span>
                    篇内容<#if tutorialCount?? && tutorialCount gt 0>,学习过 <span
                        style="color:#2563eb;font-weight:700;font-size:20px;">${tutorialCount}</span> 个教程</#if>。
                </div>
            </div>

            <#-- 卡片四：初次相遇日期（右中） -->
            <div style="background:#ffffff;border-radius:16px;padding:28px;box-shadow:0 4px 16px rgba(0,0,0,0.08);position:relative;overflow:hidden;">
                <#if registerTime??>
                    <div style="font-size:16px;color:#64748b;line-height:1.6;">
                        你与摸鱼岛初次相遇于 <span
                                style="color:#2563eb;font-weight:700;font-size:20px;">${registerTime?string("yyyy 年 M 月 d 日")}</span>,你的每次进步我们都记得。
                    </div>
                    <#if accompanyDays?? && accompanyDays gt 0>
                        <div style="font-size:16px;color:#64748b;margin-top:8px;line-height:1.6;">
                            从那一天到现在,你已经在摸鱼岛摸鱼了 <span
                                    style="color:#2563eb;font-weight:700;font-size:20px;">${accompanyDays}</span> 天。
                        </div>
                    </#if>
                <#else>
                    <div style="font-size:16px;color:#64748b;line-height:1.6;">
                        感谢你与摸鱼岛一起成长,你的每次进步我们都记得。
                    </div>
                </#if>
            </div>

            <#-- 卡片五：特别事件/最佳帖子（左下） -->
            <#if bestPostTitle?? && bestPostTitle != "">
                <div style="background:#ffffff;border-radius:16px;padding:28px;box-shadow:0 4px 16px rgba(0,0,0,0.08);position:relative;overflow:hidden;">
                    <#if bestPostDate??>
                        <div style="font-size:16px;color:#64748b;margin-bottom:12px;line-height:1.6;">
                            <span style="color:#2563eb;font-weight:700;font-size:20px;">${bestPostDate}</span>你发布了最佳帖子《${bestPostTitle}
                            》,还记得你收藏的那些精彩内容吗?
                        </div>
                    <#else>
                        <div style="font-size:16px;color:#64748b;margin-bottom:12px;line-height:1.6;">
                            你发布了最佳帖子《${bestPostTitle}》,还记得你收藏的那些精彩内容吗?
                        </div>
                    </#if>
                    <div style="font-size:14px;color:#94a3b8;margin-top:12px;line-height:1.6;">
                        不断丰富你的"代码仓库",终将改变人生。
                    </div>
                </div>
            </#if>

            <#-- 卡片六：宠物养成（右下） -->
            <div style="background:#ffffff;border-radius:16px;padding:28px;box-shadow:0 4px 16px rgba(0,0,0,0.08);position:relative;overflow:hidden;">
                <#if petCount?? && petCount gt 0>
                    <div style="font-size:16px;color:#64748b;line-height:1.6;">
                        这一年,你养成了 <span style="color:#2563eb;font-weight:700;font-size:20px;">${petCount}</span>
                        只宠物
                        <#if topPetName??>
                        ,其中 <span style="color:#2563eb;font-weight:700;font-size:20px;">${topPetName}</span> 达到了
                        <span style="color:#2563eb;font-weight:700;font-size:20px;">${topPetLevel!0}</span> 级
                        </#if>。
                    </div>
                <#else>
                    <div style="font-size:16px;color:#64748b;line-height:1.6;">
                        这一年,你还没有领养宠物,期待与你的第一只摸鱼小伙伴相遇。
                    </div>
                </#if>
            </div>

            <#-- 卡片七：赞助支持 -->
            <div style="background:#ffffff;border-radius:16px;padding:28px;box-shadow:0 4px 16px rgba(0,0,0,0.08);position:relative;overflow:hidden;">
                <#assign donationValue = donationText!"0.00">
                <#if donationValue != "0.00">
                    <div style="font-size:16px;color:#64748b;line-height:1.6;">
                        这一年,你为摸鱼岛赞助了 <span
                                style="color:#2563eb;font-weight:700;font-size:20px;">${donationValue}</span>
                        元,谢谢你的真金白银支持。
                    </div>
                <#else>
                    <div style="font-size:16px;color:#64748b;line-height:1.6;">
                        这一年,即便没有赞助,你的每一次浏览和互动,也是对摸鱼岛最好的支持。
                    </div>
                </#if>
            </div>

            <#-- 底部区域：行动呼吁和二维码 -->
            <div style="background:#ffffff;border-radius:16px;padding:28px;box-shadow:0 4px 16px rgba(0,0,0,0.08);text-align:center;position:relative;overflow:hidden;">
                <#-- 行动呼吁文字 -->
                <div style="font-size:20px;font-weight:700;color:#2563eb;margin-bottom:18px;">
                    划划水｜冒个泡｜聊一会 就上摸鱼岛
                </div>

                <#-- 二维码区域（预留，如果需要可以后续添加） -->
                <div style="margin:14px 0;">
                    <img src="https://oss.cqbo.com/moyu/yucoder.cn.png" alt="年度报告二维码"
                         style="width:200px;height:200px;border:2px solid #e5e7eb;border-radius:8px;"/>
                </div>
                <div style="font-size:16px;color:#64748b;margin-top:10px;">
                    扫码生成你的 <span style="color:#2563eb;font-weight:700;">
                        <#if year??>
                            ${year?c}
                        <#else>
                            ${.now?string("yyyy")}
                        </#if>
                        年度报告
                    </span>
                </div>

                <#-- 年度报告提示文字 -->
                <div style="font-size:16px;color:#64748b;margin-top:16px;line-height:1.6;">
                    这是你的 <span style="color:#2563eb;font-weight:700;">
                        <#if year??>
                            ${year?c}
                        <#else>
                            ${.now?string("yyyy")}
                        </#if>
                        年度报告
                    </span>，感谢你与摸鱼岛一起成长！
                </div>
            </div>
        </div>
    </div>
</div>